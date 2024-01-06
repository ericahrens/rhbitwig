package com.yaeltex.bindings;

import com.bitwig.extension.controller.api.Parameter;
import com.bitwig.extensions.framework.Binding;
import com.yaeltex.controls.RingEncoder;

public class RingLightParameterBinding extends Binding<Parameter, RingEncoder> {
    private double value;
    
    public RingLightParameterBinding(final Parameter source, final RingEncoder target) {
        super(target, source, target);
        source.value().addValueObserver(this::valueChange);
    }
    
    private void valueChange(final double value) {
        final int sendValue = (int) (value * 127);
        this.value = value;
        if (isActive()) {
            getTarget().sendValue(sendValue);
        }
    }
    
    @Override
    protected void deactivate() {
    }
    
    @Override
    protected void activate() {
        final int sendValue = (int) (value * 127);
        getTarget().sendValue(sendValue);
    }
    
}
