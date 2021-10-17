package com.novation.launchcontrol.arp;

import com.bitwig.extension.controller.api.Parameter;

public class PatternTypeButtonMode extends ButtonModeLayer {

	private final Parameter mode;

	public PatternTypeButtonMode(final LpcArpControlExtension driver, final String name) {
		super(driver, name);
		mode = driver.getArpdevice().createParameter("MODE");
		mode.markInterested();
		final RedGreenButton[] focusButtons = driver.getFocusButtons();
		final RedGreenButton[] controlButtons = driver.getControlButtons();
		for (int i = 0; i < 8; i++) {
			final int index1 = i + 1;
			final int index2 = i + 9;
			final RedGreenButton row1button = focusButtons[i];
			final RedGreenButton row2button = controlButtons[i];

			bindPressed(row1button, () -> mode.value().setRaw(index1));
			bindPressed(row2button, () -> mode.value().setRaw(index2));
			bindLightState(() -> ColorButtonLedState.GREEN_DIM, row2button);
			bindLightState(() -> mode.getRaw() == index1 ? ColorButtonLedState.RED_FULL : ColorButtonLedState.RED_DIM,
					row1button);
			bindLightState(() -> mode.getRaw() == index2 ? ColorButtonLedState.RED_FULL : ColorButtonLedState.RED_DIM,
					row2button);
		}

	}

	@Override
	public void doActivate() {
		super.doActivate();
		mode.subscribe();
	}

	@Override
	public void doDeactivate() {
		super.doDeactivate();
		mode.unsubscribe();
	}

}
