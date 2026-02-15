package com.yaeltex.common.controls;

import java.util.function.IntSupplier;

import com.bitwig.extension.controller.api.HardwareSurface;
import com.bitwig.extension.controller.api.InternalHardwareLightState;
import com.bitwig.extension.controller.api.MultiStateHardwareLight;
import com.bitwig.extensions.framework.Layer;
import com.yaeltex.common.RingValueState;
import com.yaeltex.common.YaeltexMidiProcessor;

public class VuMeter {
    
    private final MultiStateHardwareLight light;
    private final int midiValue;
    private final YaeltexMidiProcessor midiProcessor;
    private final int portIndex;
    
    public VuMeter(final int index, final int portIndex, final HardwareSurface surface,
        final YaeltexMidiProcessor midiProcessor) {
        this.midiValue = index;
        this.portIndex = portIndex;
        this.midiProcessor = midiProcessor;
        light = surface.createMultiStateHardwareLight("VU_LIGHT_%d_%d".formatted(portIndex, index));
        light.state().onUpdateHardware(this::handleValue);
    }
    
    private void handleValue(final InternalHardwareLightState internalHardwareLightState) {
        if (internalHardwareLightState instanceof final RingValueState value) {
            midiProcessor.sendCcValue(portIndex, 2, midiValue, value.getValue());
        }
    }
    
    public void sendVuValue(final int value) {
        midiProcessor.sendCcValue(portIndex, 2, midiValue, value);
    }
    
    public void sendEncoderValue(final int value) {
        midiProcessor.sendCcValue(portIndex, 1, midiValue, value);
    }
    
    public void bindValueLight(final Layer layer, final IntSupplier valueSource) {
        layer.bindLightState(() -> RingValueState.of(valueSource.getAsInt()), light);
    }
    
}
