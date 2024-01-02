package com.yaeltex.seqarp168.encoders;

import com.bitwig.extension.controller.api.InternalHardwareLightState;
import com.yaeltex.seqarp168.ArpDisplayModeType;
import com.yaeltex.seqarp168.ColorButtonLedState;
import com.yaeltex.seqarp168.EncoderUtil;
import com.yaeltex.seqarp168.RgbButton;
import com.yaeltex.seqarp168.YaeltexArpControlExtension;
import com.yaeltex.seqarp168.device.FocusDevice;
import com.yaeltex.seqarp168.layer.MuteState;

public class Encoder8StepLayout extends EncoderLayout {

	private final int NOTE_OFFSET = 8;
	private final int VEL_OFFSET = 16;
	private final int GATE_OFFSET = 24;

	public Encoder8StepLayout(final YaeltexArpControlExtension driver) {
		super(driver, ArpDisplayModeType.MODE_1X8);
		final RgbButton[] encoderButtons = driver.getEncoderButtons();

		for (int i = 0; i < 8; i++) {
			final int index = i;
			final RgbButton stepSkipButton = encoderButtons[index];
			bindPressed(stepSkipButton, () -> getFocussedDevice().ifPresent(d -> toggleStepSkip(d, index)));
			bindLightState(() -> getButtonColorNotes(index), stepSkipButton);
		}
		for (int i = NOTE_OFFSET; i < 16; i++) {
			final int index = i;
			final RgbButton stepSkipButton = encoderButtons[index];
			bindPressed(stepSkipButton,
					() -> getFocussedDevice().ifPresent(d -> resetNoteOffset(d, index - NOTE_OFFSET)));
			bindLightState(() -> getButtonColorNotesOffset(index - NOTE_OFFSET), stepSkipButton);
		}
		for (int i = VEL_OFFSET; i < 24; i++) {
			final int index = i;
			final RgbButton stepSkipButton = encoderButtons[index];
			bindPressed(stepSkipButton, () -> getFocussedDevice().ifPresent(d -> toggleVelMute(d, index - VEL_OFFSET)));
			bindLightState(() -> getButtonVelocity(index - VEL_OFFSET), stepSkipButton);
		}
		for (int i = GATE_OFFSET; i < 32; i++) {
			final int index = i;
			final RgbButton stepSkipButton = encoderButtons[index];
			bindPressed(stepSkipButton, () -> getFocussedDevice().ifPresent(d -> toggleMute(d, index - GATE_OFFSET)));
			bindLightState(() -> getButtonGates(index - GATE_OFFSET), stepSkipButton);
		}
	}

	@Override
	public void nextEncMode() {
	}

	@Override
	public int nrOfModes() {
		return 1;
	}

	@Override
	public InternalHardwareLightState getEncModeLight() {
		return ColorButtonLedState.WHITE;
	}

	@Override
	public void toggleGateMute(final int buttonIndex) {
		getFocussedDevice().ifPresent(d -> d.toggleGateMute(buttonIndex));
	}

	@Override
	public void toggleVelMute(final int buttonIndex) {
		getFocussedDevice().ifPresent(d -> d.toggleVelMute(buttonIndex));
	}

	@Override
	public MuteState getGateMuteState(final int index) {
		return getFocussedDevice().map(d -> d.getGateMuteState(index)).orElse(MuteState.UNDEFINED);
	}

	@Override
	public MuteState getVelocityMuteState(final int index) {
		return getFocussedDevice().map(d -> d.getVelMuteState(index)).orElse(MuteState.UNDEFINED);
	}

	@Override
	public void handleNoteUpdate(final FocusDevice deviceSource, final int index, final int value) {
		final FocusDevice device = getTarget(currentDeviceSlot, deviceSource, index);
		if (device == null) {
			return;
		}
		final int outvalue = EncoderUtil.NOTE_ENCODER_MAPPING[value];
		final int midiValue = EncoderUtil.ENCODER_INDEX[index];
		getDriver().getEncoderButtons()[index].setColor(getButtonColorNotes(index));
		getDriver().sendCC(midiValue, outvalue);
	}

	@Override
	public void handleNoteOffsetUpdate(final FocusDevice deviceSource, final int index, final int value) {
		final FocusDevice device = getTarget(currentDeviceSlot, deviceSource, index);
		if (device == null) {
			return;
		}
		final int midiValue = EncoderUtil.ENCODER_INDEX[index + NOTE_OFFSET];
		final int outvalue = EncoderUtil.toOffsetCc(value);
		getDriver().sendCC(midiValue, outvalue);
	}

	@Override
	public void handleVelocityUpdate(final FocusDevice deviceSource, final int index, final int value) {
		final FocusDevice device = getTarget(currentDeviceSlot, deviceSource, index);
		if (device == null) {
			return;
		}

		final int midiValue = EncoderUtil.ENCODER_INDEX[index + VEL_OFFSET];
		getDriver().getEncoderButtons()[index + VEL_OFFSET].setColor(getButtonVelocity(index));
		getDriver().sendCC(midiValue, value);
	}

	@Override
	public void handleGateUpdate(final FocusDevice deviceSource, final int index, final int value) {
		final FocusDevice device = getTarget(currentDeviceSlot, deviceSource, index);
		if (device == null) {
			return;
		}

		final int midiValue = EncoderUtil.ENCODER_INDEX[index + GATE_OFFSET];
		getDriver().getEncoderButtons()[index + GATE_OFFSET].setColor(getButtonGates(index));
		getDriver().sendCC(midiValue, value);
	}

	@Override
	public void handleSkipStepUpdate(final FocusDevice deviceSource, final int index, final int value) {
		final FocusDevice device = getTarget(currentDeviceSlot, deviceSource, index);
		if (device == null) {
			return;
		}
		getDriver().getEncoderButtons()[index].setColor(getButtonColorNotes(index));
	}

	@Override
	public void handleStepPosition(final FocusDevice deviceSource, final int position, final int prevPosition) {
		final FocusDevice device = getTarget(currentDeviceSlot, deviceSource);
		if (device == null) {
			return;
		}
		if (position != -1 && position < 8) {
			getDriver().getEncoderButtons()[position] //
					.setColor(getButtonColorNotes(position));
		}
		if (prevPosition != -1 && prevPosition < 8) {
			getDriver().getEncoderButtons()[prevPosition] //
					.setColor(getButtonColorNotes(currentDeviceSlot, prevPosition));
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
		final FocusDevice device = getDriver().getFocussedDevice();
		if (!device.isArp()) {
			return;
		}
		if (index < 8) {
			device.changeNoteValue(index, offset);
		} else if (index < 16) {
			device.changeNoteOffsetValue(index - 8, offset);
		} else if (index < 24) {
			device.changeVelocityValue(index - 16, offset);
		} else {
			device.changeGateValue(index - 24, offset);
		}
	}

	private void refreshAllButtons() {
		final RgbButton[] encoderButtons = getDriver().getEncoderButtons();
		for (int i = 0; i < 8; i++) {
			encoderButtons[i].setColor(getButtonColorNotes(i));
			encoderButtons[i + NOTE_OFFSET].setColor(getButtonColorNotesOffset(i));
			encoderButtons[i + VEL_OFFSET].setColor(getButtonVelocity(i));
			encoderButtons[i + GATE_OFFSET].setColor(getButtonGates(i));
		}
	}

	@Override
	public void refresh() {
		final FocusDevice device = getDriver().getFocussedDevice();

		if (device.isArp()) {
			device.updateGlobals(0);
			for (int i = 0; i < 8; i++) {
				handleNoteUpdate(device, i, device.getNoteValue(i));
				handleNoteOffsetUpdate(device, i, device.getNoteOffsetVal(i));
				handleGateUpdate(device, i, device.getGateValue(i));
				handleVelocityUpdate(device, i, device.getVelocityValue(i));
				handleSkipStepUpdate(device, i, device.getStepSkipValue(i));
			}
			refreshAllButtons();
		} else {
			final RgbButton[] encoderButtons = getDriver().getEncoderButtons();
			for (int i = 0; i < 32; i++) {
				encoderButtons[i].setColor(ColorButtonLedState.colorFor(0));
			}
			clearEncoderSection();
		}
	}

}
