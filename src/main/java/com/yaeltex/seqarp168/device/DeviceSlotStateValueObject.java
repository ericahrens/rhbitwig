package com.yaeltex.seqarp168.device;

import java.util.ArrayList;
import java.util.List;

import com.bitwig.extension.callback.ValueChangedCallback;

public class DeviceSlotStateValueObject {

	private DeviceSlotState state = DeviceSlotState.EMPTY;
	private final List<DeviceSlotStateValueChangedCallback> callbacks = new ArrayList<DeviceSlotStateValueChangedCallback>();

	@FunctionalInterface
	public interface DeviceSlotStateValueChangedCallback extends ValueChangedCallback {
		void valueChanged(DeviceSlotState newValue);
	}

	public DeviceSlotState get() {
		return state;
	}

	public void set(final DeviceSlotState state) {
		this.state = state;
		callbacks.forEach(callbacks -> callbacks.valueChanged(this.state));
	}

	public void addValueObserver(final DeviceSlotStateValueChangedCallback callback) {
		if (!callbacks.contains(callback)) {
			callbacks.add(callback);
		}
	}

	public void removeValueObserver(final DeviceSlotStateValueChangedCallback callback) {
		if (callbacks.contains(callback)) {
			callbacks.remove(callback);
		}
	}

}
