package com.novation.launchcontrol.arp;

import com.bitwig.extension.controller.api.Parameter;

public class GlobalGateButtonMode extends ButtonModeLayer {

	public GlobalGateButtonMode(final LpcArpControlExtension driver, final String name) {
		super(driver, name);
		final RedGreenButton[] focusButtons = driver.getFocusButtons();
		final RedGreenButton[] controlButtons = driver.getControlButtons();

		assignStepControl(focusButtons, driver.getArpStepsParam(), driver.getArpStepPositionParam());

		final Parameter globalGate = driver.getArpdevice().createParameter("GLOBAL_GATE");
		globalGate.markInterested();

		final double[] values = { 0, 0.5, 0.75, 1.0, 1.5, 2.0, 3.0, 4.0 };
		for (int i = 0; i < values.length; i++) {
			final double lower = i == 0 ? 0 : values[i];
			final double upper = i == values.length - 1 ? values[i] : values[i + 1];
			bindButtonToValue(controlButtons[i], globalGate, values[i], lower, upper);
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
				return ColorButtonLedState.ORANGE_FULL;
			}
			if (lowBound < raw && raw < upBound) {
				return ColorButtonLedState.ORANGE_FULL;
			}

			return ColorButtonLedState.ORANGE_DIM;
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
