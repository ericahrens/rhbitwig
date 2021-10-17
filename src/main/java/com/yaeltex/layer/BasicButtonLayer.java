package com.yaeltex.layer;

import com.yaeltex.ColorButtonLedState;
import com.yaeltex.RgbButton;
import com.yaeltex.YaeltexArpControlExtension;
import com.yaeltex.encoders.EncoderLayout;

public abstract class BasicButtonLayer extends YaeltexLayer {

	protected EncoderLayout currentEncoderLayout;

	public BasicButtonLayer(final YaeltexArpControlExtension driver, final String name) {
		super(driver, name);
		currentEncoderLayout = driver.getCurrentEncoderLayout().get();
		driver.getCurrentEncoderLayout().addValueObserver(layout -> {
			this.currentEncoderLayout = layout;
			encoderLayoutChanged();
		});
	}

	protected abstract void encoderLayoutChanged();

	public void initTopStepButtons(final YaeltexArpControlExtension driver) {
		final RgbButton[] topButton = driver.getTopRowButtons();
		for (int i = 0; i < topButton.length; i++) {
			final int index = i;
			final RgbButton button = topButton[i];
			bindLightState(() -> getStepColor(index), button);
			bindPressed(button, () -> setStepLength(index));
		}
	}

	protected void setStepLength(final int index) {
		currentEncoderLayout.setStepLength(index);
	}

	protected ColorButtonLedState getStepColor(final int index) {
		if (currentEncoderLayout.isStepValue(index)) {
			return ColorButtonLedState.GREEN;
		}
		return ColorButtonLedState.OFF;
	}

	protected void toggleGateMute(final int index) {
		currentEncoderLayout.toggleGateMute(index);
	}

	protected ColorButtonLedState getGateMuteColor(final int index) {
		final MuteState state = currentEncoderLayout.getGateMuteState(index);
		if (state == MuteState.ACTIVE) {
			return ColorButtonLedState.YELLOW;
		} else if (state == MuteState.MUTED) {
			return ColorButtonLedState.WHITE;
		}
		return ColorButtonLedState.OFF;
	}

	protected void toggleVelocityMute(final int index) {
		currentEncoderLayout.toggleVelMute(index);
	}

	protected ColorButtonLedState getVelocityColor(final int index) {
		final MuteState state = currentEncoderLayout.getVelocityMuteState(index);
		if (state == MuteState.ACTIVE) {
			return ColorButtonLedState.BLUE;
		} else if (state == MuteState.MUTED) {
			return ColorButtonLedState.WHITE;
		}
		return ColorButtonLedState.OFF;
	}

}
