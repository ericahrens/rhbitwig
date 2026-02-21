package com.yaeltex.djm.bindings;

import com.bitwig.extension.controller.api.HardwareBinding;
import com.bitwig.extension.controller.api.SettableRangedValue;
import com.bitwig.extensions.framework.Binding;
import com.yaeltex.djm.DjmRingEncoder;

public class DjmEncoderParameterBinding extends Binding<SettableRangedValue, DjmRingEncoder> {
    
    protected int value;
    protected HardwareBinding hardwareBinding;
    
    public DjmEncoderParameterBinding(final DjmRingEncoder encoder, final SettableRangedValue target) {
        super(encoder, target, encoder);
        target.addValueObserver(128, this::handleValueChanged);
        this.value = (int) (target.get() * 127);
    }
    
    private void handleValueChanged(final int value) {
        this.value = value;
        if (isActive()) {
            getTarget().updateValue(this.value);
        }
    }
    
    @Override
    protected void activate() {
        hardwareBinding = getTarget().getEncoder().addBinding(getSource());
        getTarget().updateValue(this.value);
    }
    
    @Override
    protected void deactivate() {
        if (hardwareBinding != null) {
            hardwareBinding.removeBinding();
        }
        hardwareBinding = null;
    }
    
}
