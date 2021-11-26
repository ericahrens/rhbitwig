package com.akai.fire;

import java.util.HashMap;
import java.util.Map;

import com.akai.fire.control.BiColorButton;
import com.akai.fire.control.RgbButton;
import com.akai.fire.control.TouchEncoder;
import com.akai.fire.display.OledDisplay;
import com.akai.fire.lights.BiColorLightState;
import com.akai.fire.lights.RgbLigthState;
import com.akai.fire.sequence.DrumSequenceMode;
import com.bitwig.extension.api.util.midi.ShortMidiMessage;
import com.bitwig.extension.callback.ShortMidiMessageReceivedCallback;
import com.bitwig.extension.controller.ControllerExtension;
import com.bitwig.extension.controller.api.ControllerHost;
import com.bitwig.extension.controller.api.HardwareSurface;
import com.bitwig.extension.controller.api.MidiIn;
import com.bitwig.extension.controller.api.MidiOut;
import com.bitwig.extension.controller.api.MultiStateHardwareLight;
import com.bitwig.extension.controller.api.NoteInput;
import com.bitwig.extension.controller.api.Transport;
import com.bitwig.extensions.debug.RemoteConsole;
import com.bitwig.extensions.framework.Layer;
import com.bitwig.extensions.framework.Layers;
import com.bitwig.extensions.rh.BooleanValueObject;
import com.bitwig.extensions.rh.Midi;

public class AkaiFireDrumSeqExtension extends ControllerExtension {
	private HardwareSurface surface;
	private Transport transport;
	private MidiIn midiIn;
	private MidiOut midiOut;
	private Layers layers;
	private int blinkTicks = 0;

	public final static byte SE_ST = (byte) 0xf0;
	public final static byte SE_EN = (byte) 0xf7;
	public final static byte MAN_ID_AKAI = 0x47;
	public final static byte DEVICE_ID = 0x7f;
	public final static byte PRODUCT_ID = 0x43;
	public final static byte SE_CMD_RGB = 0x65;
	public final static byte SE_OLED_RGB = 0x08;
	private final static String DEV_INQ = "F0 7E 00 06 01 F7";
	private final byte[] singleRgb = new byte[] { SE_ST, MAN_ID_AKAI, DEVICE_ID, PRODUCT_ID, SE_CMD_RGB, 00, 04, 0, 0,
			0, 0, SE_EN };

	private final int[] lastCcValue = new int[128];

	private Layer mainLayer;
	private final RgbButton[] rgbButtons = new RgbButton[64];
	private final TouchEncoder[] encoders = new TouchEncoder[4];
	private final MultiStateHardwareLight[] stateLights = new MultiStateHardwareLight[4];
	private final Map<NoteAssign, BiColorButton> controlButtons = new HashMap<>();
	private NoteInput noteInput;
	private DrumSequenceMode drumSeqenceLayer;
	private ViewCursorControl viewControl;

	private final BooleanValueObject shiftActive = new BooleanValueObject();
	private OledDisplay oled;
	private ControllerHost host;
	private TouchEncoder mainEncoder;

	protected AkaiFireDrumSeqExtension(final AkaiFireDrumSeqDefinition definition, final ControllerHost host) {
		super(definition, host);
	}

	@Override
	public void init() {
		host = getHost();
		RemoteConsole.out.registerHost(host);
		for (int i = 0; i < lastCcValue.length; i++) {
			lastCcValue[i] = -1;
		}
		layers = new Layers(this);
		midiIn = host.getMidiInPort(0);
		midiIn.setMidiCallback((ShortMidiMessageReceivedCallback) msg -> onMidi0(msg));
		midiIn.setSysexCallback(msg -> onSysEx(msg));
		midiOut = host.getMidiOutPort(0);
		transport = host.createTransport();
		surface = host.createHardwareSurface();
		noteInput = midiIn.createNoteInput("MIDI", "80????", "90????", "A0????", "D0????");
		noteInput.setShouldConsumeEvents(false);
		viewControl = new ViewCursorControl(host, 16);

		mainLayer = new Layer(layers, "Main");
		oled = new OledDisplay(midiOut);

		setUpHardware();
		setUpTransportControl();

		drumSeqenceLayer = new DrumSequenceMode(this);
		midiOut.sendSysex(DEV_INQ);

		oled.showLogo();
		mainLayer.activate();
		drumSeqenceLayer.activate();
		host.scheduleTask(this::handlePing, 100);
		getHost().showPopupNotification("Init Akai Fire: Drum Sequencer");
	}

	private void handlePing() {
		// sections.forEach(section -> section.notifyBlink(blinkTicks));
		blinkTicks++;
		oled.notifyBlink(blinkTicks);
		drumSeqenceLayer.notifyBlink(blinkTicks);
		host.scheduleTask(this::handlePing, 100);
	}

	private void onSysEx(final String msg) {
		// getHost().println("Sys Ex IN " + msg);
	}

	private void sendPadRgb(final int pad, final int r, final int g, final int b) {
		singleRgb[7] = (byte) pad;
		singleRgb[8] = (byte) r;
		singleRgb[9] = (byte) g;
		singleRgb[10] = (byte) b;
		midiOut.sendSysex(singleRgb);
	}

	private void setUpTransportControl() {
		transport.isPlaying().markInterested();
		transport.tempo().markInterested();
		transport.playPosition().markInterested();
		transport.isClipLauncherOverdubEnabled().markInterested();
		final BiColorButton playButton = addButton(NoteAssign.PLAY);
		playButton.bindPressed(mainLayer, this::togglePlay, this::getPlayState);
		final BiColorButton recButton = addButton(NoteAssign.REC);
		recButton.bindPressed(mainLayer, this::toggleRec, this::getOverdubState);
		final BiColorButton stopButton = addButton(NoteAssign.STOP);
		stopButton.bindPressed(mainLayer, this::stopAction, BiColorLightState.RED_FULL);

		final BiColorButton shiftButton = addButton(NoteAssign.SHIFT);
		shiftButton.bind(mainLayer, shiftActive, BiColorLightState.RED_HALF, BiColorLightState.OFF);

		final BiColorButton m1Button = addButton(NoteAssign.MUTE_1);
		m1Button.bindPressed(mainLayer, this::dummyAction, BiColorLightState.RED_FULL);
		addButton(NoteAssign.MUTE_2);
		addButton(NoteAssign.MUTE_3);
		addButton(NoteAssign.MUTE_4);
		addButton(NoteAssign.STEP_SEQ);
		addButton(NoteAssign.KNOB_MODE, NoteAssign.KNOB_MODE_LIGHT.getNoteValue());
		addButton(NoteAssign.NOTE);
		addButton(NoteAssign.DRUM);
		addButton(NoteAssign.PERFORM);
		addButton(NoteAssign.ALT);
		addButton(NoteAssign.PATTERN_UP);
		addButton(NoteAssign.PATTERN_DOWN);
		addButton(NoteAssign.BANK_L);
		addButton(NoteAssign.BANK_R);
		addButton(NoteAssign.PATTERN);
		addButton(NoteAssign.BROWSER);
		stateLights[0] = createLight(NoteAssign.TRACK_SELECT_1);
		stateLights[1] = createLight(NoteAssign.TRACK_SELECT_2);
		stateLights[2] = createLight(NoteAssign.TRACK_SELECT_3);
		stateLights[3] = createLight(NoteAssign.TRACK_SELECT_4);
	}

	private BiColorButton addButton(final NoteAssign which, final int ccLightValue) {
		final BiColorButton button = new BiColorButton(which, this, ccLightValue);
		controlButtons.put(which, button);
		return button;
	}

	private BiColorButton addButton(final NoteAssign which) {
		final BiColorButton button = new BiColorButton(which, this);
		controlButtons.put(which, button);
		return button;
	}

	private MultiStateHardwareLight createLight(final NoteAssign assignment) {
		final MultiStateHardwareLight light = surface
				.createMultiStateHardwareLight("BASIC_LIGHT_" + assignment.toString());
		final int ccValue = assignment.getNoteValue();
		light.state().onUpdateHardware(state -> {
			if (state instanceof BiColorLightState) {
				sendCC(ccValue, ((BiColorLightState) state).getStateValue());
			} else {
				sendCC(ccValue, 0);
			}
		});
		return light;
	}

	private BiColorLightState getPlayState() {
		return transport.isPlaying().get() ? BiColorLightState.GREEN_FULL : BiColorLightState.GREEN_HALF;
	}

	private BiColorLightState getOverdubState() {
		return transport.isClipLauncherOverdubEnabled().get() ? BiColorLightState.GREEN_FULL : BiColorLightState.OFF;
	}

	private void dummyAction(final boolean pressed) {
	}

	private void stopAction(final boolean pressed) {
		if (!pressed) {
			return;
		}
		transport.stop();
	}

	private void toggleRec(final boolean pressed) {
		if (!pressed) {
			return;
		}
		transport.isClipLauncherOverdubEnabled().toggle();
	}

	private void togglePlay(final boolean pressed) {
		if (!pressed) {
			return;
		}
		if (transport.isPlaying().get()) {
			transport.isPlaying().set(false);
		} else {
			drumSeqenceLayer.retrigger();
			transport.restart();
		}
	}

	private void setUpHardware() {
		for (int index = 0; index < 4; index++) {
			final int controlId = 16 + index;
			encoders[index] = new TouchEncoder(controlId, controlId, this);
		}
		mainEncoder = new TouchEncoder(0x76, 0x19, this);

		for (int i = 0; i < rgbButtons.length; i++) {
			final int index = i;
			rgbButtons[index] = new RgbButton(index, this);
		}
	}

	public HardwareSurface getSurface() {
		return surface;
	}

	public MidiIn getMidiIn() {
		return midiIn;
	}

	public MidiOut getMidiOut() {
		return midiOut;
	}

	public Layers getLayers() {
		return layers;
	}

	public NoteInput getNoteInput() {
		return noteInput;
	}

	public ViewCursorControl getViewControl() {
		return viewControl;
	}

	public RgbButton[] getRgbButtons() {
		return rgbButtons;
	}

	public TouchEncoder[] getEncoders() {
		return encoders;
	}

	public OledDisplay getOled() {
		return oled;
	}

	public BiColorButton getButton(final NoteAssign which) {
		return controlButtons.get(which);
	}

	public TouchEncoder getMainEncoder() {
		return mainEncoder;
	}

	private void onMidi0(final ShortMidiMessage msg) {
		getHost().println("MIDI " + msg.getStatusByte() + " " + msg.getData1() + " " + msg.getData2());
	}

	@Override
	public void exit() {
		getHost().showPopupNotification("Exit Akai Fire Drum Seq");
	}

	@Override
	public void flush() {
		surface.updateHardware();
	}

	public void sendCC(final int ccNr, final int value) {
		if (lastCcValue[ccNr] == -1 || lastCcValue[ccNr] != value) {
			midiOut.sendMidi(Midi.CC, ccNr, value);
			lastCcValue[ccNr] = value;
		}
	}

	public void updateRgbPad(final int index, final RgbLigthState state) {
		sendPadRgb(index, state.getRed(), state.getGreen(), state.getBlue());
	}

	public MultiStateHardwareLight[] getStateLights() {
		return stateLights;
	}

}
