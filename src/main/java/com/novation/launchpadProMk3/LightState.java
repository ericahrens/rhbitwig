package com.novation.launchpadProMk3;

public enum LightState {
	NORMAL(0), FLASHING(1), PULSING(2);

	private int channel;

	LightState(final int channel) {
		this.channel = channel;
	}

	public int getChannel() {
		return channel;
	}

}
