package com.yaeltex.value;

import java.util.ArrayList;
import java.util.List;

public class DeviceValueObject {
	private final int[] values;
	private final List<DeviceValueCallback> callbacks = new ArrayList<>();

	public DeviceValueObject(final int dimesion) {
		this.values = new int[dimesion];
	}

	public void addValueObserver(final DeviceValueCallback callback) {
		if (!callbacks.contains(callback)) {
			callbacks.add(callback);
		}
	}

	public int get(final int index) {
		return this.values[index];
	}

	public void set(final int index, final int value) {
		if (this.values[index] != value) {
			this.values[index] = value;
			for (final DeviceValueCallback callback : callbacks) {
				callback.changed(index, value);
			}
		}
	}
}
