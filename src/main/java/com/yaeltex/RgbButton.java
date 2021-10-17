package com.yaeltex;

import com.bitwig.extension.controller.api.HardwareButton;
import com.bitwig.extension.controller.api.MultiStateHardwareLight;
import com.bitwig.extensions.rh.Midi;

public class RgbButton {

	public static final int STEPS_MT_MODE = 0;
	public static final int VEL_MT_MODE = 1;
	public static final int RETRIG_MODE = 2;
	public static final int TIME_WARP_MODE = 3;
	public static final int STEPS_VPCT_MODE = 4;
	public static final int STEPS_GPCT_MODE = 5;
	public static final int OPTIONS_MODE = 6;
	public static final int ENC_MODE = 7;

	private final HardwareButton hwButton;
	private final int index;
	private final MultiStateHardwareLight light;
	private final int notevalue;
	private final int channel;

	public RgbButton(final YaeltexArpControlExtension driver, final String group, final int index, final int notvalue,
			final int channel) {
		this.index = index;
		this.notevalue = notvalue;
		this.channel = channel;
		hwButton = driver.getSurface().createHardwareButton(group + "_" + index);
		hwButton.pressedAction()
				.setPressureActionMatcher(driver.getMidiIn().createNoteOnVelocityValueMatcher(channel, notvalue));
		hwButton.releasedAction().setActionMatcher(driver.getMidiIn().createNoteOffActionMatcher(channel, notvalue));

		light = driver.getSurface().createMultiStateHardwareLight(group + "_LIGHT_" + index);
		// light.state().setValue(ColorButtonLedState.OFF);
		light.state().onUpdateHardware(hwState -> driver.updatePadLed(this));
		hwButton.isPressed().markInterested();
		hwButton.setBackgroundLight(light);
		driver.initButton(this); // Needs to be done because of some issues when restarting the extension
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

	public ColorButtonLedState getLedState() {
		return (ColorButtonLedState) light.state().currentValue();
	}

	public void setColor(final ColorButtonLedState ledState) {
//		if (hwButton.getId().equals("ENCODER_BUTTON_8")) {
//			final StringBuilder sb = new StringBuilder();
//			final StackTraceElement[] st = Thread.currentThread().getStackTrace();
//			for (final StackTraceElement stackTraceElement : st) {
//				sb.append(stackTraceElement.toString()).append("\n");
//			}
//			RemoteConsole.out.println(" SET <{}>  {}  {}", hwButton.getId(), ledState.getColorCode(), sb);
//		}
		light.state().setValue(ledState);
	}

	public HardwareButton getHwButton() {
		return hwButton;
	}

	public int getIndex() {
		return index;
	}

}
