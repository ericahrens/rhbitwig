package com.yaeltex.djm;

import java.util.function.Supplier;

import com.bitwig.extension.controller.api.HardwareSurface;
import com.bitwig.extension.controller.api.InternalHardwareLightState;
import com.bitwig.extension.controller.api.MidiIn;
import com.bitwig.extension.controller.api.MultiStateHardwareLight;
import com.bitwig.extension.controller.api.Parameter;
import com.bitwig.extension.controller.api.RelativeHardwareKnob;
import com.bitwig.extensions.framework.Layer;
import com.yaeltex.common.RingValueState;
import com.yaeltex.common.YaeltexButtonLedState;
import com.yaeltex.common.YaeltexMidiProcessor;
import com.yaeltex.common.controls.RgbButton;
import com.yaeltex.djm.bindings.DjmEncoderParameterBinding;

public class DjmRingEncoder {
    private final YaeltexMidiProcessor midiProcessor;
    private final RelativeHardwareKnob encoder;
    private final MultiStateHardwareLight ringLight;
    private final MultiStateHardwareLight valueLight;
    private final RgbButton button;
    
    private final int midiValue;
    private final int midiPort;
    
    
    public DjmRingEncoder(final int port, final int midiValue, final String name, final HardwareSurface surface,
        final YaeltexMidiProcessor midiProcessor) {
        this.midiPort = port;
        this.midiProcessor = midiProcessor;
        final MidiIn midiIn = midiProcessor.getMidiIn(port);
        this.midiValue = midiValue;
        encoder = surface.createRelativeHardwareKnob(name);
        encoder.setAdjustValueMatcher(midiIn.createRelativeSignedBit2CCValueMatcher(0, midiValue, 200));
        encoder.setStepSize(0.1);
        ringLight = surface.createMultiStateHardwareLight(name + "_LIGHT");
        ringLight.state().onUpdateHardware(this::handleColor);
        valueLight = surface.createMultiStateHardwareLight(name + "_VALUE_LIGHT");
        valueLight.state().onUpdateHardware(this::handleValue);
        button = new RgbButton(port, 0, midiValue, name + "_BUTTON", surface, midiProcessor);
    }
    
    private void handleValue(final InternalHardwareLightState internalHardwareLightState) {
        if (internalHardwareLightState instanceof final RingValueState value) {
            midiProcessor.sendCcValue(midiPort, midiValue, value.getValue());
        }
    }
    
    private void handleColor(final InternalHardwareLightState internalHardwareLightState) {
        if (internalHardwareLightState instanceof final YaeltexButtonLedState color) {
            setColor(color.getColorCode());
        } else {
            setColor(0);
        }
    }
    
    public void bindParameter(final Layer layer, final Parameter parameter) {
        final DjmEncoderParameterBinding binding = new DjmEncoderParameterBinding(this, parameter.value());
        layer.addBinding(binding);
    }
    
    public void bindRingLightColor(final Layer layer, final Supplier<InternalHardwareLightState> supplier) {
        layer.bindLightState(supplier, ringLight);
    }
    
    public void updateValue(final int value) {
        midiProcessor.sendCcValue(midiPort, midiValue, value);
    }
    
    public void setColor(final int value) {
        midiProcessor.sendCcColor(midiPort, midiValue, value);
    }
    
    public void setBounds(final double xMM, final double yMm, final double size) {
        final double lightSize = 2;
        final double buttonOffset = 6;
        encoder.setBounds(xMM + lightSize, yMm, size - buttonOffset, size - buttonOffset);
        ringLight.setBounds(xMM, yMm, lightSize, size);
        button.setBounds(xMM + buttonOffset, yMm + size - buttonOffset, buttonOffset, buttonOffset);
    }
    
    public RgbButton getButton() {
        return button;
    }
    
    public RelativeHardwareKnob getEncoder() {
        return encoder;
    }
    
    public void setLabel(final String name) {
        button.setLabel(name);
    }
    
    public void refresh() {
        button.refresh();
    }
}
