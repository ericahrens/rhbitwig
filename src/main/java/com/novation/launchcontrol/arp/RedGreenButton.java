package com.novation.launchcontrol.arp;

import com.bitwig.extension.controller.api.HardwareButton;
import com.bitwig.extension.controller.api.MultiStateHardwareLight;
import com.bitwig.extensions.rh.Midi;

public class RedGreenButton {
	private final HardwareButton hwButton;
	private final int index;
	private final MultiStateHardwareLight light;
	private final int notevalue;
	private final int channel;

	public RedGreenButton(final LpcArpControlExtension driver, final String group, final int index, final int notvalue,
			final int channel) {
		this.index = index;
		this.notevalue = notvalue;
		this.channel = channel;
		hwButton = driver.getSurface().createHardwareButton(group + "_" + index);
		hwButton.pressedAction()
				.setPressureActionMatcher(driver.getMidiIn().createNoteOnVelocityValueMatcher(channel, notvalue));
		hwButton.releasedAction().setActionMatcher(driver.getMidiIn().createNoteOffActionMatcher(channel, notvalue));

		light = driver.getSurface().createMultiStateHardwareLight(group + "_LIGHT_" + index);
		light.state().setValue(ColorButtonLedState.OFF);
		light.state().onUpdateHardware(hwState -> driver.updatePadLed(this));
		hwButton.setBackgroundLight(light);
		hwButton.isPressed().addValueObserver(v -> {
			// RemoteConsole.out.println("Select {} = {}", index, v);
		});

	}

	public int getMidiDataNr() {
		return notevalue;
	}

	public int getMidiStatus() {
		return Midi.NOTE_ON | channel;
	}

	public MultiStateHardwareLight getLight() {
		return light;
	}

	public HardwareButton getHwButton() {
		return hwButton;
	}

	public int getIndex() {
		return index;
	}

}
