package com.yaeltex.seqarp168mk2.bindings;

import com.bitwig.extension.controller.api.BooleanValue;
import com.bitwig.extension.controller.api.RelativeHardwarControlBindable;
import com.bitwig.extensions.framework.Binding;
import com.yaeltex.controls.RingEncoder;
import com.yaeltex.seqarp168mk2.device.NoteControlValue;

public class EncoderOffsetValueBinding extends Binding<RingEncoder, NoteControlValue> {
    
    private final NoteControlValue value;
    private final RelativeHardwarControlBindable incBinder;
    private int viewValue = 0;
    private boolean exists;
    
    public EncoderOffsetValueBinding(final RingEncoder encoder, final NoteControlValue value,
        final BooleanValue existsReference) {
        super(encoder, value);
        
        this.value = value;
        this.exists = existsReference.get();
        this.viewValue = value.getOffsetValue() + 24;
        incBinder = encoder.createIncrementBinder(this::changeValue);
        existsReference.addValueObserver(this::handleExists);
        this.value.addOffsetValueListener(offset -> {
            updateViewValue(offset + 24);
        });
    }
    
    private void changeValue(final int inc) {
        if (!isActive()) {
            return;
        }
        this.value.incOffset(inc);
    }
    
    private void handleExists(final boolean exists) {
        this.exists = exists;
        if (isActive()) {
            updateViewValue();
        }
    }
    
    private void updateViewValue() {
        if (exists) {
            getSource().updateValue(EncoderBaseValueBinding.NOTE_ENCODER_MAPPING[viewValue]);
        } else {
            getSource().updateValue(0);
        }
    }
    
    private void updateViewValue(final int newNoteValue) {
        viewValue = newNoteValue;
        if (isActive()) {
            updateViewValue();
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
