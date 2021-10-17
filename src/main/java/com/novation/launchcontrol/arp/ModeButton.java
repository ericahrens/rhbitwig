package com.novation.launchcontrol.arp;

import com.bitwig.extension.controller.api.HardwareButton;
import com.bitwig.extension.controller.api.OnOffHardwareLight;

/**
 * Mode Button with simple on/off LEDs.
 */
public class ModeButton {
	private final HardwareButton hwButton;
	private final OnOffHardwareLight led;

	public ModeButton(final LpcArpControlExtension driver, final Assignment assignement) {
		hwButton = driver.getSurface().createHardwareButton(assignement.toString() + "_BUTTON");
		hwButton.pressedAction().setActionMatcher(assignement.createActionMatcherPressed(driver.getMidiIn()));
		hwButton.releasedAction().setActionMatcher(assignement.createActionMatcherReleaseed(driver.getMidiIn()));
		led = driver.getSurface().createOnOffHardwareLight(assignement + "_BUTTON_LED");
		hwButton.setBackgroundLight(led);
		led.onUpdateHardware(() -> driver.sendLedUpdate(assignement, led.isOn().currentValue() ? 127 : 0));
	}

	public HardwareButton getHwButton() {
		return hwButton;
	}

	public OnOffHardwareLight getLed() {
		return led;
	}
}
