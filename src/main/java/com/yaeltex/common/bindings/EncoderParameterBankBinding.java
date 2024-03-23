package com.yaeltex.common.bindings;

import com.bitwig.extension.controller.api.SettableRangedValue;
import com.bitwig.extensions.framework.values.BooleanValueObject;
import com.yaeltex.common.controls.RingEncoder;

public class EncoderParameterBankBinding extends EncoderParameterBinding {
    
    private boolean valueExists;
    
    public EncoderParameterBankBinding(final RingEncoder encoder, final SettableRangedValue target,
        final BooleanValueObject valueExists) {
        super(encoder, target);
        this.valueExists = valueExists.get();
        valueExists.addValueObserver(this::handlePageExistsChanged);
    }
    
    private void handlePageExistsChanged(final boolean exists) {
        this.valueExists = exists;
        if (isActive()) {
            if (valueExists) {
                if (hardwareBinding != null) {
                    hardwareBinding.removeBinding();
                }
                activate();
            } else {
                deactivate();
            }
        }
    }
    
    @Override
    protected void activate() {
        if (valueExists) {
            hardwareBinding = getTarget().getEncoder().addBinding(getSource());
            getTarget().setBoundToTarget(true);
            getTarget().updateValue(this.value);
        }
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
