package com.novation.launchcontrol.arp;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

import com.bitwig.extension.api.util.midi.ShortMidiMessage;
import com.bitwig.extension.callback.ShortMidiMessageReceivedCallback;
import com.bitwig.extension.controller.ControllerExtension;
import com.bitwig.extension.controller.api.AbsoluteHardwareKnob;
import com.bitwig.extension.controller.api.ControllerHost;
import com.bitwig.extension.controller.api.CursorTrack;
import com.bitwig.extension.controller.api.DocumentState;
import com.bitwig.extension.controller.api.HardwareSurface;
import com.bitwig.extension.controller.api.IntegerValue;
import com.bitwig.extension.controller.api.MidiIn;
import com.bitwig.extension.controller.api.MidiOut;
import com.bitwig.extension.controller.api.MultiStateHardwareLight;
import com.bitwig.extension.controller.api.Parameter;
import com.bitwig.extension.controller.api.PinnableCursorDevice;
import com.bitwig.extension.controller.api.SettableEnumValue;
import com.bitwig.extension.controller.api.SpecificBitwigDevice;
import com.bitwig.extension.controller.api.TrackBank;
import com.bitwig.extensions.framework.Layers;
import com.yaeltex.debug.RemoteConsole;

public class LpcArpControlExtension extends ControllerExtension implements ArpParameterContainer {

	private static final int BASE_CHANNEL = 0;
	private static final int SENDA_CC_INDEX = 13;
	private static final int SENDB_CC_INDEX = 29;
	private static final int PAN_CC_INDEX = 49;
	private static final int SLIDER_CC_INDEX = 77;
	private static final int[] TRACK_FOCUS_NOTE = { 41, 42, 43, 44, 57, 58, 59, 60 };
	private static final int[] TRACK_CONTROL_NOTE = { 73, 74, 75, 76, 89, 90, 91, 92 };

	private final byte[] buttonStateSysEx = { (byte) 0xF0, 0x00, 0x20, 0x29, 0x02, 0x11, 0x78, 0x00, 0x00, 0x00,
			(byte) 0xF7 };

	private final RedGreenButton[] focusButtons = new RedGreenButton[8];
	private final RedGreenButton[] controlButtons = new RedGreenButton[8];
	final List<MultiStateHardwareLight> sendALights = new ArrayList<>();
	final List<MultiStateHardwareLight> sendBLights = new ArrayList<>();
	final List<MultiStateHardwareLight> panLights = new ArrayList<>();
	private HardwareSurface surface;

	private MidiOut midiOut;

	private MidiIn midiIn;

	private ArpInstance currentArp = null;

	private final HashMap<String, ArpInstance> arpInstances = new HashMap<>();

	private String currentTrackName = "";
	private int currentTrackIndex = -1;
	private boolean arpSelected = false;
	private Layers layers;
	ButtonModeLayer currentMode = null;
	private DefaultButtonMode mainButtonLayer;
	private SpecificBitwigDevice arpdevice;
	private List<Parameter> arpGateParams;
	private List<Parameter> arpNoteParams;
	private List<Parameter> arpVelocityParams;
	private Parameter arpStepsParam;
	private IntegerValue arpStepPositionParam;
	private PinnableCursorDevice cursorDevice;
	private CursorTrack cursorTrack;
	private SettableEnumValue quantizeMode;
	private ArrayList<Parameter> arpSkipStepParameters;

	protected LpcArpControlExtension(final LpcArpControlExtensionDefinition definition, final ControllerHost host) {
		super(definition, host);
	}

	@Override
	public void init() {
		final ControllerHost host = getHost();

		layers = new Layers(this);
		// transport = host.createTransport();
		midiIn = host.getMidiInPort(0);
		midiIn.setMidiCallback((ShortMidiMessageReceivedCallback) msg -> onMidi0(msg));
		midiIn.setSysexCallback((final String data) -> onSysex0(data));
		midiOut = host.getMidiOutPort(0);
		surface = host.createHardwareSurface();
		cursorTrack = host.createCursorTrack(1, 1);
		final TrackBank trackBank = host.createTrackBank(16, 0, 1);

		trackBank.cursorIndex().addValueObserver(v -> {
			currentTrackIndex = v;
		});
		trackBank.followCursorTrack(cursorTrack);

		cursorDevice = cursorTrack.createCursorDevice();
		cursorTrack.name().addValueObserver(s -> {
			currentTrackName = s;
		});
		cursorDevice.name().addValueObserver(s -> {
			RemoteConsole.out.println("Device Selected = {} {}", s, cursorDevice.presetName().get());
			if (s.equals("Arpeggiator")) {
				arpSelected = true;
				currentMode.activate();
				selectArpInstance(currentTrackIndex, cursorDevice.presetName().get());
			} else if (currentArp != null) {
				currentArp = null;
				arpSelected = false;
				currentMode.deactivate();
				clearTemplate(0);
			}
		});
		cursorDevice.presetName().addValueObserver(pn -> {
			RemoteConsole.out.println("PN = {}", pn);
			if (arpSelected) {
				RemoteConsole.out.println(" ARP SElected TI={} TN={} PN={}", currentTrackIndex, currentTrackName, pn);
				selectArpInstance(currentTrackIndex, pn);
			}
		});
		setUpArpDevice(cursorDevice);
		setUpKnobs();
		for (int i = 0; i < 8; i++) {
			focusButtons[i] = new RedGreenButton(this, "FOUCS", i, TRACK_FOCUS_NOTE[i], BASE_CHANNEL);
			controlButtons[i] = new RedGreenButton(this, "CONTROL", i, TRACK_CONTROL_NOTE[i], BASE_CHANNEL);
		}

		final DocumentState documentState = getHost().getDocumentState();
		quantizeMode = documentState.getEnumSetting("Modes", //
				"Quantize",
				new String[] { QuantizeMode.MUTE.getDescriptor(), QuantizeMode.NEAREST_VALUE.getDescriptor() },
				QuantizeMode.MUTE.getDescriptor());
		quantizeMode.markInterested();
		quantizeMode.addValueObserver(newValue -> {
			final QuantizeMode newMode = QuantizeMode.toMode(newValue);
			RemoteConsole.out.println(" Q MODE => {}", newMode);
			for (final ArpInstance arp : arpInstances.values()) {
				arp.setQuantizeMode(newMode);
			}
		});

		setUpLayers();

		clearTemplate(0);
		host.showPopupNotification("LpcArpControl Initialized");
	}

	private void setUpLayers() {
		mainButtonLayer = new DefaultButtonMode(this, "BUTTON_MAIN");
		currentMode = mainButtonLayer;
		final ModeButton sendSelectUpButton = new ModeButton(this, Assignment.SEND_UP);
		final ModeButton sendSelectDownButton = new ModeButton(this, Assignment.SEND_DOWN);
		final Parameter octaves = arpdevice.createParameter("OCTAVES");
		octaves.markInterested();
		octaves.value().addRawValueObserver(v -> {
			sendSelectUpButton.getLed().isOn().setValue(v == 4 || v == 3);
			sendSelectDownButton.getLed().isOn().setValue(v == 2 || v == 4);
		});
		sendSelectUpButton.getHwButton().isPressed().addValueObserver(v -> {
			if (v) {
				octaves.value().incRaw(1);
			}
		});
		sendSelectDownButton.getHwButton().isPressed().addValueObserver(v -> {
			if (v) {
				octaves.value().incRaw(-1);
			}
		});

		final ModeButton trackLeftButton = new ModeButton(this, Assignment.TRACK_LEFT);
		final ModeButton trackRightButton = new ModeButton(this, Assignment.TRACK_RIGHT);

		final ModeButton armModeButton = new ModeButton(this, Assignment.ARM);
		final ModeButton deviceModeButton = new ModeButton(this, Assignment.DEVICE);
		final ModeButton soloModeButton = new ModeButton(this, Assignment.SOLO);
		final ModeButton muteModeButton = new ModeButton(this, Assignment.MUTE);

		final TimingButtonMode timingMode = new TimingButtonMode(this, "TIMING_LAYER");
		final PatternTypeButtonMode patternTypeMode = new PatternTypeButtonMode(this, "PATTERNTYPE_LAYER");
		final VelocityGateButtonMode velGateMode = new VelocityGateButtonMode(this, "MUTE_GATE_MODE");

		final QuantizeButtonMode quantizeSettingMode = new QuantizeButtonMode(this, "QUANTIZE_MODE");
		final GlobalVelocityButtonMode velocityButtonMode = new GlobalVelocityButtonMode(this, "GLOBAL_VELOCITYMODE");
		final GlobalGateButtonMode gateButtonMode = new GlobalGateButtonMode(this, "GLOBAL_GATEMODE");

		mainButtonLayer.bindModeToggle(deviceModeButton, timingMode);
		mainButtonLayer.bindModeToggle(soloModeButton, patternTypeMode);
		mainButtonLayer.bindModeToggle(muteModeButton, velGateMode);
		mainButtonLayer.bindModeToggle(armModeButton, quantizeSettingMode);
		mainButtonLayer.bindModeToggle(trackLeftButton, velocityButtonMode);
		mainButtonLayer.bindModeToggle(trackRightButton, gateButtonMode);

		mainButtonLayer.activate();
	}

	public PinnableCursorDevice getCursorDevice() {
		return cursorDevice;
	}

	public Parameter getArpStepsParam() {
		return arpStepsParam;
	}

	public List<Parameter> getArpSkipStepsParam() {
		return arpSkipStepParameters;
	}

	public IntegerValue getArpStepPositionParam() {
		return arpStepPositionParam;
	}

	public List<Parameter> getArpGateParams() {
		return arpGateParams;
	}

	public List<Parameter> getArpVelocityParams() {
		return arpVelocityParams;
	}

	public List<Parameter> getArpNoteParams() {
		return arpNoteParams;
	}

	public CursorTrack getCursorTrack() {
		return cursorTrack;
	}

	void toggleMode(final ButtonModeLayer mode) {
		assert mode != null;
		if (currentMode == mode) {
			currentMode.deactivate();
			currentMode = mainButtonLayer;
			currentMode.activate();
		} else {
			currentMode.deactivate();
			currentMode = mode;
			currentMode.activate();
		}
	}

	public void releaseMode() {
		if (currentMode != mainButtonLayer) {
			currentMode.deactivate();
			currentMode = mainButtonLayer;
			currentMode.activate();
		}
	}

	public ArpInstance getCurrentArp() {
		return currentArp;
	}

	void setMode(final ButtonModeLayer mode) {
		assert mode != null;

		if (currentMode == mode) {
			return;
		}

		currentMode.deactivate();
		currentMode = mode;
		currentMode.activate();

	}

	public RedGreenButton[] getFocusButtons() {
		return focusButtons;
	}

	public RedGreenButton[] getControlButtons() {
		return controlButtons;
	}

	public Layers getLayers() {
		return layers;
	}

	public HardwareSurface getSurface() {
		return surface;
	}

	public MidiIn getMidiIn() {
		return midiIn;
	}

	public void selectArpInstance(final int trackIndex, final String presetName) {
		// RemoteConsole.out.println(" SELELCT ARP Instance {}", presetName);
		final String key = presetName.length() == 0 ? "_track" + trackIndex : presetName;
		final ArpInstance inst = arpInstances.get(key);
		if (currentArp == null) {
			if (inst == null) {
				// RemoteConsole.out.println("Create Arp Instance {}", key);
				currentArp = new ArpInstance(key, this, QuantizeMode.toMode(quantizeMode.get()));
				arpInstances.put(key, currentArp);
			} else {
				// RemoteConsole.out.println("Reselect ARP");
				currentArp = inst;
			}
			surface.invalidateHardwareOutputState();
		} else if (!currentArp.getIdentifier().equals(key)) {
			// RemoteConsole.out.println("RENAME {} --> {}", currentArp.getIdentifier(),
			// presetName);
			arpInstances.remove(currentArp.getIdentifier());
			currentArp.setIdentifier(presetName);
			arpInstances.put(key, currentArp);
		}

	}

	@Override
	public void applyGateValueToParameter(final int index, final double value) {
		if (index < arpGateParams.size()) {
			arpGateParams.get(index).value().set(value);
		}
	}

	@Override
	public void applyNoteValueToParameter(final int index, final double value) {
		if (index < arpGateParams.size()) {
			arpNoteParams.get(index).value().set(value);
		}
	}

	@Override
	public double getGateValue(final int index) {
		if (index < arpGateParams.size()) {
			return arpGateParams.get(index).value().get();
		}
		return 0.0;
	}

	public void updatePadLed(final RedGreenButton button) {
		final ColorButtonLedState state = (ColorButtonLedState) button.getLight().state().currentValue();
		if (state != null) {
			midiOut.sendMidi(button.getMidiStatus(), button.getMidiDataNr(), state.getColorCode());
		} else {
			midiOut.sendMidi(button.getMidiStatus(), button.getMidiDataNr(), 0);
		}
	}

	public void sendLedUpdate(final Assignment assignement, final int value) {
		midiOut.sendMidi(assignement.getMidiStatus(), assignement.getDataValue(), value);
	}

	public void sendLightState(final int template, final int index, final int value) {
		buttonStateSysEx[7] = (byte) template;
		buttonStateSysEx[8] = (byte) index;
		buttonStateSysEx[9] = (byte) value;
		midiOut.sendSysex(buttonStateSysEx);
	}

	private void setUpKnobs() {
		final AbsoluteHardwareKnob[] sendAKnobs = new AbsoluteHardwareKnob[8];
		final AbsoluteHardwareKnob[] sendBKnobs = new AbsoluteHardwareKnob[8];
		final AbsoluteHardwareKnob[] panKnobs = new AbsoluteHardwareKnob[8];
		final AbsoluteHardwareKnob[] sliders = new AbsoluteHardwareKnob[8];

		for (int i = 0; i < 8; i++) {
			final int index = i;
			final AbsoluteHardwareKnob knob = surface.createAbsoluteHardwareKnob("SEND_A" + i);
			sendAKnobs[i] = knob;
			knob.setAdjustValueMatcher(midiIn.createAbsoluteCCValueMatcher(BASE_CHANNEL, SENDA_CC_INDEX + i));
			knob.value().addValueObserver(v -> {
				if (currentArp != null) {
					final int value = (int) (-24 + 48 * v);
					currentArp.setOffsetNote(index, value);
					arpNoteParams.get(index).value().set(currentArp.getNoteValue(index));
				}
			});
		}
		for (int i = 0; i < 8; i++) {
			final int index = i;
			final AbsoluteHardwareKnob knob = surface.createAbsoluteHardwareKnob("SEND_B" + i);
			sendBKnobs[i] = knob;
			knob.setAdjustValueMatcher(midiIn.createAbsoluteCCValueMatcher(BASE_CHANNEL, SENDB_CC_INDEX + i));
			knob.value().addValueObserver(value -> {
				arpVelocityParams.get(index).value().set(value);
			});
		}

		for (int i = 0; i < 8; i++) {
			final AbsoluteHardwareKnob knob = surface.createAbsoluteHardwareKnob("PAN_" + i);
			final int index = i;
			panKnobs[i] = knob;
			knob.setAdjustValueMatcher(midiIn.createAbsoluteCCValueMatcher(BASE_CHANNEL, PAN_CC_INDEX + i));
			knob.value().addValueObserver(value -> {
				if (currentArp != null) {
					currentArp.updateGateParam(index, arpGateParams.get(index), value);
				}
			});
		}

		for (int i = 0; i < 8; i++) {
			final int index = i;
			final AbsoluteHardwareKnob knob = surface.createAbsoluteHardwareKnob("SLIDER_" + i);
			sliders[i] = knob;
			knob.setAdjustValueMatcher(midiIn.createAbsoluteCCValueMatcher(BASE_CHANNEL, SLIDER_CC_INDEX + i));
			knob.value().addValueObserver(v -> {
				if (currentArp != null) {
					final int value = (int) (-24 + 48 * v);
					currentArp.setBaseNote(index, value);
					arpNoteParams.get(index).value().set(currentArp.getNoteValue(index));
				}
			});
		}
	}

	private void setUpArpDevice(final PinnableCursorDevice cursorDevice) {
		arpdevice = cursorDevice.createSpecificBitwigDevice(UUID.fromString("4d407a2b-c91b-4e4c-9a89-c53c19fe6251"));

		arpGateParams = new ArrayList<>();
		arpNoteParams = new ArrayList<>();
		arpVelocityParams = new ArrayList<>();
		arpSkipStepParameters = new ArrayList<>();
		for (int i = 0; i < 16; i++) {
			final Parameter gate = arpdevice.createParameter("GATE_" + (i + 1));
			final Parameter velocity = arpdevice.createParameter("STEP_" + (i + 1));
			final Parameter note = arpdevice.createParameter("STEP_" + (i + 1) + "_TRANSPOSE");
			final Parameter skip = arpdevice.createParameter("SKIP_" + (i + 1));
			gate.markInterested();
			velocity.markInterested();
			note.markInterested();
			skip.markInterested();
			arpGateParams.add(gate);
			arpNoteParams.add(note);
			arpVelocityParams.add(velocity);
			arpSkipStepParameters.add(skip);
		}

		RemoteConsole.out.println("INIT ");
//		cursorDevice.addDirectParameterIdObserver(newValue -> {
//			RemoteConsole.out.println("PARAMETER");
//			for (final String pname : newValue) {
//				RemoteConsole.out.println(" :[{}]", pname);
//			}
//		});
//		final Parameter syncedTimes = arpdevice.createParameter("SYNCED_TIMES");
//		final Parameter rateInTimes = arpdevice.createParameter("RATE_IN_TIME");
//		final Parameter overlappingNotes = arpdevice.createParameter("OVERLAPPING_NOTES");
//		final Parameter freeNoteStart = arpdevice.createParameter("FREE_NOTE_START");
//		final Parameter humanize = arpdevice.createParameter("HUMANIZE");
//		final Parameter terminateNotes = arpdevice.createParameter("IMMEDIATELY_TERMINATE_NOTES");
//		final Parameter enableTransposition = arpdevice.createParameter("ENABLE_TRANSPOSITION");
//		final Parameter octaveBehavior = arpdevice.createParameter("OCTAVE_BEHAVIOR");
		arpStepsParam = arpdevice.createParameter("STEPS");
		arpStepPositionParam = arpdevice.createIntegerOutputValue("STEP");
		arpStepPositionParam.markInterested();
		arpStepsParam.markInterested();

		for (int i = 0; i < 8; i++) {
			final int index = i;
			final Parameter noteParm = arpNoteParams.get(index);
			final MultiStateHardwareLight light = surface.createMultiStateHardwareLight("SENDA_LIGHT_" + i);
			sendALights.add(light);
			light.state().setValue(noteValueToColor(noteParm.value().get()));
			light.state().onUpdateHardware(hwState -> {
				if (currentArp != null) {
					final ColorButtonLedState state = (ColorButtonLedState) light.state().currentValue();
					sendLightState(0, index, state.getColorCode());
				} else {
					sendLightState(0, index, ColorButtonLedState.OFF.getColorCode());
				}
			});
			noteParm.value().addRawValueObserver(v -> {
				if (currentArp != null) {
					light.state().setValue(noteValueToColor(v));
				} else {
					light.state().setValue(ColorButtonLedState.OFF);
				}
			});
		}

		for (int i = 0; i < 8; i++) {
			final int index = i;
			final Parameter velocityParam = arpVelocityParams.get(index);
			final MultiStateHardwareLight light = surface.createMultiStateHardwareLight("SENDB_LIGHT_" + i);
			sendBLights.add(light);
			light.state().setValue(percentToGreenColor(velocityParam.value().get()));
			light.state().onUpdateHardware(hwState -> {
				if (currentArp != null) {
					final ColorButtonLedState state = (ColorButtonLedState) light.state().currentValue();
					sendLightState(0, index + 8, state.getColorCode());
				} else {
					light.state().setValue(ColorButtonLedState.OFF);
				}
			});
			velocityParam.value().addRawValueObserver(v -> {
				light.state().setValue(percentToGreenColor(v));
			});
		}
		for (int i = 0; i < 8; i++) {
			final int index = i;
			final MultiStateHardwareLight light = surface.createMultiStateHardwareLight("PAN_LIGHT_" + i);
			final Parameter gateParam = arpGateParams.get(index);
			panLights.add(light);
			light.state().setValue(percentToAmberColor(gateParam.value().get()));
			light.state().onUpdateHardware(hwState -> {
				if (currentArp != null) {
					final ColorButtonLedState state = (ColorButtonLedState) light.state().currentValue();
					sendLightState(0, index + 16, state.getColorCode());
				} else {
					light.state().setValue(ColorButtonLedState.OFF);
				}
			});
			gateParam.value().addRawValueObserver(v -> {
				light.state().setValue(percentToAmberColor(v));
			});
		}
	}

	private ColorButtonLedState percentToGreenColor(final double vel) {
		if (currentArp == null || vel == 0) {
			return ColorButtonLedState.OFF;
		}
		if (vel == 1) {
			return ColorButtonLedState.GREEN_FULL;
		}
		if (vel < 0.5) {
			return ColorButtonLedState.GREEN_DIM;
		}
		return ColorButtonLedState.GREEN_SEMI;
	}

	private ColorButtonLedState percentToAmberColor(final double vel) {
		if (currentArp == null || vel == 0) {
			return ColorButtonLedState.OFF;
		}
		if (vel == 1) {
			return ColorButtonLedState.AMBER_FULL;
		}
		if (vel < 0.5) {
			return ColorButtonLedState.AMBER_DIM;
		}
		return ColorButtonLedState.AMBER_SEMI;
	}

	private ColorButtonLedState noteValueToColor(final double v) {
		if (v == 0) {
			return ColorButtonLedState.YELLOW_FULL;
		}
		if (v == -12 || v == -24) {
			return ColorButtonLedState.RED_FULL;
		}
		if (v < -12) {
			return ColorButtonLedState.RED_SEMI;
		}
		if (v < 0) {
			return ColorButtonLedState.RED_DIM;
		}
		if (v == 12 || v == 24) {
			return ColorButtonLedState.GREEN_FULL;
		}
		if (v > 12) {
			return ColorButtonLedState.GREEN_DIM;
		}
		return ColorButtonLedState.GREEN_SEMI;
	}

	public SpecificBitwigDevice getArpdevice() {
		return arpdevice;
	}

	@Override
	public void exit() {
		getHost().showPopupNotification("LpcArpControl Exited");
	}

	@Override
	public void flush() {
		surface.updateHardware();
	}

	public void clearTemplate(final int templateNr) {
		midiOut.sendMidi(0xB0 | templateNr, 0, 0);
	}

	public void enableFlashing(final int templateNr) {
		midiOut.sendMidi(0xB0 | templateNr, 0, 0x28);
	}

	/** Called when we receive short MIDI message on port 0. */
	private void onMidi0(final ShortMidiMessage msg) {
		// if(msg.getData1() == )
	}

//	private void sendLight(int light, int value) {
//		String header = "F0002029021178"+"00" + ""
//	}

	/** Called when we receive sysex MIDI message on port 0. */
	private void onSysex0(final String data) {
		RemoteConsole.out.println("SYS EX = {}", data);
	}

}
