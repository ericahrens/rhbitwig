package com.yaeltex.layer;

import com.yaeltex.RgbButton;
import com.yaeltex.YaeltexArpControlExtension;

public class StepVelMuteButtonLayer extends BasicButtonLayer {

	public StepVelMuteButtonLayer(final YaeltexArpControlExtension driver) {
		super(driver, "STEP_MUTE_BUTTON_LAYER");

		final RgbButton[] topButton = driver.getTopRowButtons();
		for (int i = 0; i < topButton.length; i++) {
			final int index = i;
			final RgbButton button = topButton[i];
			bindLightState(() -> getVelocityColor(index), button);
			bindPressed(button, () -> toggleVelocityMute(index));
		}

		final RgbButton[] bottomButton = driver.getBottomRowButtons();
		for (int i = 0; i < bottomButton.length; i++) {
			final int index = i;
			final RgbButton button = bottomButton[i];
			bindLightState(() -> getGateMuteColor(index), button);
			bindPressed(button, () -> toggleGateMute(index));
		}
	}

	@Override
	protected void encoderLayoutChanged() {

	}

}
