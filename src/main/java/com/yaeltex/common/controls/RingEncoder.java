package com.yaeltex.common.controls;

import java.util.function.IntConsumer;
import java.util.function.Supplier;

import com.bitwig.extension.controller.api.AbsoluteHardwareValueMatcher;
import com.bitwig.extension.controller.api.BooleanValue;
import com.bitwig.extension.controller.api.HardwareSurface;
import com.bitwig.extension.controller.api.InternalHardwareLightState;
import com.bitwig.extension.controller.api.MidiIn;
import com.bitwig.extension.controller.api.MultiStateHardwareLight;
import com.bitwig.extension.controller.api.Parameter;
import com.bitwig.extension.controller.api.RelativeHardwarControlBindable;
import com.bitwig.extension.controller.api.RelativeHardwareKnob;
import com.bitwig.extension.controller.api.SettableRangedValue;
import com.bitwig.extensions.framework.Layer;
import com.bitwig.extensions.framework.values.IntValueObject;
import com.bitwig.extensions.framework.values.Midi;
import com.yaeltex.common.YaelTexColors;
import com.yaeltex.common.YaeltexButtonLedState;
import com.yaeltex.common.YaeltexMidiProcessor;
import com.yaeltex.common.bindings.EncoderIncrementBinding;
import com.yaeltex.common.bindings.EncoderParameterBinding;
import com.yaeltex.common.bindings.EncoderParameterValueBinding;
import com.yaeltex.seqarp168mk2.SeqArp168Extension;
import com.yaeltex.seqarp168mk2.bindings.EncoderBaseValueBinding;
import com.yaeltex.seqarp168mk2.bindings.EncoderOffsetValueBinding;
import com.yaeltex.seqarp168mk2.device.NoteControlValue;

public class RingEncoder {
    private final YaeltexMidiProcessor midiProcessor;
    private final RelativeHardwareKnob encoder;
    private int lastValueSent = -1;
    private int lastColorSent = -1;
    private final int lastIntensityValueSent = 127;
    private final int midiValue;
    private final RgbButton button;
    private final MultiStateHardwareLight light;
    private boolean boundToTarget = false;
    
    public enum Mode {
        SIGNED_BIT,
        BINARY_OFFSET,
        BINARY_OFFSET_REVERSE
    }
    
    public RingEncoder(final int channel, final int midiValue, final String name, final HardwareSurface surface,
        final YaeltexMidiProcessor midiProcessor) {
        this(channel, midiValue, name, surface, midiProcessor, Mode.SIGNED_BIT);
    }
    
    public RingEncoder(final int midiValue, final String name, final HardwareSurface surface,
        final YaeltexMidiProcessor midiProcessor, final Mode mode) {
        this(0, midiValue, name, surface, midiProcessor, mode);
    }
    
    public RingEncoder(final int channel, final int midiValue, final String name, final HardwareSurface surface,
        final YaeltexMidiProcessor midiProcessor, final Mode mode) {
        super();
        this.midiProcessor = midiProcessor;
        final MidiIn midiIn = midiProcessor.getMidiIn();
        this.midiValue = midiValue;
        
        final AbsoluteHardwareValueMatcher absoluteMatcher = midiIn.createAbsoluteCCValueMatcher(channel, midiValue);
        encoder = surface.createRelativeHardwareKnob(name);
        switch (mode) {
            case SIGNED_BIT ->
                encoder.setAdjustValueMatcher(midiIn.createRelativeSignedBit2CCValueMatcher(channel, midiValue, 100));
            case BINARY_OFFSET ->
                encoder.setAdjustValueMatcher(midiIn.createRelativeSignedBitCCValueMatcher(channel, midiValue, 100));
            case BINARY_OFFSET_REVERSE ->
                encoder.setAdjustValueMatcher(midiIn.createRelativeSignedBitCCValueMatcher(channel, midiValue, -100));
        }
        
        encoder.setStepSize(0.0075);
        light = surface.createMultiStateHardwareLight(name + "_LIGHT");
        light.state().onUpdateHardware(this::handleColor);
        encoder.targetValue().addValueObserver(this::handleTargetUpdating);
        button = new RgbButton(channel, midiValue, name + "_BUTTON", surface, midiProcessor);
    }
    
    public void setBoundToTarget(final boolean boundToTarget) {
        this.boundToTarget = boundToTarget;
    }
    
    private void handleTargetUpdating(final double v) {
        if (boundToTarget) {
            updateValue(v);
        }
    }
    
    private void handleColor(final InternalHardwareLightState internalHardwareLightState) {
        if (internalHardwareLightState instanceof YaeltexButtonLedState color) {
            setColor(color.getColorCode());
        } else {
            setColor(1);
        }
    }
    
    
    public int getMidiValue() {
        return midiValue;
    }
    
    public RelativeHardwarControlBindable createIncrementBinder(final IntConsumer incHandler) {
        return midiProcessor.createIncrementBinder(incHandler::accept);
    }
    
    public RelativeHardwarControlBindable createAccelIncrementBinder(final IntConsumer incHandler,
        final int resolution) {
        return midiProcessor.createAccelIncrementBinder(incHandler::accept, resolution);
    }
    
    public void bind(final Layer layer, final IntConsumer incHandler) {
        layer.bind(encoder, midiProcessor.createIncrementBinder(incHandler::accept));
    }
    
    public void bind(final Layer layer, final IntConsumer incHandler, final IntValueObject value) {
        layer.addBinding(new EncoderIncrementBinding(this, value, incHandler));
    }
    
    public void bind(final Layer layer, final Parameter parameter, final YaelTexColors color) {
        layer.bind(encoder, parameter);
        parameter.exists().markInterested();
        bindLight(layer,
            () -> parameter.exists().get() ? YaeltexButtonLedState.of(color, 0) : YaeltexButtonLedState.OFF);
    }
    
    public void bind(final Layer layer, final SettableRangedValue value, final YaelTexColors color) {
        layer.bind(encoder, value);
        bindLight(layer, () -> YaeltexButtonLedState.of(color));
    }
    
    public void bind(final Layer layer, final SettableRangedValue value) {
        layer.addBinding(new EncoderParameterBinding(this, value));
    }
    
    public void bindValue(final Layer layer, final SettableRangedValue value, final BooleanValue existsSource,
        final int resolution) {
        layer.addBinding(new EncoderParameterValueBinding(this, value, existsSource, resolution));
    }
    
    public void bindNoteValue(final Layer layer, final NoteControlValue noteValue, final Parameter parameter,
        final BooleanValue existsReference) {
        layer.addBinding(new EncoderBaseValueBinding(this, noteValue, existsReference, parameter));
    }
    
    public void bindOffsetValue(final Layer layer, final NoteControlValue noteValue,
        final BooleanValue existsReference) {
        layer.addBinding(new EncoderOffsetValueBinding(this, noteValue, existsReference));
    }
    
    
    public void bindLight(final Layer layer, final Supplier<InternalHardwareLightState> supplier) {
        layer.bindLightState(supplier, light);
    }
    
    public RelativeHardwareKnob getEncoder() {
        return encoder;
    }
    
    public RgbButton getButton() {
        return button;
    }
    
    public void updateValue(final double value) {
        final int sendValue = (int) (value * 127);
        updateValue(sendValue);
    }
    
    public void updateValue(final int value) {
        if (value != lastValueSent) {
            midiProcessor.sendMidi(Midi.CC, midiValue, value);
            lastValueSent = value;
        }
    }
    
    public void setColor(final int value) {
        if (value != lastColorSent) {
            midiProcessor.sendMidi(Midi.CC | 15, midiValue, value);
            lastColorSent = value;
        }
    }
    
    public void refresh() {
        midiProcessor.sendMidi(Midi.CC, midiValue, lastValueSent);
        midiProcessor.sendMidi(Midi.CC | 15, midiValue, lastColorSent);
    }
    
    public void clear() {
        if (midiValue == 4) {
            SeqArp168Extension.println("Target=> <%d> clear", midiValue);
        }
        midiProcessor.sendMidi(Midi.CC, midiValue, 0);
    }
    
    public void setBounds(final double xMM, final double yMm, final double size) {
        final double lightSize = 2;
        final double buttonOffset = 6;
        encoder.setBounds(xMM + lightSize, yMm, size - buttonOffset, size - buttonOffset);
        light.setBounds(xMM, yMm, lightSize, size);
        button.setBounds(xMM + buttonOffset, yMm + size - buttonOffset, buttonOffset, buttonOffset);
    }
    
    
}
