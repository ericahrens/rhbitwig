package com.yaeltex.controls;

import java.util.function.IntConsumer;
import java.util.function.Supplier;

import com.bitwig.extension.controller.api.HardwareSurface;
import com.bitwig.extension.controller.api.InternalHardwareLightState;
import com.bitwig.extension.controller.api.MidiIn;
import com.bitwig.extension.controller.api.MultiStateHardwareLight;
import com.bitwig.extension.controller.api.Parameter;
import com.bitwig.extension.controller.api.RelativeHardwareKnob;
import com.bitwig.extensions.framework.Layer;
import com.bitwig.extensions.framework.values.Midi;
import com.yaeltex.bindings.RingLightParameterBinding;
import com.yaeltex.common.YaelTexColors;
import com.yaeltex.common.YaeltexButtonLedState;
import com.yaeltex.common.YaeltexMidiProcessor;
import com.yaeltex.fuse.FuseExtension;

public class RingEncoder {
    private final YaeltexMidiProcessor midiProcessor;
    private final RelativeHardwareKnob encoder;
    private int lastValueSent = -1;
    private int lastColorSent = -1;
    private final int midiValue;
    private final RgbButton button;
    private final MultiStateHardwareLight light;
    
    public RingEncoder(final int channel, final int midiValue, String name, final HardwareSurface surface,
        YaeltexMidiProcessor midiProcessor) {
        super();
        this.midiProcessor = midiProcessor;
        final MidiIn midiIn = midiProcessor.getMidiIn();
        this.midiValue = midiValue;
        
        encoder = surface.createRelativeHardwareKnob(name);
        encoder.setAdjustValueMatcher(midiIn.createRelativeSignedBit2CCValueMatcher(channel, midiValue, 100));
        encoder.setStepSize(0.0075);
        light = surface.createMultiStateHardwareLight(name + "_LIGHT");
        button = new RgbButton(channel, midiValue, name + "_BUTTON", surface, midiProcessor);
    }
    
    public void bind(final Layer layer, final IntConsumer incHandler) {
        layer.bind(encoder, midiProcessor.createIncrementBinder(incHandler::accept));
    }
    
    public void bind(final Layer layer, Parameter parameter, YaelTexColors color) {
        layer.bind(encoder, parameter);
        parameter.exists().markInterested();
        layer.addBinding(new RingLightParameterBinding(parameter, this));
        bindLight(
            layer, () -> parameter.exists().get() ? YaeltexButtonLedState.of(color, 0) : YaeltexButtonLedState.OFF);
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
    
    public void sendValue(final int value) {
        if (value != lastValueSent) {
            midiProcessor.sendMidi(Midi.CC, midiValue, value);
            lastValueSent = value;
        }
    }
    
    public void setColor(final int value) {
        if (value != lastColorSent) {
            FuseExtension.println(" SEND Color => %d %d", midiValue, value);
            midiProcessor.sendMidi(Midi.CC | 15, midiValue, value);
            lastColorSent = value;
        }
    }
    
    public void refresh() {
        midiProcessor.sendMidi(Midi.CC, midiValue, lastValueSent);
        midiProcessor.sendMidi(Midi.CC | 15, midiValue, lastColorSent);
    }
    
    public void clear() {
        midiProcessor.sendMidi(Midi.CC, midiValue, 0);
    }
    
}
