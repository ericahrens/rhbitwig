package com.allenheath.k2.set1;

import java.util.ArrayList;
import java.util.List;
import java.io.IOException;
import java.net.URI;
import java.net.HttpURLConnection;

import com.bitwig.extension.controller.ControllerExtension;
import com.bitwig.extension.controller.ControllerExtensionDefinition;
import com.bitwig.extension.controller.api.ControllerHost;
import com.bitwig.extension.controller.api.DocumentState;
import com.bitwig.extension.controller.api.HardwareSurface;
import com.bitwig.extension.controller.api.MidiIn;
import com.bitwig.extension.controller.api.MidiOut;
import com.bitwig.extension.controller.api.NoteInput;
import com.bitwig.extension.controller.api.SettableStringValue;
import com.bitwig.extensions.framework.Layer;
import com.bitwig.extensions.framework.Layers;

public class AllenHeathK2ControllerExtension extends ControllerExtension {

	private static final String[] DEFAULT_PAD_ASSIGNMENTS = { "1,2", "4,8", "3", "5,6,13", "7", "9", "10", "11,12" };
	private static final long DECK_DOUBLE_CLICK_WINDOW_MS = 320;
	private static final int[] DECK_NOTES = {48, 51, 48, 51};
	private static final int[] DECK_LED_NOTES = {84, 87, 84, 87};
	private static final String[] DECK_NAMES = {"A", "B", "C", "D"};
	// Bitwig matcher channels are 0-based: 13/14 == MIDI channels 14/15
	private static final int[] DECK_CHANNELS = {13, 13, 14, 14};
	private HardwareSurface surface;
	private MidiIn midiIn;
	private MidiOut midiOut;
    private ButtonManager buttonManager;
	private Layers layers;
	private NoteInput noteInput;
	private ViewCursorControl viewControl;
	private Layer mainLayer;
	private final StateButton[] reverbSendButtons = new StateButton[8];
	private final StateButton[] delaySendButtons = new StateButton[8];
	private final List<PadContainer> drumPadsList = new ArrayList<>();
	private final PadAssignment[] assignmentList = new PadAssignment[8];

	private DirectParameterControl psp42RptUnlControl;
	private DirectParameterControl psp42xRptUnlControl;
	private DirectParameterControl psp42xDlyX2Control;
	private DirectParameterControl psp42DlyX2Control;
	private DirectParameterControl reverbControl;

	// selected deck for the 4 K2 deck buttons (0..3). -1 = none
	private int selectedK2Deck = -1;
	private int pendingDeckClick = -1;
	private long pendingDeckClickAtMs = -1;

	protected AllenHeathK2ControllerExtension(final ControllerExtensionDefinition definition, final ControllerHost host) {
		super(definition, host);
	}

	@Override
	public void init() {
		final ControllerHost host = getHost();

		layers = new Layers(this);
		surface = host.createHardwareSurface();
		midiIn = host.getMidiInPort(0);
		midiOut = host.getMidiOutPort(0);

		buttonManager = new ButtonManager(surface, midiIn, midiOut);
		
		host.println("========================================");
		host.println("XONE:K2/K3 DJSet");
		host.println("========================================");
		
		noteInput = midiIn.createNoteInput("MIDI", "80????", "90????", "A0????", "D0????");
		noteInput.setShouldConsumeEvents(false);

		psp42RptUnlControl = new DirectParameterControl(SpecialVstDevices.LEXICON_PSP, SpecialParam.PSP_REPEAT_INF);
		psp42DlyX2Control = new DirectParameterControl(SpecialVstDevices.LEXICON_PSP, SpecialParam.PSP_DELAY_X2);
		psp42xRptUnlControl = new DirectParameterControl(SpecialVst3Devices.LEXICON_PSP2, SpecialParam.PSP2_REPEAT_INF);
		psp42xDlyX2Control = new DirectParameterControl(SpecialVst3Devices.LEXICON_PSP2, SpecialParam.PSP2_DELAY_X2);
		reverbControl = new DirectParameterControl(SpecialVst3Devices.MEGAVERB3, SpecialParam.MEGA_VERB_GATE);

		final List<DirectParameterControl> controlList = new ArrayList<>();
		controlList.add(psp42RptUnlControl);
		controlList.add(psp42xRptUnlControl);
		controlList.add(psp42xDlyX2Control);
		controlList.add(psp42DlyX2Control);
		controlList.add(reverbControl);

		viewControl = new ViewCursorControl(host, controlList, 16);
		mainLayer = new Layer(layers, "MainLayer");
		host.showPopupNotification("Initialize Xone:K2/K3 DJ Set");
		initSendsButtons();
		initDocumentProperties();
		selectK2Deck(0);
		mainLayer.activate();
		
		host.println("========================================");
		host.println("K2/K3 DJSet READY!");
		host.println("========================================");
	}

	private void initDocumentProperties() {
		final DocumentState documentState = getHost().getDocumentState();
		for (int i = 0; i < 8; i++) {
			final int index = i;
			final SettableStringValue padAssignment = documentState.getStringSetting("Column " + (i + 1), "Pad Assignments", 10,
					DEFAULT_PAD_ASSIGNMENTS[i]);
			padAssignment.addValueObserver(value -> assignmentList[index].assign(value, drumPadsList));
		}
	}

	private void initSendsButtons() {
		for (int i = 0; i < 16; i++) {
			final PadContainer pad = new PadContainer(i, viewControl.getDrumPadBank().getItemAt(i));
			drumPadsList.add(pad);
		}
		for (int i = 0; i < assignmentList.length; i++) {
			assignmentList[i] = new PadAssignment(i);
		}

		for (int i = 0; i < 8; i++) {
			final int channel = 13 + i / 4;
			final int noteOffset = i % 4;

			if (i < 4) {
				final int deckIndex = i;
				final StateButton deckButton = buttonManager.createDirectLedStateButton("DECK_" + (i + 1) + "_BUTTON",
						DECK_NOTES[i], DECK_LED_NOTES[i], DECK_CHANNELS[i]);
				deckButton.bind(mainLayer, () -> handleDeckButtonPress(deckIndex),
						() -> selectedK2Deck == deckIndex ? RedGreenButtonState.GREEN : RedGreenButtonState.OFF);
			}
			
			final StateButton revButton = buttonManager.createStateButton("REV_" + i + "_BUTTON", 44 + noteOffset, channel);
			final StateButton delButton = buttonManager.createStateButton("DEL_" + i + "_BUTTON", 40 + noteOffset, channel);
			final PadAssignment assignment = assignmentList[i];
			delButton.bind(mainLayer, () -> assignment.toggleSendValue(0), () -> assignment.sendStatusColor(0));
			revButton.bind(mainLayer, () -> assignment.toggleSendValue(1), () -> assignment.sendStatusColor(1));
			reverbSendButtons[i] = revButton;
			delaySendButtons[i] = delButton;
		}

		final StateButton psp42DlyRptButton = buttonManager.createStateButton("PSP42_DLY_INF_BUTTON", 49, 13);
		final StateButton psp42DlyX2 = buttonManager.createStateButton("PSP42_DLYX2_BUTTON", 50, 13);
		final StateButton psp42xDly2RptButton = buttonManager.createStateButton("PSP42x_DLY_INF_BUTTON", 49, 13);
		final StateButton psp42xDlyX2 = buttonManager.createStateButton("PSP42x_DLYX2_BUTTON", 50, 13);
		final StateButton revGateButton = buttonManager.createStateButton("RVB_GATE_BUTTON", 50, 14);

		psp42DlyRptButton.bind(mainLayer, () -> psp42RptUnlControl.toggle(SpecialParam.PSP_REPEAT_INF),
				() -> psp42RptUnlControl.getState(SpecialParam.PSP_REPEAT_INF));
		psp42DlyX2.bind(mainLayer, () -> psp42DlyX2Control.toggle(SpecialParam.PSP_DELAY_X2),
				() -> psp42DlyX2Control.getState(SpecialParam.PSP_DELAY_X2));
		psp42xDly2RptButton.bind(mainLayer, () -> psp42xRptUnlControl.toggle(SpecialParam.PSP2_REPEAT_INF),
				() -> psp42xRptUnlControl.getState(SpecialParam.PSP2_REPEAT_INF));
		psp42xDlyX2.bind(mainLayer, () -> psp42xDlyX2Control.toggle(SpecialParam.PSP2_DELAY_X2),
				() -> psp42xDlyX2Control.getState(SpecialParam.PSP2_DELAY_X2));
		revGateButton.bind(mainLayer, () -> reverbControl.toggle(SpecialParam.MEGA_VERB_GATE),
				() -> reverbControl.getState(SpecialParam.MEGA_VERB_GATE));
	}

	@Override
	public void exit() {
		getHost().showPopupNotification("Xone:K2/K3 DJ Set exited");
	}

	private void handleDeckButtonPress(final int deckIndex) {
		final long nowMs = System.nanoTime() / 1_000_000L;
		final long deltaMs = nowMs - pendingDeckClickAtMs;
		if (pendingDeckClick == deckIndex && pendingDeckClickAtMs >= 0 && deltaMs <= DECK_DOUBLE_CLICK_WINDOW_MS) {
			pendingDeckClick = -1;
			pendingDeckClickAtMs = -1;
			selectK2Deck(deckIndex);
			return;
		}

		pendingDeckClick = deckIndex;
		pendingDeckClickAtMs = nowMs;
	}

	private void selectK2Deck(final int deckIndex) {
		if (deckIndex < 0 || deckIndex > 3 || selectedK2Deck == deckIndex) return;
		selectedK2Deck = deckIndex;
		pendingDeckClick = -1;
		pendingDeckClickAtMs = -1;
		if (midiOut != null) {
			final int selectedStatus = 0x90 | (DECK_CHANNELS[deckIndex] & 0x0F);
			midiOut.sendMidi(selectedStatus, DECK_NOTES[deckIndex], 127);
		}
		notifyBridgeActiveDeck(deckIndex);
	}

	private void notifyBridgeActiveDeck(final int deckIndex) {
		final String deckName = DECK_NAMES[deckIndex];
		HttpURLConnection connection = null;
		try {
			connection = (HttpURLConnection) URI.create("http://localhost:8080/activeDeck/" + deckName).toURL().openConnection();
			connection.setRequestMethod("POST");
			connection.setDoOutput(true);
			connection.setConnectTimeout(200);
			connection.setReadTimeout(200);
			connection.getOutputStream().close();
			connection.getResponseCode();
		} catch (final IOException error) {
			getHost().println("Failed to notify bridge for deck " + deckName + ": " + error.getMessage());
		} finally {
			if (connection != null) {
				connection.disconnect();
			}
		}
	}
    

	@Override
	public void flush() {
		surface.updateHardware();
	}
}