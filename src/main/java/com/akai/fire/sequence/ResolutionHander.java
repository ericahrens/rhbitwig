package com.akai.fire.sequence;

import com.akai.fire.lights.BiColorLightState;

public class ResolutionHander {
	private final DrumSequenceMode parent;
	private boolean buttonHeld = false;
	private static final double[] GRID_RATES = new double[] { 0.125, 0.25, 0.5, 1.0, 2.0, 4.0, //
			1.0 / 12, 1.0 / 6, 1.0 / 3, 2.0 / 3 };
	private static final String[] GRID_RATES_STR = new String[] { "1/32", "1/16", "1/8", "1/4", "1/2", "1/1", //
			"1/32T", "1/16T", "1/8T", "1/4T" };

	public ResolutionHander(final DrumSequenceMode drumSequenceMode) {
		this.parent = drumSequenceMode;
	}

	BiColorLightState getLightState() {
		return buttonHeld ? BiColorLightState.AMBER_FULL : BiColorLightState.AMBER_HALF;
	}

	void handlePressed(final boolean pressed) {
		buttonHeld = pressed;
		if (pressed) {
			final int index = resValue();
			parent.getOled().valueInfo("Grid", GRID_RATES_STR[index]);
		} else {
			parent.getOled().clearScreenDelayed();
		}
	}

	private int resValue() {
		final double current = parent.getPositionHandler().getGridResolution();
		for (int i = 0; i < GRID_RATES.length; i++) {
			if (current == GRID_RATES[i]) {
				return i;
			}
		}
		return 0;
	}

	boolean isHolding() {
		return buttonHeld;
	}

	public void handleMainEncoder(final int inc) {
		if (!buttonHeld) {
			return;
		}
		final int index = resValue();
		final int newValue = index + inc;
		if (newValue >= 0 && newValue < GRID_RATES.length) {
			parent.getPositionHandler().setGridResolution(GRID_RATES[newValue]);
			parent.getOled().valueInfo("Grid", GRID_RATES_STR[newValue]);
		}
	}

	public void handeMainEncoderPress(final boolean press) {

	}

}
