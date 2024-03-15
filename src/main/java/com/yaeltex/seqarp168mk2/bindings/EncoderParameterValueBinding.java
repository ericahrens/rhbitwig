package com.yaeltex.seqarp168mk2.bindings;

import com.bitwig.extension.controller.api.BooleanValue;
import com.bitwig.extension.controller.api.RelativeHardwarControlBindable;
import com.bitwig.extension.controller.api.SettableRangedValue;
import com.bitwig.extensions.framework.Binding;
import com.yaeltex.controls.RingEncoder;

public class EncoderParameterValueBinding extends Binding<RingEncoder, SettableRangedValue> {
    
    private int lastValue = 0;
    private boolean exists;
    private final RelativeHardwarControlBindable incBinder;
    
    public EncoderParameterValueBinding(final RingEncoder encoder, final SettableRangedValue value,
        final BooleanValue existsReference, final int resolution) {
        super(encoder, value);
        value.addValueObserver(128, this::handleValueChange);
        incBinder = encoder.createAccelIncrementBinder(this::changeValue, resolution);
        this.exists = existsReference.get();
        existsReference.addValueObserver(this::handleExists);
    }
    
    private void changeValue(final int inc) {
        if (!isActive()) {
            return;
        }
        final int newValue = Math.max(0, Math.min(127, lastValue + inc));
        getTarget().set(newValue, 128);
    }
    
    private void handleExists(final boolean exists) {
        this.exists = exists;
        if (isActive()) {
            updateViewValue();
        }
    }
    
    private void handleValueChange(final int newValue) {
        if (newValue != lastValue) {
            lastValue = newValue;
        }
        if (isActive()) {
            updateViewValue();
        }
    }
    
    private void updateViewValue() {
        if (exists) {
            getSource().updateValue(lastValue);
        } else {
            getSource().updateValue(0);
        }
    }
    
    @Override
    protected void deactivate() {
        getSource().getEncoder().clearBindings();
    }
    
    @Override
    protected void activate() {
        getSource().getEncoder().addBinding(incBinder);
        updateViewValue();
    }
}