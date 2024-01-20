package com.yaeltex.bindings;

import com.bitwig.extension.controller.api.RelativeHardwarControlBindable;
import com.bitwig.extensions.framework.Binding;
import com.yaeltex.common.IntValueObject;
import com.yaeltex.controls.RingEncoder;

public class EncoderIntBinding extends Binding<RingEncoder, IntValueObject> {

    private final int value;
    private final RelativeHardwarControlBindable incBinder;

    public EncoderIntBinding(final RingEncoder encoder, final IntValueObject target) {
        super(encoder, target);
        encoder.getEncoder().hasTargetValue().markInterested();
        incBinder = encoder.createIncrementBinder(
                incValue -> target.increment(incValue));

        this.value = target.getValue();
        target.addValueObserver(newValue -> {
            if (isActive()) {
                encoder.updateValue(target.get());
            }
        });
    }

    @Override
    protected void deactivate() {
        getSource().getEncoder().clearBindings();
    }

    @Override
    protected void activate() {
        getSource().getEncoder().addBinding(incBinder);
        getSource().updateValue(value);
    }
}
