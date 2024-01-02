package com.yaeltex.seqarp168.layer;

import com.yaeltex.seqarp168.ColorButtonLedState;
import com.yaeltex.seqarp168.RgbButton;
import com.yaeltex.seqarp168.YaeltexArpControlExtension;

public class StepVelocityPctButtonLayer extends BasicButtonLayer {

	public StepVelocityPctButtonLayer(final YaeltexArpControlExtension driver) {
		super(driver, "STEP_VELPCT_BUTTON_LAYER");

		initTopStepButtons(driver);

		final RgbButton[] bottomButton = driver.getBottomRowButtons();
		for (int i = 0; i < bottomButton.length; i++) {
			final int index = i;
			final RgbButton button = bottomButton[i];
			bindLightState(() -> getColor(index), button);
			bindPressed(button, () -> setValue(index));
		}
	}

	@Override
	protected void encoderLayoutChanged() {

	}

	private void setValue(final int index) {
		currentEncoderLayout.setGlobalVelocityLength(index);
	}

	private ColorButtonLedState getColor(final int index) {
		if (currentEncoderLayout.isGlobalVelocityValue(index)) {
			return ColorButtonLedState.AQUA;
		}
		return ColorButtonLedState.OFF;
	}

}
