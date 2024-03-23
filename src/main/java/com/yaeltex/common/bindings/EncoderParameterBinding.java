package com.yaeltex.common.bindings;

import com.bitwig.extension.controller.api.HardwareBinding;
import com.bitwig.extension.controller.api.SettableRangedValue;
import com.bitwig.extensions.framework.Binding;
import com.yaeltex.common.controls.RingEncoder;

public class EncoderParameterBinding extends Binding<SettableRangedValue, RingEncoder> {
    
    protected int value;
    protected HardwareBinding hardwareBinding;
    
    public EncoderParameterBinding(final RingEncoder encoder, final SettableRangedValue target) {
        super(encoder, target, encoder);
        target.addValueObserver(128, this::handleValueChanged);
        this.value = (int) (target.get() * 127);
    }
    
    private void handleValueChanged(final int value) {
        this.value = value;
    }
    
    @Override
    protected void activate() {
        hardwareBinding = getTarget().getEncoder().addBinding(getSource());
        getTarget().setBoundToTarget(true);
        getTarget().updateValue(this.value);
    }
    
    @Override
    protected void deactivate() {
        if (hardwareBinding != null) {
            getTarget().setBoundToTarget(false);
            hardwareBinding.removeBinding();
        }
        hardwareBinding = null;
    }
    
}
