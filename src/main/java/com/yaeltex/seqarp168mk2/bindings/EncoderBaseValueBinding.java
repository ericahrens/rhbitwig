package com.yaeltex.seqarp168mk2.bindings;

import com.bitwig.extension.controller.api.BooleanValue;
import com.bitwig.extension.controller.api.Parameter;
import com.bitwig.extension.controller.api.RelativeHardwarControlBindable;
import com.bitwig.extensions.framework.Binding;
import com.yaeltex.common.controls.RingEncoder;
import com.yaeltex.seqarp168mk2.device.NoteControlValue;

public class EncoderBaseValueBinding extends Binding<RingEncoder, NoteControlValue> {
    private final NoteControlValue value;
    private final RelativeHardwarControlBindable incBinder;
    private int viewValue = 0;
    private boolean exists;
    public static final int[] NOTE_ENCODER_MAPPING = new int[] { //
        // -23,-22,-21,-20,-19,-18,-17,-16,-15,-14,-13
        10, 20, 20, 20, 20, 20, 20, 20, 20, 20, 20, 20, //
        // -11,-10,-09,-08,-07,-06,-05,-04,-03,-02,-01
        30, 40, 40, 40, 40, 46, 50, 58, 58, 58, 58, 58, //
        // 01, 02, 03, 04, 05, 06, 07, 08, 09, 10, 11
        64, 73, 73, 73, 73, 73, 73, 83, 94, 94, 94, 94, //
        // 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23, 24
        100, 112, 112, 112, 112, 112, 112, 112, 112, 112, 112, 112, 127 //
    };
    
    public EncoderBaseValueBinding(final RingEncoder encoder, final NoteControlValue value,
        final BooleanValue existsReference, final Parameter viewParameter) {
        super(encoder, value);
        
        this.value = value;
        this.exists = existsReference.get();
        this.viewValue = (int) viewParameter.getRaw() + 24;
        incBinder = encoder.createIncrementBinder(this::changeValue);
        existsReference.addValueObserver(this::handleExists);
        viewParameter.value().addValueObserver(49, this::updateViewValue);
    }
    
    private void changeValue(final int inc) {
        if (!isActive()) {
            return;
        }
        this.value.incBase(inc);
    }
    
    private void handleExists(final boolean exists) {
        this.exists = exists;
        if (isActive()) {
            updateViewValue();
        }
    }
    
    private void updateViewValue() {
        if (exists) {
            getSource().updateValue(NOTE_ENCODER_MAPPING[viewValue]);
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
