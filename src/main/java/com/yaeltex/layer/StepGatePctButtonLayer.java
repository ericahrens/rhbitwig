package com.yaeltex.layer;

import com.yaeltex.ColorButtonLedState;
import com.yaeltex.RgbButton;
import com.yaeltex.YaeltexArpControlExtension;

public class StepGatePctButtonLayer extends BasicButtonLayer {

	public StepGatePctButtonLayer(final YaeltexArpControlExtension driver) {
		super(driver, "STEP_GATEPCT_BUTTON_LAYER");

		initTopStepButtons(driver);

		final RgbButton[] bottomButton = driver.getBottomRowButtons();
		for (int i = 0; i < bottomButton.length; i++) {
			final int index = i;
			final RgbButton button = bottomButton[i];
			bindLightState(() -> getGlobalGateColor(index), button);
			bindPressed(button, () -> setGlobalGateLength(index));
		}
	}

	@Override
	protected void encoderLayoutChanged() {

	}

	private void setGlobalGateLength(final int index) {
		currentEncoderLayout.setGlobalGateLength(index);
	}

	private ColorButtonLedState getGlobalGateColor(final int index) {
		if (currentEncoderLayout.isGlobalGateValue(index)) {
			return ColorButtonLedState.ORANGE_DIM;
		}
		return ColorButtonLedState.OFF;
	}

}
