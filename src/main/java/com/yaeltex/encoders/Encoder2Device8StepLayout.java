package com.yaeltex.encoders;

import com.bitwig.extension.controller.api.InternalHardwareLightState;
import com.yaeltex.ArpDisplayModeType;
import com.yaeltex.ColorButtonLedState;
import com.yaeltex.EncoderUtil;
import com.yaeltex.RgbButton;
import com.yaeltex.YaeltexArpControlExtension;
import com.yaeltex.device.FocusDevice;
import com.yaeltex.layer.MuteState;

public class Encoder2Device8StepLayout extends EncoderLayout {

	private enum Mode {
		VELOCITY, GATE;
	}

	public static final double[] gateMapping = new double[] { //
			0.0, 0.0625, 0.125, 0.1875, //
			0.25, 0.5, 0.75, 1.0 };

	public static final double[] velMapping = new double[] { //
			0.0, 0.25, 0.33, 0.5, //
			0.66, 0.75, 0.9, 1.0 };

	public static final int[] LAYOUT_INDEX_1 = new int[] { //
			0, 1, 2, 3, //
			8, 9, 10, 11, //
			16, 17, 18, 19, //
			24, 25, 26, 27 };
	public static final int[] LAYOUT_INDEX_2 = new int[] { //
			4, 5, 6, 7, //
			12, 13, 14, 15, //
			20, 21, 22, 23, //
			28, 29, 30, 31 };

	private static class LayoutEle {
		private final int device;
		private final int element;
		private final int index;

		public LayoutEle(final int device, final int element, final int index) {
			super();
			this.device = device;
			this.element = element;
			this.index = index;
		}
	}

	private static LayoutEle[] LAYOUT = new LayoutEle[] { //
			new LayoutEle(0, 0, 0), new LayoutEle(0, 0, 1), new LayoutEle(0, 0, 2), new LayoutEle(0, 0, 3), //
			new LayoutEle(1, 0, 0), new LayoutEle(1, 0, 1), new LayoutEle(1, 0, 2), new LayoutEle(1, 0, 3), //
			new LayoutEle(0, 0, 4), new LayoutEle(0, 0, 5), new LayoutEle(0, 0, 6), new LayoutEle(0, 0, 7), //
			new LayoutEle(1, 0, 4), new LayoutEle(1, 0, 5), new LayoutEle(1, 0, 6), new LayoutEle(1, 0, 7), //
			new LayoutEle(0, 1, 0), new LayoutEle(0, 1, 1), new LayoutEle(0, 1, 2), new LayoutEle(0, 1, 3), //
			new LayoutEle(1, 1, 0), new LayoutEle(1, 1, 1), new LayoutEle(1, 1, 2), new LayoutEle(1, 1, 3), //
			new LayoutEle(0, 1, 4), new LayoutEle(0, 1, 5), new LayoutEle(0, 1, 6), new LayoutEle(0, 1, 7), //
			new LayoutEle(1, 1, 4), new LayoutEle(1, 1, 5), new LayoutEle(1, 1, 6), new LayoutEle(1, 1, 7) //
	};

	private Mode mode = Mode.VELOCITY;

	public Encoder2Device8StepLayout(final YaeltexArpControlExtension driver) {
		super(driver, ArpDisplayModeType.MODE_2X8);
		final RgbButton[] encoderButtons = driver.getEncoderButtons();
		for (int i = 0; i < 16; i++) {
			final LayoutEle ele = LAYOUT[i];
			final RgbButton stepSkipButton = encoderButtons[i];
			bindPressed(stepSkipButton, () -> {
				toggleStepSkip(driver.getDevice(ele.device), ele.index);
			});
			bindLightState(() -> getButtonColorNotes(ele.device, ele.index), stepSkipButton);
		}
		for (int i = 16; i < 32; i++) {
			final LayoutEle ele = LAYOUT[i];
			final RgbButton setVelButton = encoderButtons[i];
			bindPressed(setVelButton, () -> {
				toggleBottomRow(driver.getDevice(ele.device), ele.index);
			});
			bindLightState(() -> getButtonVelocity(ele.device, ele.index), setVelButton);
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
	public int nrOfModes() {
		return 2;
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
		final FocusDevice device1 = getTarget(0, deviceSource, index);
		if (device1 != null) {
			final int mindex = LAYOUT_INDEX_1[index];
			final int outvalue = EncoderUtil.NOTE_ENCODER_MAPPING[value];
			final int midiValue = EncoderUtil.ENCODER_INDEX[mindex];
			device1.updateGlobals(0);
			getDriver().getEncoderButtons()[mindex].setColor(getButtonColorNotes(0, index));
			getDriver().sendCC(midiValue, outvalue);
		}
		final FocusDevice device2 = getTarget(1, deviceSource, index);
		if (device2 != null) {
			device2.updateGlobals(1);
			final int mindex = LAYOUT_INDEX_2[index];
			final int outvalue = EncoderUtil.NOTE_ENCODER_MAPPING[value];
			final int midiValue = EncoderUtil.ENCODER_INDEX[mindex];
			getDriver().getEncoderButtons()[mindex].setColor(getButtonColorNotes(1, index));
			getDriver().sendCC(midiValue, outvalue);
		}
	}

	@Override
	public void handleNoteOffsetUpdate(final FocusDevice deviceSource, final int index, final int value) {
	}

	private void toggleBottomRow(final FocusDevice device, final int index) {
		if (mode == Mode.GATE) {
			device.toggleGateMute(index);
		} else {
			device.toggleVelMute(index);
		}
	}

	@Override
	public void handleGateUpdate(final FocusDevice deviceSource, final int index, final int value) {
		if (mode != Mode.GATE) {
			return;
		}
		final FocusDevice device1 = getTarget(0, deviceSource, index);
		if (device1 != null) {
			final int mindex = LAYOUT_INDEX_1[index + 8];
			final int midiValue = EncoderUtil.ENCODER_INDEX[mindex];
			getDriver().getEncoderButtons()[mindex].setColor(getButtonGates(0, index));
			getDriver().sendCC(midiValue, value);
		}
		final FocusDevice device2 = getTarget(1, deviceSource, index);
		if (device2 != null) {
			final int mindex = LAYOUT_INDEX_2[index + 8];
			final int midiValue = EncoderUtil.ENCODER_INDEX[mindex];
			getDriver().getEncoderButtons()[mindex].setColor(getButtonGates(1, index));
			getDriver().sendCC(midiValue, value);
		}

	}

	@Override
	public void handleVelocityUpdate(final FocusDevice deviceSource, final int index, final int value) {
		if (mode != Mode.VELOCITY) {
			return;
		}
		final FocusDevice device1 = getTarget(0, deviceSource, index);
		if (device1 != null) {
			final int mindex = LAYOUT_INDEX_1[index + 8];
			final int midiValue = EncoderUtil.ENCODER_INDEX[mindex];
			getDriver().getEncoderButtons()[mindex].setColor(getButtonVelocity(0, index));
			getDriver().sendCC(midiValue, value);
		}
		final FocusDevice device2 = getTarget(1, deviceSource, index);
		if (device2 != null) {
			final int mindex = LAYOUT_INDEX_2[index + 8];
			final int midiValue = EncoderUtil.ENCODER_INDEX[mindex];
			getDriver().getEncoderButtons()[mindex].setColor(getButtonVelocity(1, index));
			getDriver().sendCC(midiValue, value);
		}
	}

	@Override
	public void toggleGateMute(final int buttonIndex) {
		if (buttonIndex < 8) {
			getDevice(0).ifPresent(d -> d.toggleGateMute(buttonIndex));
		} else {
			getDevice(1).ifPresent(d -> d.toggleGateMute(buttonIndex - 8));
		}
	}

	@Override
	public void toggleVelMute(final int buttonIndex) {
		if (buttonIndex < 8) {
			getDevice(0).ifPresent(d -> d.toggleVelMute(buttonIndex));
		} else {
			getDevice(1).ifPresent(d -> d.toggleVelMute(buttonIndex - 8));
		}
	}

	@Override
	public void setStepLength(final int buttonIndex) {
		if (buttonIndex < 8) {
			getDevice(0).ifPresent(d -> d.setStepLength(buttonIndex));
		} else {
			getDevice(1).ifPresent(d -> d.setStepLength(buttonIndex - 8));
		}
	}

	@Override
	public boolean isStepValue(final int buttonIndex) {
		if (buttonIndex < 8) {
			return getDevice(0).map(d -> buttonIndex < d.getStepLength() + 1).orElse(false);
		}
		return getDevice(1).map(d -> buttonIndex - 8 < d.getStepLength() + 1).orElse(false);
	}

	@Override
	public void setGlobalGateLength(final int buttonIndex) {
		if (buttonIndex < 8) {
			getDevice(0).ifPresent(d -> {
				d.setGlobalGate(gateMapping[buttonIndex]);
			});
		} else {
			getDevice(1).ifPresent(d -> {
				d.setGlobalGate(gateMapping[buttonIndex - 8]);
			});
		}
	}

	@Override
	public boolean isGlobalGateValue(final int buttonIndex) {
		if (buttonIndex < 8) {
			return getDevice(0).map(d -> {
				final int v = EncoderUtil.indexFromMapping(gateMapping, d.getGlobalGateLength());
				return buttonIndex < v + 1;
			}).orElse(false);
		}
		return getDevice(1).map(d -> {
			final int v = EncoderUtil.indexFromMapping(gateMapping, d.getGlobalGateLength());
			return buttonIndex - 8 < v + 1;
		}).orElse(false);
	}

	@Override
	public void setGlobalVelocityLength(final int buttonIndex) {
		if (buttonIndex < 8) {
			getDevice(0).ifPresent(d -> {
				d.setGlobalVelocity(buttonIndex / 7.0);
			});
		} else {
			getDevice(1).ifPresent(d -> {
				d.setGlobalVelocity((buttonIndex - 8) / 7.0);
			});
		}
	}

	@Override
	public boolean isGlobalVelocityValue(final int buttonIndex) {
		if (buttonIndex < 8) {
			return getDevice(0).map(d -> {
				final int v = (int) (d.getGlobalVelocityLength() * 7.0);
				return buttonIndex < v + 1;
			}).orElse(false);
		}
		return getDevice(1).map(d -> {
			final int v = (int) (d.getGlobalVelocityLength() * 7.0);
			return buttonIndex - 8 < v + 1;
		}).orElse(false);
	}

	@Override
	public MuteState getGateMuteState(final int buttonIndex) {
		if (buttonIndex < 8) {
			return getDevice(0).map(d -> d.getGateMuteState(buttonIndex)).orElse(MuteState.UNDEFINED);
		}
		return getDevice(1).map(d -> d.getGateMuteState(buttonIndex - 8)).orElse(MuteState.UNDEFINED);
	}

	@Override
	public MuteState getVelocityMuteState(final int buttonIndex) {
		if (buttonIndex < 8) {
			return getDevice(0).map(d -> d.getVelMuteState(buttonIndex)).orElse(MuteState.UNDEFINED);
		}
		return getDevice(1).map(d -> d.getVelMuteState(buttonIndex - 8)).orElse(MuteState.UNDEFINED);
	}

	@Override
	public void handleSkipStepUpdate(final FocusDevice deviceSource, final int index, final int value) {
		final FocusDevice device1 = getTarget(0, deviceSource, index);
		if (device1 != null) {
			final int mindex = LAYOUT_INDEX_1[index];
			getDriver().getEncoderButtons()[mindex].setColor(getButtonColorNotes(0, index));
		}
		final FocusDevice device2 = getTarget(1, deviceSource, index);
		if (device2 != null) {
			final int mindex = LAYOUT_INDEX_2[index];
			getDriver().getEncoderButtons()[mindex].setColor(getButtonColorNotes(1, index));
		}
	}

	@Override
	public void handleStepPosition(final FocusDevice deviceSource, final int position, final int prevPosition) {
		applyStepPosition(getTarget(0, deviceSource), 0, position, prevPosition);
		applyStepPosition(getTarget(1, deviceSource), 1, position, prevPosition);
	}

	private void applyStepPosition(final FocusDevice device, final int deviceIndex, final int position,
			final int prevPosition) {
		if (device == null) {
			return;
		}
		final int[] map = deviceIndex == 0 ? LAYOUT_INDEX_1 : LAYOUT_INDEX_2;
		if (position != -1 && position < 8) {
			getDriver().getEncoderButtons()[map[position]] //
					.setColor(getButtonColorNotes(deviceIndex, position));
		}
		if (prevPosition != -1 && prevPosition < 8) {
			getDriver().getEncoderButtons()[map[prevPosition]] //
					.setColor(getButtonColorNotes(deviceIndex, prevPosition));
		}
	}

	@Override
	public void handleStepLength(final FocusDevice deviceSource, final int v, final int stepLength) {
		final FocusDevice device1 = getTarget(0, deviceSource);
		if (null != device1) {
			refreshLeds(0);
			getDriver().getStepLengthValues().set(0, stepLength);
		}
		final FocusDevice device2 = getTarget(1, deviceSource);
		if (null != device2) {
			refreshLeds(1);
			getDriver().getStepLengthValues().set(1, stepLength);
		}
	}

	@Override
	public void handleEncoderChange(final int index, final int offset) {
		final LayoutEle ele = LAYOUT[index];
		final FocusDevice device = getDriver().getDevice(ele.device);
		if (device.isArp()) {
			if (ele.element == 0) {
				device.changeNoteValue(ele.index, offset);
			} else {
				if (mode == Mode.VELOCITY) {
					device.changeVelocityValue(ele.index, offset);
				} else {
					device.changeGateValue(ele.index, offset);
				}
			}
		}
	}

	protected void clearSection(final int device) {
		final int[] map = device == 0 ? LAYOUT_INDEX_1 : LAYOUT_INDEX_2;
		final ColorButtonLedState test = device == 0 ? ColorButtonLedState.AQUA : ColorButtonLedState.RED;
		for (int i = 0; i < 16; i++) {
			final int encoderAssignment = EncoderUtil.ENCODER_INDEX[map[i]];
			getDriver().sendCC(encoderAssignment, 0);
		}
		final RgbButton[] encoderButtons = getDriver().getEncoderButtons();
		for (int i = 0; i < 16; i++) {
			encoderButtons[map[i]].setColor(test);
		}
	}

	private void refreshLeds(final int deviceIndex) {
		final int[] map = deviceIndex == 0 ? LAYOUT_INDEX_1 : LAYOUT_INDEX_2;
		final RgbButton[] encoderButtons = getDriver().getEncoderButtons();
		final FocusDevice device = getDriver().getDevice(deviceIndex);
		for (int i = 0; i < 8; i++) {
			encoderButtons[map[i]].setColor(getButtonColorNotes(deviceIndex, i));
			handleNoteUpdate(device, i, device.getNoteValue(i));
			if (mode == Mode.VELOCITY) {
				encoderButtons[map[i + 8]].setColor(getButtonVelocity(deviceIndex, i));
				handleVelocityUpdate(device, i, device.getVelocityValue(i));
			} else {
				encoderButtons[map[i + 8]].setColor(getButtonGates(deviceIndex, i));
				handleGateUpdate(device, i, device.getGateValue(i));
			}
		}
	}

	private void updateNoteSection(final int deviceIndex) {
		final int[] map = deviceIndex == 0 ? LAYOUT_INDEX_1 : LAYOUT_INDEX_2;
		final FocusDevice device = getDriver().getDevice(deviceIndex);
		for (int i = 0; i < 8; i++) {
			final int mindex = map[i];
			final int value = device.getNoteValue(i);
			final int outvalue = EncoderUtil.NOTE_ENCODER_MAPPING[value];
			final int midiValue = EncoderUtil.ENCODER_INDEX[mindex];
			getDriver().sendCC(midiValue, outvalue);
		}
	}

	public void updateVelGateSection(final int deviceIndex) {
		final int[] map = deviceIndex == 0 ? LAYOUT_INDEX_1 : LAYOUT_INDEX_2;
		final FocusDevice device = getDriver().getDevice(deviceIndex);
		for (int i = 0; i < 8; i++) {
			final int mindex = map[i + 8];
			final int value = mode == Mode.VELOCITY ? device.getVelocityValue(i) : device.getGateValue(i);
			final int midiValue = EncoderUtil.ENCODER_INDEX[mindex];
			getDriver().sendCC(midiValue, value);
		}
	}

	@Override
	public void refresh() {
		final FocusDevice device1 = getDriver().getDevice(0);
		if (device1.isArp()) {
			refreshLeds(0);
			updateNoteSection(0);
			updateVelGateSection(0);
		} else {
			clearSection(0);
		}
		final FocusDevice device2 = getDriver().getDevice(1);
		if (device2.isArp()) {
			refreshLeds(1);
			updateNoteSection(1);
			updateVelGateSection(1);
		} else {
			clearSection(1);
		}
	}

}
