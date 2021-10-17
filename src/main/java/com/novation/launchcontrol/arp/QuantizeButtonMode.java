package com.novation.launchcontrol.arp;

public class QuantizeButtonMode extends ButtonModeLayer {

	public QuantizeButtonMode(final LpcArpControlExtension driver, final String name) {
		super(driver, name);
		final RedGreenButton[] focusButtons = driver.getFocusButtons();
		final RedGreenButton[] controlButtons = driver.getControlButtons();
		bindButton(driver, controlButtons[0], 0);
		bindButton(driver, controlButtons[1], 2);
		bindButton(driver, controlButtons[2], 4);
		bindButton(driver, controlButtons[3], 5);
		bindButton(driver, controlButtons[4], 7);
		bindButton(driver, controlButtons[5], 9);
		bindButton(driver, controlButtons[6], 11);
		bindButton(driver, controlButtons[7], 0);
		bindButton(driver, focusButtons[1], 1);
		bindButton(driver, focusButtons[2], 3);
		bindButton(driver, focusButtons[4], 6);
		bindButton(driver, focusButtons[5], 8);
		bindButton(driver, focusButtons[6], 10);
	}

	private void bindButton(final LpcArpControlExtension driver, final RedGreenButton button, final int notNr) {

		bindPressed(button, () -> {
			final ArpInstance currentArp = driver.getCurrentArp();
			if (currentArp != null) {
				currentArp.toggleQuantizeNote(notNr);
			}
		});

		bindLightState(() -> {
			final ArpInstance currentArp = driver.getCurrentArp();
			if (currentArp != null) {
				if (currentArp.isQuantizeNoteSet(notNr)) {
					return ColorButtonLedState.GREEN_FULL;
				} else {
					return ColorButtonLedState.EMERALD_DIM;
				}
			}
			return ColorButtonLedState.OFF;
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
