package com.yaeltex.seqarp168mk2.device;

import java.util.ArrayList;
import java.util.List;
import java.util.function.IntConsumer;

public class NoteControlValue {
    
    private int baseValue;
    private int offsetValue;
    private final List<IntConsumer> baseListeners = new ArrayList<>();
    private final List<IntConsumer> offsetListeners = new ArrayList<>();
    
    public int getBaseValue() {
        return baseValue;
    }
    
    public int getOffsetValue() {
        return offsetValue;
    }
    
    public void setBaseValue(final int value) {
        if (value != this.baseValue) {
            this.baseValue = value;
            baseListeners.forEach(listener -> listener.accept(this.baseValue));
        }
    }
    
    public void setOffsetValue(final int value) {
        if (value != this.offsetValue) {
            this.offsetValue = value;
            offsetListeners.forEach(listener -> listener.accept(this.offsetValue));
        }
    }
    
    public void incBase(final int inc) {
        final int value = baseValue + inc;
        if (value >= -24 && value <= 24 && this.baseValue != value) {
            this.baseValue = value;
            baseListeners.forEach(listener -> listener.accept(this.baseValue));
        }
    }
    
    public void incOffset(final int inc) {
        final int value = offsetValue + inc;
        if (value >= -24 && value <= 24 && this.offsetValue != value) {
            this.offsetValue = value;
            offsetListeners.forEach(listener -> listener.accept(this.offsetValue));
        }
    }
    
    public void addBaseValueListener(final IntConsumer listener) {
        baseListeners.add(listener);
    }
    
    public void addOffsetValueListener(final IntConsumer listener) {
        offsetListeners.add(listener);
    }
    
    
}
