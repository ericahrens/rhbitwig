package com.yaeltex.value;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class SettableValueObject<T> {

	private T value;
	private final List<Consumer<T>> callbacks = new ArrayList<Consumer<T>>();

	public void addValueObserver(final Consumer<T> callback) {
		if (!callbacks.contains(callback)) {
			callbacks.add(callback);
		}
	}

	public boolean isSubscribed() {
		return !callbacks.isEmpty();
	}

	public void setValue(final T value) {
		if (value != this.value) {
			this.value = value;
			for (final Consumer<T> callback : callbacks) {
				callback.accept(value);
			}
		}
	}

	public T get() {
		return value;
	}

}
