package com.yaeltex.common.controls;

import com.bitwig.extension.controller.api.AbsoluteHardwareKnob;
import com.bitwig.extension.controller.api.HardwareSurface;
import com.bitwig.extension.controller.api.MidiIn;
import com.yaeltex.common.YaeltexButtonLedState;
import com.yaeltex.common.YaeltexMidiProcessor;

public class MappingValueButton {
    
    
    private final YaeltexMidiProcessor midiProcessor;
    private boolean hasTarget;
    private final int midiNr;
    private final int port;
    private final YaeltexButtonLedState onColor;
    private final YaeltexButtonLedState offColor;
    private int value;
    
    public MappingValueButton(final int port, final int midiNr, final String name, final HardwareSurface surface,
        final YaeltexMidiProcessor midiProcessor) {
        this.onColor = YaeltexButtonLedState.GREEN.intensity(1);
        this.offColor = YaeltexButtonLedState.GREEN;
        this.port = port;
        this.midiNr = midiNr;
        this.midiProcessor = midiProcessor;
        final AbsoluteHardwareKnob knob = surface.createAbsoluteHardwareKnob(name);
        final MidiIn midiIn = midiProcessor.getMidiIn(port);
        knob.setAdjustValueMatcher(midiIn.createNoteOnVelocityValueMatcher(0, midiNr));
        knob.hasTargetValue().addValueObserver(this::handleHasTarget);
        knob.targetValue().addValueObserver(this::handleTargetValue);
    }
    
    private void handleTargetValue(final double v) {
        this.value = (int) Math.round(v * 127);
        updateColor();
    }
    
    private void handleHasTarget(final boolean hasTarget) {
        this.hasTarget = hasTarget;
        updateColor();
    }
    
    private void updateColor() {
        if (hasTarget) {
            if (value == 0) {
                midiProcessor.sendNoteColor(port, 0, midiNr, onColor);
            } else {
                midiProcessor.sendNoteColor(port, 0, midiNr, offColor);
            }
        } else {
            midiProcessor.sendNoteColor(port, 0, midiNr, YaeltexButtonLedState.OFF);
        }
    }
}
