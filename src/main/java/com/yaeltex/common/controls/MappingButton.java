package com.yaeltex.common.controls;

import com.bitwig.extension.controller.api.AbsoluteHardwareKnob;
import com.bitwig.extension.controller.api.HardwareSurface;
import com.bitwig.extension.controller.api.MidiIn;
import com.bitwig.extension.controller.api.OnOffHardwareLight;
import com.yaeltex.common.YaeltexButtonLedState;
import com.yaeltex.common.YaeltexMidiProcessor;
import com.yaeltex.djm.extensions.DjmBControllerExtension;

public class MappingButton extends AbstractYaeltexButton {
    private final YaeltexButtonLedState onColor;
    private final YaeltexButtonLedState offColor;
    
    public MappingButton(final int port, final int midiId, final String name, final HardwareSurface surface,
        final YaeltexMidiProcessor midiProcessor) {
        super(port, 0, midiId, name, surface, midiProcessor);
        this.offColor = YaeltexButtonLedState.GREEN.intensity(1);
        this.onColor = YaeltexButtonLedState.GREEN;
        
        final AbsoluteHardwareKnob knob = surface.createAbsoluteHardwareKnob(name + "--");
        final MidiIn midiIn = midiProcessor.getMidiIn(port);
        knob.setAdjustValueMatcher(midiIn.createNoteOffVelocityValueMatcher(0, midiId));
        
        final OnOffHardwareLight light = surface.createOnOffHardwareLight("%s_LIGHT_%d".formatted(name, midiId));
        hwButton.setBackgroundLight(light);
        light.onUpdateHardware(() -> {
            midiProcessor.sendNoteColor(port, 0, midiId, light.isOn().currentValue() ? onColor : offColor);
        });
    }
    
    private void handleHasTarget(final boolean b) {
        DjmBControllerExtension.println("Has target %d %s ", midiId, b);
    }
    
}
