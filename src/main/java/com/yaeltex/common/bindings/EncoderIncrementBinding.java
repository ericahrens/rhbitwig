package com.yaeltex.common.bindings;

import java.util.function.IntConsumer;

import com.bitwig.extension.controller.api.RelativeHardwarControlBindable;
import com.bitwig.extensions.framework.Binding;
import com.bitwig.extensions.framework.values.IntValueObject;
import com.yaeltex.common.controls.RingEncoder;

public class EncoderIncrementBinding extends Binding<RingEncoder, IntValueObject> {
    
    private final RelativeHardwarControlBindable incBinder;
    private int lastValue = 0;
    
    public EncoderIncrementBinding(final RingEncoder encoder, final IntValueObject value,
        final IntConsumer intConsumer) {
        super(encoder, value);
        value.addValueObserver(this::handleValueChanged);
        lastValue = value.get();
        incBinder = encoder.createIncrementBinder(intConsumer);
    }
    
    private void handleValueChanged(final int old, final int newValue) {
        if (lastValue != newValue) {
            lastValue = newValue;
            if (isActive()) {
                updateViewValue();
            }
        }
    }
    
    private void updateViewValue() {
        getSource().updateValue(lastValue);
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
