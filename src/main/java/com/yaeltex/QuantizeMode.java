package com.yaeltex;

public enum QuantizeMode {
	OFF("Disabled"), MUTE("Mute"), NEAREST_VALUE("Nearest Value");
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

	public static String[] getDescriptors() {
		final String[] descriptors = new String[QuantizeMode.values().length];
		for (int i = 0; i < QuantizeMode.values().length; i++) {
			descriptors[i] = QuantizeMode.values()[i].getDescriptor();
		}
		return descriptors;
	}

	public String getDescriptor() {
		return descriptor;
	}
}
