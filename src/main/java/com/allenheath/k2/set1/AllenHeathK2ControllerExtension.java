package com.allenheath.k2.set1;

import java.util.ArrayList;
import java.util.List;

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
	private HardwareSurface surface;
	private MidiIn midiIn;
	private MidiOut midiOut;
	private Layers layers;
	private NoteInput noteInput;
	private ViewCursorControl viewControl;
	private Layer mainLayer;
	private final StateButton[] reverbSendButtons = new StateButton[8];
	private final StateButton[] delaySendButtons = new StateButton[8];
	private final List<PadContainer> drumPadsList = new ArrayList<>();
	private final PadAssignment[] assignmentList = new PadAssignment[8];

	// PSP42
	private DirectParameterControl psp42RptUnlControl;

	// PSP42x
	private DirectParameterControl psp42xRptUnlControl;
	private DirectParameterControl psp42xDlyX2Control;
	private DirectParameterControl psp42DlyX2Control;
	
	// Megaverb
	private DirectParameterControl reverbControl;

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
		noteInput = midiIn.createNoteInput("MIDI", "80????", "90????", "A0????", "D0????");
		noteInput.setShouldConsumeEvents(false);

		// PSP42
		psp42RptUnlControl = new DirectParameterControl(SpecialVstDevices.LEXICON_PSP, SpecialParam.PSP_REPEAT_INF);
		psp42DlyX2Control = new DirectParameterControl(SpecialVstDevices.LEXICON_PSP, SpecialParam.PSP_DELAY_X2);

		// PSP42x
		psp42xRptUnlControl = new DirectParameterControl(SpecialVst3Devices.LEXICON_PSP2, SpecialParam.PSP2_REPEAT_INF);
		psp42xDlyX2Control = new DirectParameterControl(SpecialVst3Devices.LEXICON_PSP2, SpecialParam.PSP2_DELAY_X2);
		
		// Megaverb
		reverbControl = new DirectParameterControl(SpecialVst3Devices.MEGAVERB3, SpecialParam.MEGA_VERB_GATE);

		final List<DirectParameterControl> controlList = new ArrayList<>();
		controlList.add(psp42RptUnlControl); // PSP42

		controlList.add(psp42xRptUnlControl); // PSP42x
		controlList.add(psp42xDlyX2Control); // PSP42x
		controlList.add(psp42DlyX2Control); // PSP42x

		controlList.add(reverbControl); // Megaverb

		viewControl = new ViewCursorControl(host, controlList, 16);
		mainLayer = new Layer(layers, "MainLayer");
		host.showPopupNotification("Intialize Xone:K2 DJ Set");
		initSendsButtons();
		initDocumentProperties();
		mainLayer.activate();
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
			final StateButton revButton = new StateButton("REV_" + i + "_BUTTON", 44 + noteOffset, channel, surface, midiIn, midiOut);
			final StateButton delButton = new StateButton("DEL_" + i + "_BUTTON", 40 + noteOffset, channel, surface, midiIn, midiOut);
			final PadAssignment assignment = assignmentList[i];
			delButton.bind(mainLayer, () -> assignment.toggleSendValue(0), () -> assignment.sendStatusColor(0));
			revButton.bind(mainLayer, () -> assignment.toggleSendValue(1), () -> assignment.sendStatusColor(1));
			reverbSendButtons[i] = revButton;
			delaySendButtons[i] = delButton;
		}

		// PSP42
		final StateButton psp42DlyRptButton = new StateButton("PSP42_DLY_INF_BUTTON", 49, 13, surface, midiIn, midiOut);
		final StateButton psp42DlyX2 = new StateButton("PSP42_DLYX2_BUTTON", 50, 13, surface, midiIn, midiOut);
		
		// PSP42x
		final StateButton psp42xDly2RptButton = new StateButton("PSP42x_DLY_INF_BUTTON", 49, 13, surface, midiIn, midiOut);
		final StateButton psp42xDlyX2 = new StateButton("PSP42x_DLYX2_BUTTON", 50, 13, surface, midiIn, midiOut);

		// Megaverb
		final StateButton revGateButton = new StateButton("RVB_GATE_BUTTON", 50, 14, surface, midiIn, midiOut);

		// PSP42
		psp42DlyRptButton.bind(mainLayer, () -> psp42RptUnlControl.toggle(SpecialParam.PSP_REPEAT_INF),
				() -> psp42RptUnlControl.getState(SpecialParam.PSP_REPEAT_INF));
				
		// PSP42
		psp42DlyX2.bind(mainLayer, () -> psp42DlyX2Control.toggle(SpecialParam.PSP_DELAY_X2),
				() -> psp42DlyX2Control.getState(SpecialParam.PSP_DELAY_X2));

		// PSP42x
		psp42xDly2RptButton.bind(mainLayer, () -> psp42xRptUnlControl.toggle(SpecialParam.PSP2_REPEAT_INF),
				() -> psp42xRptUnlControl.getState(SpecialParam.PSP2_REPEAT_INF));

		// PSP42x
		psp42xDlyX2.bind(mainLayer, () -> psp42xDlyX2Control.toggle(SpecialParam.PSP2_DELAY_X2),
				() -> psp42xDlyX2Control.getState(SpecialParam.PSP2_DELAY_X2));

		// Megaverb
		revGateButton.bind(mainLayer, () -> reverbControl.toggle(SpecialParam.MEGA_VERB_GATE),
				() -> reverbControl.getState(SpecialParam.MEGA_VERB_GATE));
	}

	@Override
	public void exit() {
		getHost().showPopupNotification("Xone:K2 DJ Set exited");
	}

	@Override
	public void flush() {
		surface.updateHardware();
	}

}
