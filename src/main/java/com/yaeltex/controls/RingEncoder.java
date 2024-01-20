package com.yaeltex.controls;

import com.bitwig.extension.controller.api.HardwareSurface;
import com.bitwig.extension.controller.api.InternalHardwareLightState;
import com.bitwig.extension.controller.api.MidiIn;
import com.bitwig.extension.controller.api.MultiStateHardwareLight;
import com.bitwig.extension.controller.api.Parameter;
import com.bitwig.extension.controller.api.RelativeHardwarControlBindable;
import com.bitwig.extension.controller.api.RelativeHardwareKnob;
import com.bitwig.extension.controller.api.SettableRangedValue;
import com.bitwig.extensions.framework.Layer;
import com.bitwig.extensions.framework.values.Midi;
import com.yaeltex.bindings.EncoderIntBinding;
import com.yaeltex.common.IntValueObject;
import com.yaeltex.common.YaelTexColors;
import com.yaeltex.common.YaeltexButtonLedState;
import com.yaeltex.common.YaeltexMidiProcessor;

import java.util.function.IntConsumer;
import java.util.function.Supplier;

public class RingEncoder {
    private final YaeltexMidiProcessor midiProcessor;
    private final RelativeHardwareKnob encoder;
    private int lastValueSent = -1;
    private int lastColorSent = -1;
    private final int midiValue;
    private final RgbButton button;
    private final MultiStateHardwareLight light;

    public RingEncoder(final int channel, final int midiValue, final String name, final HardwareSurface surface,
                       final YaeltexMidiProcessor midiProcessor) {
        super();
        this.midiProcessor = midiProcessor;
        final MidiIn midiIn = midiProcessor.getMidiIn();
        this.midiValue = midiValue;

        encoder = surface.createRelativeHardwareKnob(name);
        encoder.setAdjustValueMatcher(midiIn.createRelativeSignedBit2CCValueMatcher(channel, midiValue, 100));
        encoder.setStepSize(0.0075);
        light = surface.createMultiStateHardwareLight(name + "_LIGHT");
        light.state().onUpdateHardware(this::handleColor);
        encoder.targetValue().addValueObserver(v -> updateValue(v));
        button = new RgbButton(channel, midiValue, name + "_BUTTON", surface, midiProcessor);
    }

    private void handleColor(final InternalHardwareLightState internalHardwareLightState) {
        if (internalHardwareLightState instanceof YaeltexButtonLedState color) {
            setColor(color.getColorCode());
        } else {
            setColor(1);
        }
    }

    public void bind(final Layer layer, final IntValueObject value) {
        layer.addBinding(new EncoderIntBinding(this, value));
    }

    public RelativeHardwarControlBindable createIncrementBinder(final IntConsumer incHandler) {
        return midiProcessor.createIncrementBinder(incHandler::accept);
    }

    public void bind(final Layer layer, final IntConsumer incHandler) {
        layer.bind(encoder, midiProcessor.createIncrementBinder(incHandler::accept));
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
