package com.novation.launchcontrol.arp;

import java.util.List;

import com.bitwig.extension.controller.api.Parameter;

public class VelocityGateButtonMode extends ButtonModeLayer {

	public VelocityGateButtonMode(final LpcArpControlExtension driver, final String name) {
		super(driver, name);
		final RedGreenButton[] focusButtons = driver.getFocusButtons();
		final RedGreenButton[] controlButtons = driver.getControlButtons();
		final List<Parameter> arpVelParams = driver.getArpVelocityParams();
		final List<Parameter> arpGateParams = driver.getArpGateParams();

		for (int i = 0; i < 8; i++) {
			final int index = i;
			final RedGreenButton velbutton = focusButtons[i];
			final RedGreenButton gatebutton = controlButtons[i];

			bindPressed(velbutton, () -> {
				final ArpInstance currentArp = driver.getCurrentArp();
				if (currentArp != null) {
					final Parameter parm = arpVelParams.get(index);
					toggleVelocity(index, currentArp, parm);
				}
			});

			bindPressed(gatebutton, () -> {
				final ArpInstance currentArp = driver.getCurrentArp();
				if (currentArp != null) {
					currentArp.toggleGate(index, arpGateParams.get(index));
				}
			});

			bindLightState(() -> velValueToLed(arpVelParams.get(index).value().get()), velbutton);
			bindLightState(() -> gateValueToLed(arpGateParams.get(index).value().get(), index), gatebutton);
		}
	}

	private void toggleVelocity(final int index, final ArpInstance currentArp, final Parameter parm) {
		if (parm.value().get() == 1.0) {
			parm.value().setImmediately(currentArp.getStoredVelocity(index));
		} else {
			currentArp.storeVelocity(index, parm.value().get());
			parm.value().setImmediately(1);
		}
	}

	private ColorButtonLedState velValueToLed(final double value) {
		if (value == 0) {
			return ColorButtonLedState.OFF;
		} else if (value < 0.5) {
			return ColorButtonLedState.GREEN_DIM;
		} else if (value == 1) {
			return ColorButtonLedState.GREEN_FULL;
		} else {
			return ColorButtonLedState.GREEN_SEMI;
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
