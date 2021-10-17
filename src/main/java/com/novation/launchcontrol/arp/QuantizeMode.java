package com.novation.launchcontrol.arp;

public enum QuantizeMode {
	MUTE("Mute"), NEAREST_VALUE("Nearest Value");
	private String descriptor;

	private QuantizeMode(final String descriptor) {
		this.descriptor = descriptor;
	}

	public static QuantizeMode toMode(final String s) {
		for (final QuantizeMode mode : QuantizeMode.values()) {
			if (mode.getDescriptor().equals(s)) {
				return mode;
			}
		}
		return MUTE;
	}

	public String getDescriptor() {
		return descriptor;
	}
}
