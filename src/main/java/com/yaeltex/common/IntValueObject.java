package com.yaeltex.common;

import java.util.ArrayList;
import java.util.List;
import java.util.function.IntConsumer;

public class IntValueObject {

    @FunctionalInterface
    public interface Converter {
        String convert(int value);
    }

    private final List<IntConsumer> callbacks = new ArrayList<>();
    private int value;
    private final int min;
    private final int max;
    private final Converter converter;

    public IntValueObject(final int initValue, final int min, final int max) {
        this.value = initValue;
        this.min = min;
        this.max = max;
        this.converter = null;
    }

    public IntValueObject(final int initValue, final int min, final int max, final Converter converter) {
        this.value = initValue;
        this.min = min;
        this.max = max;
        this.converter = converter;
    }

    public int getMax() {
        return max;
    }

    public void addValueObserver(final IntConsumer callback) {
        if (!callbacks.contains(callback)) {
            callbacks.add(callback);
        }
    }

    public void set(final int value) {
        final int newValue = Math.max(min, Math.min(max, value));
        if (this.value == newValue) {
            return;
        }
        this.value = newValue;
        for (final IntConsumer listener : callbacks) {
            listener.accept(value);
        }
    }

    public void increment(final int amount) {
        final int newValue = Math.max(min, Math.min(max, value + amount));
        this.set(newValue);
    }

    public int getValue() {
        return value;
    }

    public double get() {
        return (value - min) / (double) (max - min);
    }

}
