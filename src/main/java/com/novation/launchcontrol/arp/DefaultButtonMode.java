package com.novation.launchcontrol.arp;

import java.util.List;

import com.bitwig.extension.controller.api.IntegerValue;
import com.bitwig.extension.controller.api.Parameter;

public class DefaultButtonMode extends ButtonModeLayer {

	public DefaultButtonMode(final LpcArpControlExtension driver, final String name) {
		super(driver, name);
		final RedGreenButton[] focusButtons = driver.getFocusButtons();

		final RedGreenButton[] controlButtons = driver.getControlButtons();

		final Parameter steps = driver.getArpStepsParam();
		final IntegerValue stepPosition = driver.getArpStepPositionParam();

		assignStepControl(controlButtons, steps, stepPosition);
		final List<Parameter> arpSkipParam = driver.getArpSkipStepsParam();
		for (int i = 0; i < 8; i++) {
			final int index = i;
			final RedGreenButton gatebutton = focusButtons[index];
			bindPressed(gatebutton, () -> {
				final ArpInstance currentArp = driver.getCurrentArp();
				if (currentArp != null) {
					currentArp.toggleSkip(index, arpSkipParam.get(index));
				}
			});
			bindLightState(() -> skipValueToLed(arpSkipParam.get(index).value().get(), index), gatebutton);
		}
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
