package com.novation.launchpadProMk3;

public enum NoteRepeatMode {
	R_32(LpColor.GREEN, 0.125), //
	R_16(LpColor.GREEN, 0.25), //
	R_08(LpColor.GREEN, 0.5), //
	R_04(LpColor.GREEN, 1), //
	R_32T(LpColor.BLUE, 1.0 / 12), //
	R_16T(LpColor.BLUE, 1.0 / 6), //
	R_08T(LpColor.BLUE, 1.0 / 3), //
	R_04T(LpColor.BLUE, 2.0 / 3);

	private final double rate;
	private final RgbState activeState;
	private final RgbState inactiveState;

	private NoteRepeatMode(final LpColor color, final double rate) {
		this.activeState = RgbState.of(color.getHiIndex(), LightState.PULSING);
		this.inactiveState = RgbState.of(0);
		this.rate = rate;
	};

	public double getRate() {
		return rate;
	}

	public RgbState getInactiveState() {
		return inactiveState;
	}

	public RgbState getActiveState() {
		return activeState;
	}
}
