package com.yaeltex.encoders;

import com.bitwig.extension.controller.api.InternalHardwareLightState;
import com.yaeltex.ArpDisplayModeType;
import com.yaeltex.ColorButtonLedState;
import com.yaeltex.EncoderUtil;
import com.yaeltex.RgbButton;
import com.yaeltex.YaeltexArpControlExtension;
import com.yaeltex.device.FocusDevice;
import com.yaeltex.layer.MuteState;

public class Encoder16StepLayout extends EncoderLayout {

	private static final int ALT_PARAM_OFF = 16;

	private enum Mode {
		VELOCITY, GATE;
	}

	private Mode mode = Mode.GATE;

	public Encoder16StepLayout(final YaeltexArpControlExtension driver) {
		super(driver, ArpDisplayModeType.MODE_1X16);
		final RgbButton[] encoderButtons = driver.getEncoderButtons();

		for (int i = 0; i < 16; i++) {
			final int index = i;
			final RgbButton stepSkipButton = encoderButtons[index];
			bindPressed(stepSkipButton, () -> getDevice(0).ifPresent(d -> toggleStepSkip(d, index)));
			bindLightState(() -> getButtonColorNotes(index), stepSkipButton);
		}
		for (int i = 16; i < 32; i++) {
			final int index = i;
			final RgbButton stepSkipButton = encoderButtons[index];
			bindPressed(stepSkipButton, () -> {
				if (mode == Mode.GATE) {
					toggleGateMute(index - 16);
				} else {
					toggleVelMute(index - 16);
				}
			});
			bindLightState(() -> getButtonGates(index - ALT_PARAM_OFF), stepSkipButton);
		}
	}

	@Override
	public void nextEncMode() {
		if (mode == Mode.GATE) {
			mode = Mode.VELOCITY;
		} else {
			mode = Mode.GATE;
		}
		refresh();
	}

	@Override
	public InternalHardwareLightState getEncModeLight() {
		if (mode == Mode.GATE) {
			return ColorButtonLedState.YELLOW;
		}
		return ColorButtonLedState.BLUE;
	}

	@Override
	public void handleNoteUpdate(final FocusDevice deviceSource, final int index, final int value) {
		final FocusDevice device = getTarget(currentDeviceSlot, deviceSource, index);
		if (device == null) {
			return;
		}
		final int outvalue = EncoderUtil.NOTE_ENCODER_MAPPING[value];
		final int midiValue = EncoderUtil.ENCODER_INDEX[index];
		getDriver().sendCC(midiValue, outvalue);
	}

	@Override
	public void handleNoteOffsetUpdate(final FocusDevice deviceSource, final int index, final int value) {
	}

	@Override
	public void handleVelocityUpdate(final FocusDevice deviceSource, final int index, final int value) {
		final FocusDevice device = getTarget(currentDeviceSlot, deviceSource, index);
		if (device == null || mode != Mode.VELOCITY) {
			return;
		}

		final int midiValue = EncoderUtil.ENCODER_INDEX[index + ALT_PARAM_OFF];
		getDriver().getEncoderButtons()[index + ALT_PARAM_OFF].setColor(getButtonVelocity(0, index));
		getDriver().sendCC(midiValue, value);
	}

	@Override
	public void handleGateUpdate(final FocusDevice deviceSource, final int index, final int value) {
		final FocusDevice device = getTarget(currentDeviceSlot, deviceSource, index);
		if (device == null || mode != Mode.GATE) {
			return;
		}

		final int midiValue = EncoderUtil.ENCODER_INDEX[index + ALT_PARAM_OFF];
		getDriver().getEncoderButtons()[index + ALT_PARAM_OFF].setColor(getButtonGates(index));
		getDriver().sendCC(midiValue, value);
	}

	@Override
	public void toggleGateMute(final int buttonIndex) {
		getDevice(currentDeviceSlot).ifPresent(d -> d.toggleGateMute(buttonIndex));
	}

	@Override
	public void toggleVelMute(final int buttonIndex) {
		getDevice(currentDeviceSlot).ifPresent(d -> d.toggleVelMute(buttonIndex));
	}

	@Override
	public MuteState getGateMuteState(final int index) {
		return getDevice(currentDeviceSlot).map(d -> d.getGateMuteState(index)).orElse(MuteState.UNDEFINED);
	}

	@Override
	public MuteState getVelocityMuteState(final int index) {
		return getDevice(currentDeviceSlot).map(d -> d.getVelMuteState(index)).orElse(MuteState.UNDEFINED);
	}

	@Override
	public void handleSkipStepUpdate(final FocusDevice deviceSource, final int index, final int value) {
		final FocusDevice device = getTarget(currentDeviceSlot, deviceSource, index);
		if (device == null) {
			return;
		}
		getDriver().getEncoderButtons()[index].setColor(getButtonColorNotes(currentDeviceSlot, index));
	}

	@Override
	public void handleStepPosition(final FocusDevice deviceSource, final int position, final int prevPosition) {
		final FocusDevice device = getTarget(currentDeviceSlot, deviceSource);
		if (device == null) {
			return;
		}
		if (position != -1 && position < 16) {
			getDriver().getEncoderButtons()[position] //
					.setColor(getButtonColorNotes(position));
		}
		if (prevPosition != -1 && prevPosition < 16) {
			getDriver().getEncoderButtons()[prevPosition] //
					.setColor(getButtonColorNotes(prevPosition));
		}
	}

	@Override
	public void handleStepLength(final FocusDevice focusDevice, final int v, final int stepLength) {
		final FocusDevice device = getTarget(currentDeviceSlot, focusDevice);
		if (device == null) {
			return;
		}
		getDriver().getStepLengthValues().set(currentDeviceSlot, stepLength);
		refreshAllButtons();
	}

	@Override
	public void handleEncoderChange(final int index, final int offset) {
		final FocusDevice device = getDriver().getDevice(currentDeviceSlot);
		if (!device.isArp()) {
			return;
		}

		if (index < 16) {
			device.changeNoteValue(index, offset);
		} else {
			if (mode == Mode.GATE) {
				device.changeGateValue(index - 16, offset);
			} else {
				device.changeVelocityValue(index - 16, offset);
			}
		}
	}

	private void refreshAllButtons() {
		final RgbButton[] encoderButtons = getDriver().getEncoderButtons();
		for (int i = 0; i < 16; i++) {
			encoderButtons[i].setColor(getButtonColorNotes(i));
			if (mode == Mode.GATE) {
				encoderButtons[i + 16].setColor(getButtonGates(i));
			} else {
				encoderButtons[i + 16].setColor(getButtonVelocity(i));
			}
		}
	}

	@Override
	public void refresh() {
		final FocusDevice device = getDriver().getDevice(currentDeviceSlot);
		if (device.isArp()) {
			for (int i = 0; i < 16; i++) {
				handleNoteUpdate(device, i, device.getNoteValue(i));
				if (mode == Mode.GATE) {
					handleGateUpdate(device, i, device.getGateValue(i));
				} else {
					handleVelocityUpdate(device, i, device.getVelocityValue(i));
				}
			}
			refreshAllButtons();
		} else {
			for (int i = 0; i < 32; i++) {
				final RgbButton[] encoderButtons = getDriver().getEncoderButtons();
				encoderButtons[i].setColor(ColorButtonLedState.colorFor(0));
			}
			clearEncoderSection();
		}
	}

	@Override
	public int nrOfModes() {
		return 2;
	}

}
