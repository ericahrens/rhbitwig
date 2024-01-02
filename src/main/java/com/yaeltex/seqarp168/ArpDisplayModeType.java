package com.yaeltex.seqarp168;

public enum ArpDisplayModeType {
	MODE_1X8(8, 1), MODE_2X8(8, 2), MODE_1X16(16, 1), MODE_2X16(16, 2);
	final int maxSteps;
	final int devices;

	private ArpDisplayModeType(final int maxSteps, final int devices) {
		this.maxSteps = maxSteps;
		this.devices = devices;
	}

	public int getMaxSteps() {
		return maxSteps;
	}

	public int getDevices() {
		return devices;
	}

}
