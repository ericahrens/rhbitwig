package com.yaeltex.seqarp168.layer;

import com.yaeltex.seqarp168.RgbButton;
import com.yaeltex.seqarp168.YaeltexArpControlExtension;

public class StepMuteButtonLayer extends BasicButtonLayer {

	public StepMuteButtonLayer(final YaeltexArpControlExtension driver) {
		super(driver, "STEP_VEL_MUTE_BUTTON_LAYER");

		initTopStepButtons(driver);

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
