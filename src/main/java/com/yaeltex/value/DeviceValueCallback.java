package com.yaeltex.value;

@FunctionalInterface
public interface DeviceValueCallback {
	void changed(int device, int value);
}
