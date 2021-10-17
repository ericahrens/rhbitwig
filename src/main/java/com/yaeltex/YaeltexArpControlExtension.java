package com.yaeltex;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.bitwig.extension.api.Color;
import com.bitwig.extension.api.util.midi.ShortMidiMessage;
import com.bitwig.extension.callback.ShortMidiMessageReceivedCallback;
import com.bitwig.extension.controller.ControllerExtension;
import com.bitwig.extension.controller.api.BooleanValue;
import com.bitwig.extension.controller.api.ControllerHost;
import com.bitwig.extension.controller.api.HardwareActionBindable;
import com.bitwig.extension.controller.api.HardwareSurface;
import com.bitwig.extension.controller.api.MidiIn;
import com.bitwig.extension.controller.api.MidiOut;
import com.bitwig.extension.controller.api.Parameter;
import com.bitwig.extension.controller.api.RelativeHardwareKnob;
import com.bitwig.extension.controller.api.RelativePosition;
import com.bitwig.extension.controller.api.TrackBank;
import com.bitwig.extensions.framework.Layers;
import com.bitwig.extensions.rh.Midi;
import com.yaeltex.debug.RemoteConsole;
import com.yaeltex.device.ArpInstance;
import com.yaeltex.device.DeviceSlotState;
import com.yaeltex.device.DeviceSlotStateValueObject;
import com.yaeltex.device.FocusDevice;
import com.yaeltex.encoders.Encoder16StepLayout;
import com.yaeltex.encoders.Encoder2Device8StepLayout;
import com.yaeltex.encoders.Encoder8StepLayout;
import com.yaeltex.encoders.EncoderLayout;
import com.yaeltex.layer.OptionsButtonLayer;
import com.yaeltex.layer.StepGatePctButtonLayer;
import com.yaeltex.layer.StepMuteButtonLayer;
import com.yaeltex.layer.StepVelMuteButtonLayer;
import com.yaeltex.layer.StepVelocityPctButtonLayer;
import com.yaeltex.layer.YaeltexLayer;
import com.yaeltex.value.DeviceValueObject;
import com.yaeltex.value.SettableValueObject;

public class YaeltexArpControlExtension extends ControllerExtension {
	private HardwareSurface surface;
	private MidiIn midiIn;
	private MidiOut midiOut;

	public static final int[] BUTTON_INDEX = new int[] { //
			56, 40, 64, 48 };

	public static final int MODE_BUTTON_OFFSET = 32;

	private final Map<ArpDisplayModeType, EncoderLayout> layoutMap = new HashMap<>();

	private Layers layers;
	private OptionsButtonLayer rowButtonLayer;

	private final int[] lastCcValue = new int[128];

	final RgbButton[] encoderButtons = new RgbButton[32];
	final RgbButton[] topRowButtons = new RgbButton[16];
	final RgbButton[] bottomRowButtons = new RgbButton[16];
	final RgbButton[] modeButtons = new RgbButton[8];

	final RelativeHardwareKnob[] encoders = new RelativeHardwareKnob[32];

	DeviceSlotStateValueObject slot1State = new DeviceSlotStateValueObject();
	DeviceSlotStateValueObject slot2State = new DeviceSlotStateValueObject();
	private FocusDevice cursorDevice;
	private FocusDevice fixedDevice1;
	private FocusDevice fixedDevice2;
	private final SettableValueObject<EncoderLayout> currentEncoderLayout = new SettableValueObject<>();
	private final SettableValueObject<YaeltexLayer> currentButtonMode = new SettableValueObject<>();
	private final SettableValueObject<Integer> focusedDeviceSlot = new SettableValueObject<>();
	private YaeltexLayer mainLayer;

	private final List<ArpInstance> arpInstances = new ArrayList<>();
	private int currentTrackIndex;
	private DeviceValueObject stepLengthValues;
	private DeviceValueObject globalVelocityValues;
	private DeviceValueObject globalGateValues;
	private YaeltexLayer timeWarpLayer;
	// private SettableEnumValue quantizeMode;

	private final SettableValueObject<Double> timeWarpEnterValue = new SettableValueObject<>();

	protected YaeltexArpControlExtension(final YaeltexArpControlExtensionDefinition definition,
			final ControllerHost host) {
		super(definition, host);
	}

	@Override
	public void init() {
		final ControllerHost host = getHost();
		focusedDeviceSlot.setValue(0);
		for (int i = 0; i < lastCcValue.length; i++) {
			lastCcValue[i] = -1;
		}
		layers = new Layers(this);
		midiIn = host.getMidiInPort(0);
		midiIn.setMidiCallback((ShortMidiMessageReceivedCallback) msg -> onMidi0(msg));
		midiOut = host.getMidiOutPort(0);
		surface = host.createHardwareSurface();

		setUpHardware();

		final Encoder8StepLayout startLayout = new Encoder8StepLayout(this);
		layoutMap.put(ArpDisplayModeType.MODE_1X8, startLayout);
		layoutMap.put(ArpDisplayModeType.MODE_2X8, new Encoder2Device8StepLayout(this));
		layoutMap.put(ArpDisplayModeType.MODE_1X16, new Encoder16StepLayout(this));
		currentEncoderLayout.setValue(startLayout);

		cursorDevice = new FocusDevice(0, true, this);
		fixedDevice1 = new FocusDevice(1, false, this);
		fixedDevice2 = new FocusDevice(2, false, this);

		stepLengthValues = new DeviceValueObject(2);
		globalVelocityValues = new DeviceValueObject(2);
		globalGateValues = new DeviceValueObject(2);

		final BooleanValue evx1 = cursorDevice.getCursorDevice().createEqualsValue(fixedDevice1.getCursorDevice());
		final BooleanValue evx2 = cursorDevice.getCursorDevice().createEqualsValue(fixedDevice2.getCursorDevice());

		evx1.addValueObserver(v -> {
			fixedDevice1.setIsOnCursorDevice(v);
		});
		evx2.addValueObserver(v -> {
			fixedDevice2.setIsOnCursorDevice(v);
		});

		final TrackBank trackBank = host.createTrackBank(16, 0, 1);
		trackBank.cursorIndex().addValueObserver(v -> {
			currentTrackIndex = v;
			if (v != -1) {
				cursorDevice.notifyTrackIndex(v);
				fixedDevice1.notifyTrackIndex(v);
				fixedDevice1.notifyTrackIndex(v);
			}
		});
		trackBank.followCursorTrack(cursorDevice.getCursorTrack());
//		cursorDevice.getCursorDevice().addDirectParameterIdObserver(newValue -> {
//			RemoteConsole.out.println("PARAMETER");
//			for (final String pname : newValue) {
//				RemoteConsole.out.println(" :[{}]", pname);
//			}
//		});
		surface.setPhysicalSize(310, 234);
		setGuiSimulation();

		mainLayer = new YaeltexLayer(this, "Main");
		mainLayer.activate();

//		final DocumentState documentState = getHost().getDocumentState();

//		quantizeMode = documentState.getEnumSetting("Modes", //
//				"Quantize", QuantizeMode.getDescriptors(), QuantizeMode.MUTE.getDescriptor());
//		quantizeMode.markInterested();
//		quantizeMode.addValueObserver(newValue -> {
//			final QuantizeMode newMode = QuantizeMode.toMode(newValue);
//			RemoteConsole.out.println(" Q MODE => {}", newMode);
//			for (final ArpInstance arp : arpInstances) {
//				arp.setQuantizeMode(newMode);
//			}
//		});

		initButtonLayers();
		initSpecialButtons();

		startLayout.activate();
		currentEncoderLayout.get().activate();
		currentButtonMode.get().activate();

		host.showPopupNotification("SEQ ARP 168 Initialized");
	}

	private void initButtonLayers() {
		rowButtonLayer = new OptionsButtonLayer(this);
		final StepMuteButtonLayer stepMuteLayer = new StepMuteButtonLayer(this);
		final StepVelMuteButtonLayer stepVelMuteLayer = new StepVelMuteButtonLayer(this);
		final StepGatePctButtonLayer stepGatesPctLayer = new StepGatePctButtonLayer(this);
		final StepVelocityPctButtonLayer stepVelPctLayer = new StepVelocityPctButtonLayer(this);
		final RgbButton optionsButton = modeButtons[RgbButton.OPTIONS_MODE];

		initTimeWarpLayer();

		mainLayer.bindPressed(optionsButton.getHwButton(), () -> {
			if (currentButtonMode.get() == rowButtonLayer) {
				rowButtonLayer.switchInternalMode();
			} else {
				setButtonMode(rowButtonLayer);
			}
		});
		mainLayer.bindLightState(rowButtonLayer::getModeColor, optionsButton);

		mainLayer.bindButtonMode(modeButtons[RgbButton.STEPS_MT_MODE], stepMuteLayer, ColorButtonLedState.GREEN);
		mainLayer.bindButtonMode(modeButtons[RgbButton.VEL_MT_MODE], stepVelMuteLayer, ColorButtonLedState.GREEN);
		mainLayer.bindButtonMode(modeButtons[RgbButton.STEPS_GPCT_MODE], stepGatesPctLayer, ColorButtonLedState.GREEN);
		mainLayer.bindButtonMode(modeButtons[RgbButton.STEPS_VPCT_MODE], stepVelPctLayer, ColorButtonLedState.GREEN);
		currentButtonMode.setValue(rowButtonLayer);
	}

	public void initTimeWarpLayer() {
		timeWarpLayer = new YaeltexLayer(this, "TIME_WARP_LAYER");
		for (int i = 0; i < 7; i++) {
			bindRateButton(timeWarpLayer, bottomRowButtons[i], i);
		}
		bindShuffeButton(timeWarpLayer, bottomRowButtons[7]);
		bindDeviceToggle(timeWarpLayer, topRowButtons[15]);

		final RgbButton timeWarpButton = modeButtons[RgbButton.TIME_WARP_MODE];
		mainLayer.bindPressed(timeWarpButton.getHwButton(), () -> {

			timeWarpLayer.activate();
			final FocusDevice device = getFocussedDevice();
			if (!device.isArp()) {
				return;
			}
			final Parameter rate = getFocussedDevice().getRateParam();
			timeWarpEnterValue.setValue(rate.getAsDouble());
			RemoteConsole.out.println(" TW PRESSED enter Rate = {} ", rate.getAsDouble());
		});
		mainLayer.bindReleased(timeWarpButton.getHwButton(), () -> {
			RemoteConsole.out.println(" TW REleased ");
			final FocusDevice device = getFocussedDevice();
			if (!device.isArp()) {
				return;
			}
			final Parameter rate = getFocussedDevice().getRateParam();
			rate.set(timeWarpEnterValue.get());
			timeWarpLayer.deactivate();
		});
		mainLayer.bindLightState(() -> {
			if (timeWarpButton.getHwButton().isPressed().get()) {
				return ColorButtonLedState.WHITE;
			}
			return ColorButtonLedState.OFF;
		}, timeWarpButton.getLight());
	}

	private void initSpecialButtons() {
		final RgbButton retrig = modeButtons[RgbButton.RETRIG_MODE];
		mainLayer.bindPressed(retrig.getHwButton(), () -> {
			final FocusDevice device = getDevice(0);
			if (device.isArp()) {
				final double v = device.getRetriggerParam().value().get();
				if (v == 0) {
					device.getRetriggerParam().value().setImmediately(1);
				} else {
					device.getRetriggerParam().value().setImmediately(0);
				}
			}
		});
		mainLayer.bindReleased(retrig.getHwButton(), () -> {

		});
		mainLayer.bindLightState(() -> {
			final FocusDevice device = getDevice(0);
			if (device.isArp()) {
				if (device.getRetriggerParam().value().get() == 0) {
					return ColorButtonLedState.WHITE;
				} else {
					return ColorButtonLedState.colorFor(96);
				}
			}
			return ColorButtonLedState.OFF;
		}, retrig);
	}

	public void pinDevice(final int slotIndex) {
		if (slotIndex == 0) {
			if (fixedDevice1.isPinned()) {
				fixedDevice1.getCursorDevice().isPinned().set(false);
				applyDeviceToSlot(slot1State, fixedDevice1);
			} else if (cursorDevice.isArp() && !fixedDevice2.isOnCursorDevice()) {
				fixedDevice1.set(cursorDevice, true);
				applyDeviceToSlot(slot1State, fixedDevice1);
			} else if (fixedDevice1.isArp()) {
				fixedDevice1.getCursorDevice().isPinned().set(true);
			}
		} else {
			if (fixedDevice2.isPinned()) {
				fixedDevice2.getCursorDevice().isPinned().set(false);
				applyDeviceToSlot(slot2State, fixedDevice2);
			} else if (cursorDevice.isArp() && !fixedDevice1.isOnCursorDevice()) {
				fixedDevice2.set(cursorDevice, true);
				applyDeviceToSlot(slot2State, fixedDevice2);
			} else if (fixedDevice2.isArp()) {
				fixedDevice2.getCursorDevice().isPinned().set(true);
			}
		}
	}

	public void bindDeviceToggle(final YaeltexLayer layer, final RgbButton button) {
		layer.bindPressed(button, () -> {
			final FocusDevice device = getFocussedDevice();
			if (!device.isArp()) {
				return;
			}
			toggleFocussedDeviceSlot();
		});
		layer.bindLightState(() -> {
			final FocusDevice device = getFocussedDevice();
			if (!device.isArp()) {
				return ColorButtonLedState.OFF;
			}
			return getFocusedDeviceSlot().get() == 0 ? ColorButtonLedState.PURPLE : ColorButtonLedState.BLUE_ACTIVE;
		}, button);
	}

	public void bindShuffeButton(final YaeltexLayer layer, final RgbButton button) {
		final ColorButtonLedState activeColor = new ColorButtonLedState(YaelTexColors.DARK_ORANGE, 0);
		final ColorButtonLedState nonActiveColor = ColorButtonLedState.colorFor(YaelTexColors.DARK_ORANGE, 2);

		layer.bindPressed(button, () -> {
			final FocusDevice device = getFocussedDevice();
			if (!device.isArp()) {
				return;
			}
			final Parameter shuffle = device.getShuffleParam();
			final double v = shuffle.get();
			shuffle.setImmediately(v == 0 ? 1.0 : 0.0);
		});
		layer.bindLightState(() -> {
			final FocusDevice device = getFocussedDevice();
			if (!device.isArp()) {
				return ColorButtonLedState.OFF;
			}
			final Parameter shurffleParam = device.getShuffleParam();
			return shurffleParam.get() > 0 ? activeColor : nonActiveColor;
		}, button);
	}

	public void bindRateButton(final YaeltexLayer layer, final RgbButton button, final int matchValue) {
		final ColorButtonLedState activeColor = new ColorButtonLedState(YaelTexColors.DARK_ORANGE, 0);
		final ColorButtonLedState nonActiveColor = ColorButtonLedState.colorFor(0);
		layer.bindPressed(button, () -> {
			final FocusDevice device = getFocussedDevice();
			if (!device.isArp()) {
				return;
			}
			final Parameter rate = getFocussedDevice().getRateParam();
			rate.set(matchValue / 6.0);
		});
		layer.bindLightState(() -> {
			final FocusDevice device = getFocussedDevice();
			if (!device.isArp()) {
				return ColorButtonLedState.OFF;
			}
			final Parameter rate = getFocussedDevice().getRateParam();
			final int value = (int) (rate.get() * 6);
			return value == matchValue ? activeColor : nonActiveColor;
		}, button);
	}

	public RgbButton getModeButtons(final int index) {
		assert index < modeButtons.length;
		return modeButtons[index];
	}

	public int getCurrentTrackIndex() {
		return currentTrackIndex;
	}

	public SettableValueObject<Integer> getFocusedDeviceSlot() {
		return focusedDeviceSlot;
	}

	public void toggleFocussedDeviceSlot() {
		if (focusedDeviceSlot.get() == 0) {
			final FocusDevice device = getDevice(1);
			if (device != null && device.isArp()) {
				focusedDeviceSlot.setValue(1);
				currentEncoderLayout.get().refresh();
			}
		} else {
			focusedDeviceSlot.setValue(0);
			currentEncoderLayout.get().refresh();
		}
	}

	public void setButtonMode(final YaeltexLayer mode) {
		if (currentButtonMode.get() != mode) {
			currentButtonMode.get().deactivate();
			currentButtonMode.setValue(mode);
			currentButtonMode.get().activate();
		}
	}

	public void notifyPinned(final FocusDevice device, final boolean pinned) {
		final boolean isArp = device.isArp();

		if (isArp && !device.isInitalized() && device.getWhich() > 0) {
			assignFixedDeviceToSlot(device);
		}

		if (device.getWhich() == 1) {
			if (isArp) {
				slot1State.set(pinned ? DeviceSlotState.PINNED : DeviceSlotState.FOLLOW);
			} else {
				slot1State.set(cursorDevice.isArp() ? DeviceSlotState.FOLLOW : DeviceSlotState.EMPTY);
			}
		} else if (device.getWhich() == 2) {
			if (isArp) {
				slot2State.set(pinned ? DeviceSlotState.PINNED : DeviceSlotState.FOLLOW);
			} else {
				slot2State.set(cursorDevice.isArp() ? DeviceSlotState.FOLLOW : DeviceSlotState.EMPTY);
			}
		} else if (device.getWhich() == 0) {
			if (!fixedDevice1.isArp()) {
				slot1State.set(cursorDevice.isArp() ? DeviceSlotState.FOLLOW : DeviceSlotState.EMPTY);
			}
			if (!fixedDevice2.isArp()) {
				slot2State.set(cursorDevice.isArp() ? DeviceSlotState.FOLLOW : DeviceSlotState.EMPTY);
			}
		}
	}

	private void assignFixedDeviceToSlot(final FocusDevice device) {
		// RemoteConsole.out.println("INIT SHIT {}", device.getWhich());
		if (device.getWhich() == 1) {
			applyDeviceToSlot(slot1State, fixedDevice1);
			device.setInitialized();
		} else if (device.getWhich() == 2) {
			applyDeviceToSlot(slot2State, fixedDevice2);
			device.setInitialized();
		}
		currentEncoderLayout.get().refresh();
	}

	private void applyDeviceToSlot(final DeviceSlotStateValueObject state, final FocusDevice device) {
		final boolean isArp = device.isArp();
		if (isArp) {
			state.set(device.isPinned() ? DeviceSlotState.PINNED : DeviceSlotState.FOLLOW);
		} else {
			state.set(DeviceSlotState.EMPTY);
		}
	}

	public DeviceValueObject getStepLengthValues() {
		return stepLengthValues;
	}

	public DeviceValueObject getGlobalGateValues() {
		return globalGateValues;
	}

	public DeviceValueObject getGlobalVelocityValues() {
		return globalVelocityValues;
	}

	public HardwareSurface getSurface() {
		return surface;
	}

	public RgbButton[] getEncoderButtons() {
		return encoderButtons;
	}

	public RgbButton[] getTopRowButtons() {
		return topRowButtons;
	}

	public RgbButton[] getBottomRowButtons() {
		return bottomRowButtons;
	}

	public MidiIn getMidiIn() {
		return midiIn;
	}

	public Layers getLayers() {
		return layers;
	}

	public DeviceSlotStateValueObject getSlot1State() {
		return slot1State;
	}

	public DeviceSlotStateValueObject getSlot2State() {
		return slot2State;
	}

	private void setGuiSimulation() {
		// surface.hardwareElementWithId("ENDLESKNOB_0").setBounds(16.5, 16.75, 10.0,
		// 10.0);
		for (int i = 0; i < 32; i++) {
			encoders[i].setBounds(30 + 35 * (i % 8), 65 + 35 * (i / 8), 15, 15);
			encoders[i].setLabel("Encoder " + (i + 1));
			encoders[i].setLabelColor(Color.fromHex("f00"));
			encoders[i].setLabelPosition(RelativePosition.BELOW);
		}
	}

	private void setUpHardware() {
		for (int i = 0; i < 32; i++) {
			final int index = i;
			final int midiValue = EncoderUtil.ENCODER_INDEX[i];
			final RelativeHardwareKnob encoder = surface.createRelativeHardwareKnob("ENDLESKNOB_" + i);

			encoders[index] = encoder;
			encoder.setAdjustValueMatcher(midiIn.createRelativeBinOffsetCCValueMatcher(0, midiValue, 64));
			encoder.setStepSize(1 / 64.0);

			final HardwareActionBindable incAction = getHost().createAction(() -> {
				currentEncoderLayout.get().handleEncoderChange(index, 1);
			}, () -> "+");
			final HardwareActionBindable decAction = getHost().createAction(() -> {
				currentEncoderLayout.get().handleEncoderChange(index, -1);
			}, () -> "-");
			encoder.addBinding(getHost().createRelativeHardwareControlStepTarget(incAction, decAction));
			sendCC(midiValue, 0);
		}
		for (int i = 0; i < 32; i++) {
			final int midiValue = EncoderUtil.ENCODER_INDEX[i];
			encoderButtons[i] = new RgbButton(this, "ENCODER_BUTTON", i, midiValue, 0);
		}
		for (int i = 0; i < 16; i++) {
			final int midiValue = BUTTON_INDEX[i / 8] + i % 8;
			topRowButtons[i] = new RgbButton(this, "TOP_ROW_BUTTON", i, midiValue, 0);
		}
		for (int i = 0; i < 16; i++) {
			final int midiValue = BUTTON_INDEX[i / 8 + 2] + i % 8;
			bottomRowButtons[i] = new RgbButton(this, "BOTTOM_ROW_BUTTON", i, midiValue, 0);
		}
		for (int i = 0; i < 8; i++) {
			modeButtons[i] = new RgbButton(this, "MODE_BUTTON", i, i + MODE_BUTTON_OFFSET, 0);
		}
	}

	int changeValue(final int value, final int amount) {
		return Math.min(Math.max(0, value + amount), 127);
	}

	private void onMidi0(final ShortMidiMessage msg) {
	}

	@Override
	public void exit() {
		getHost().showPopupNotification("YaeltexArpControl Exited");
	}

	@Override
	public void flush() {
		surface.updateHardware();
	}

	public SettableValueObject<EncoderLayout> getCurrentEncoderLayout() {
		return currentEncoderLayout;
	}

	public FocusDevice getDevice(final int index) {
		if (index == 0) {
			if (fixedDevice1.isArp()) {
				return fixedDevice1;
			} else if (!fixedDevice1.isPinned()) {
				return cursorDevice;
			}
			return fixedDevice1;
		} else {
			return fixedDevice2;
		}
	}

	private int count = 0;

	public void sendCC(final int ccNr, final int value) {
		if (lastCcValue[ccNr] == -1 || lastCcValue[ccNr] != value) {
			midiOut.sendMidi(Midi.CC, ccNr, value);
			lastCcValue[ccNr] = value;
			count++;
			if (count % 128 == 0) { // Device seems get into hickups when bombarded with CCS
				pause(10); // This giving it some air seems to improve things
			}
		}
	}

	public void pause(final int amount) {
		try {
			Thread.sleep(amount);
			if (count > 10000) {
				count = 1;
			}
		} catch (final InterruptedException e) {
		}
	}

	public void initButton(final RgbButton button) {
		midiOut.sendMidi(button.getMidiStatus(), button.getMidiDataNr(), 0);
		count++;
	}

	public void updatePadLed(final RgbButton button) {
		final ColorButtonLedState state = button.getLedState();
		if (state != null) {
			midiOut.sendMidi(button.getMidiStatus(), button.getMidiDataNr(), state.getColorCode());
		} else {
			midiOut.sendMidi(button.getMidiStatus(), button.getMidiDataNr(), 0);
		}
	}

	public void selectArpLayoutMode(final ArpDisplayModeType modeAssign) {
		final EncoderLayout selectedLayout = layoutMap.get(modeAssign);
		if (selectedLayout != null && selectedLayout != currentEncoderLayout.get()) {
			currentEncoderLayout.get().deactivate();
			currentEncoderLayout.setValue(selectedLayout);
			selectedLayout.activate();
		}
	}

	public ArpInstance getArpInstance(final String trackName, final String presetName) {
		RemoteConsole.out.println("GET Arp Instance tn={}  pn=<{}> tnofai={}", trackName, presetName,
				arpInstances.size());
		final Optional<ArpInstance> arpOpt = arpInstances //
				.stream() //
				.filter(arp -> arp.matches(trackName, presetName)) //
				.findFirst();

		if (arpOpt.isEmpty()) {
			RemoteConsole.out.println(" CREATE NEW ARP INSTANCE tn={} pn={}", trackName, presetName);
			final ArpInstance arpInstance = new ArpInstance(trackName, presetName, QuantizeMode.NEAREST_VALUE);
			arpInstances.add(arpInstance);
			return arpInstance;
		} else {
			RemoteConsole.out.println(" Existing ARP Instance for tn={} pn={}", trackName, presetName);
			return arpOpt.get();
		}
	}

	public FocusDevice getFocussedDevice() {
		return getDevice(focusedDeviceSlot.get());
	}

}
