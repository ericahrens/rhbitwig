package com.yaeltex.encoders;

import java.util.Optional;

import com.bitwig.extension.controller.api.InternalHardwareLightState;
import com.yaeltex.ArpDisplayModeType;
import com.yaeltex.ColorButtonLedState;
import com.yaeltex.EncoderUtil;
import com.yaeltex.RgbButton;
import com.yaeltex.YaelTexColors;
import com.yaeltex.YaeltexArpControlExtension;
import com.yaeltex.device.FocusDevice;
import com.yaeltex.layer.MuteState;
import com.yaeltex.layer.YaeltexLayer;

public abstract class EncoderLayout extends YaeltexLayer {

	private final ArpDisplayModeType type;

	protected int currentDeviceSlot = 0;

	public EncoderLayout(final YaeltexArpControlExtension driver, final ArpDisplayModeType type) {
		super(driver, "ARP_ENCODER_LAYOUT_" + type.toString());
		this.type = type;
		final RgbButton modeButton = getModeButton();

		driver.getFocusedDeviceSlot().addValueObserver(v -> {
			currentDeviceSlot = v;
		});
		bindPressed(modeButton, this::nextEncMode);
		bindLightState(this::getEncModeLight, modeButton.getLight());
	}

	public abstract void handleNoteUpdate(FocusDevice deviceSource, int index, int value);

	public abstract void handleNoteOffsetUpdate(FocusDevice deviceSource, int index, int value);

	public abstract void handleGateUpdate(FocusDevice deviceSource, int index, int value);

	public abstract void handleVelocityUpdate(FocusDevice deviceSource, int index, int value);

	public abstract void handleSkipStepUpdate(FocusDevice deviceSource, int index, int value);

	public abstract void handleStepPosition(FocusDevice deviceSource, int position, int prevPosition);

	public abstract void nextEncMode();

	public abstract InternalHardwareLightState getEncModeLight();

	public abstract void toggleGateMute(int buttonIndex);

	public abstract void toggleVelMute(int buttonIndex);

	public abstract MuteState getGateMuteState(int index);

	public abstract MuteState getVelocityMuteState(int index);

	public abstract int nrOfModes();

	public void setGlobalVelocityLength(final int buttonIndex) {
		getDevice(currentDeviceSlot).ifPresent(d -> {
			d.setGlobalVelocity(buttonIndex / 15.0);
		});
	}

	public boolean isGlobalVelocityValue(final int buttonIndex) {
		return getDevice(currentDeviceSlot).map(d -> {
			final int v = (int) (d.getGlobalVelocityLength() * 16.0);
			return buttonIndex < v + 1;
		}).orElse(false);
	}

	public void setGlobalGateLength(final int buttonIndex) {
		getDevice(currentDeviceSlot).ifPresent(d -> {
			if (buttonIndex == 15) {
				d.setGlobalGate(1.0);
			} else {
				d.setGlobalGate(buttonIndex / 16.0);
			}
		});
	}

	public boolean isGlobalGateValue(final int buttonIndex) {
		return getDevice(currentDeviceSlot).map(d -> {
			final int v = (int) (d.getGlobalGateLength() * 16.0);
			return buttonIndex < v + 1;
		}).orElse(false);
	}

	public void setStepLength(final int buttonIndex) {
		getDevice(currentDeviceSlot).ifPresent(d -> d.setStepLength(buttonIndex));
	}

	public boolean isStepValue(final int buttonIndex) {
		return getDevice(currentDeviceSlot).map(d -> buttonIndex < d.getStepLength() + 1).orElse(false);
	}

	public ArpDisplayModeType getType() {
		return type;
	}

	protected RgbButton getModeButton() {
		return getDriver().getModeButtons(RgbButton.ENC_MODE);
	}

	protected void toggleStepSkip(final FocusDevice device, final int index) {
		if (index >= type.getMaxSteps() || !device.isArp()) {
			return;
		}
		device.toggleStepSkipValue(index);
	}

	protected void toggleMute(final FocusDevice device, final int index) {
		if (index >= type.getMaxSteps() || !device.isArp()) {
			return;
		}
		device.toggleGateMute(index);
	}

	protected void toggleVelMute(final FocusDevice device, final int index) {
		if (index >= type.getMaxSteps() || !device.isArp()) {
			return;
		}
		device.toggleVelMute(index);
	}

	protected void resetNoteOffset(final FocusDevice device, final int index) {
		if (!device.isArp()) {
			return;
		}
		device.resetNoteOffset(index);
	}

	protected FocusDevice getTarget(final int deviceIndex, final FocusDevice source, final int index) {
		if (index >= type.getMaxSteps()) {
			return null;
		}
		final FocusDevice device = getDriver().getDevice(deviceIndex);
		if (device.getWhich() != source.getWhich() || !device.isArp()) {
			return null;
		}
		return device;
	}

	protected FocusDevice getTarget(final int deviceIndex, final FocusDevice source) {
		final FocusDevice device = getDriver().getDevice(deviceIndex);
		if (device.getWhich() != source.getWhich() || !device.isArp()) {
			return null;
		}
		return device;
	}

	public abstract void refresh();

	public abstract void handleEncoderChange(int index, int offset);

	public abstract void handleStepLength(final FocusDevice focusDevice, final int v, final int stepLength);

	protected ColorButtonLedState getButtonGates(final int index) {
		return getButtonGates(currentDeviceSlot, index);
	}

	protected ColorButtonLedState getButtonGates(final int deviceIndex, final int index) {
		final FocusDevice device = getDriver().getDevice(deviceIndex);
		if (device.isArp()) {
			// final ArpInstance assignedArpInstance = device.getAssignedArpInstance();
			if (index > device.getStepLength()) {
				return ColorButtonLedState.OFF;
			}
			if (device.getGateValue(index) == 0) {
				return ColorButtonLedState.WHITE;
			}
			return ColorButtonLedState.YELLOW;
		}
		return ColorButtonLedState.OFF;
	}

	protected ColorButtonLedState getButtonColorNotes(final int index) {
		return getButtonColorNotes(currentDeviceSlot, index);
	}

	protected ColorButtonLedState getButtonColorNotes(final int deviceIndex, final int index) {
		final FocusDevice device = getDriver().getDevice(deviceIndex);
		if (device.isArp()) {
			if (index > device.getStepLength()) {
				return ColorButtonLedState.OFF;
			}
			if (device.getStepPostion() == index) {
				return ColorButtonLedState.GREEN;
			}
			if (device.getStepSkipValue(index) == 1) {
				return ColorButtonLedState.RED;
			}
			return ColorButtonLedState.ORANGE_DIM;
		}
		return ColorButtonLedState.OFF;
	}

	protected ColorButtonLedState getButtonColorNotesOffset(final int index) {
		return getButtonColorNotesOffset(currentDeviceSlot, index);
	}

	protected ColorButtonLedState getButtonColorNotesOffset(final int deviceIndex, final int index) {
		final FocusDevice device = getDriver().getDevice(deviceIndex);
		if (device.isArp()) {
			if (index > device.getStepLength()) {
				return ColorButtonLedState.OFF;
			}
			return ColorButtonLedState.colorFor(25);
		}
		return ColorButtonLedState.OFF;
	}

	protected ColorButtonLedState getButtonVelocity(final int index) {
		return getButtonVelocity(currentDeviceSlot, index);
	}

	protected ColorButtonLedState getButtonVelocity(final int deviceIndex, final int index) {
		final FocusDevice device = getDriver().getDevice(deviceIndex);
		if (device.isArp()) {
			if (index > device.getStepLength()) {
				return ColorButtonLedState.OFF;
			}
			if (device.getVelocityValue(index) == 0) {
				return ColorButtonLedState.colorFor(YaelTexColors.AQUA, 0);
			}
			return ColorButtonLedState.colorFor(YaelTexColors.DARK_BLUE, 0);
		}
		return ColorButtonLedState.OFF;
	}

	protected void clearEncoderSection() {
		for (int i = 0; i < 32; i++) {
			final int encoderAssignment = EncoderUtil.ENCODER_INDEX[i];
			getDriver().sendCC(encoderAssignment, 0);
		}
	}

	protected Optional<FocusDevice> getDevice(final int deviceIndex) {
		final FocusDevice device = getDriver().getDevice(deviceIndex);
		if (device != null && device.isArp()) {
			return Optional.of(device);
		}
		return Optional.empty();
	}

	protected Optional<FocusDevice> getFocussedDevice() {
		final FocusDevice device = getDriver().getFocussedDevice();
		if (device != null && device.isArp()) {
			return Optional.of(device);
		}
		return Optional.empty();
	}

	@Override
	protected void onActivate() {
		super.onActivate();
		this.refresh();
	}

	@Override
	protected void onDeactivate() {
	}

}
