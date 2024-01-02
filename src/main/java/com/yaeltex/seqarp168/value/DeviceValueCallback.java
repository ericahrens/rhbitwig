package com.yaeltex.seqarp168.value;

@FunctionalInterface
public interface DeviceValueCallback {
	void changed(int device, int value);
}
