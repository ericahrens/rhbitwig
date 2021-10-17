package com.novation.launchcontrol.arp;

import com.bitwig.extension.controller.api.Parameter;

public class GlobalVelocityButtonMode extends ButtonModeLayer {

	public GlobalVelocityButtonMode(final LpcArpControlExtension driver, final String name) {
		super(driver, name);
		final RedGreenButton[] focusButtons = driver.getFocusButtons();
		final RedGreenButton[] controlButtons = driver.getControlButtons();

		assignStepControl(focusButtons, driver.getArpStepsParam(), driver.getArpStepPositionParam());

		final Parameter globalVelocity = driver.getArpdevice().createParameter("GLOBAL_VEL");
		globalVelocity.markInterested();

		final double[] values = { 0, 0.1, 0.25, 0.40, 0.55, 0.75, 0.9, 1.0 };
		for (int i = 0; i < values.length; i++) {
			final double lower = i == 0 ? 0 : values[i];
			final double upper = i == values.length - 1 ? values[i] : values[i + 1];
			bindButtonToValue(controlButtons[i], globalVelocity, values[i], lower, upper);
		}
	}

	private void bindButtonToValue(final RedGreenButton button, final Parameter globalGate, final double value,
			final double lowBound, final double upBound) {
		bindPressed(button, () -> {
			globalGate.value().setRaw(value);
		});
		bindLightState(() -> {
			final double raw = globalGate.value().getRaw();
			if (raw == value) {
				return ColorButtonLedState.RED_FLASH;
			}
			if (lowBound < raw && raw < upBound) {
				return ColorButtonLedState.RED_FLASH;
			}

			return ColorButtonLedState.RED_DIM;
		}, button);
	}

	@Override
	public void doActivate() {
		super.doActivate();
	}

	@Override
	public void doDeactivate() {
		super.doDeactivate();
	}

}
