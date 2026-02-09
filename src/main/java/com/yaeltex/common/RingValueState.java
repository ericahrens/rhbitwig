package com.yaeltex.common;

import java.util.Objects;

import com.bitwig.extension.controller.api.HardwareLightVisualState;
import com.bitwig.extension.controller.api.InternalHardwareLightState;

public class RingValueState extends InternalHardwareLightState {
    
    private static final RingValueState[] VALUES = new RingValueState[128];
    
    private final Integer value;
    
    static {
        for (int i = 0; i < VALUES.length; i++) {
            VALUES[i] = new RingValueState(i);
        }
    }
    
    public static RingValueState of(final int value) {
        return VALUES[value];
    }
    
    private RingValueState(final Integer value) {
        this.value = value;
    }
    
    public Integer getValue() {
        return value;
    }
    
    @Override
    public HardwareLightVisualState getVisualState() {
        return null;
    }
    
    @Override
    public final boolean equals(final Object o) {
        if (!(o instanceof final RingValueState that)) {
            return false;
        }
        
        return Objects.equals(value, that.value);
    }
    
    @Override
    public int hashCode() {
        return Objects.hashCode(value);
    }
}
