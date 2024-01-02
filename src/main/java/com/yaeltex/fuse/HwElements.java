package com.yaeltex.fuse;

import java.util.ArrayList;
import java.util.List;

import com.bitwig.extension.controller.api.AbsoluteHardwareKnob;
import com.bitwig.extension.controller.api.ControllerHost;
import com.bitwig.extension.controller.api.HardwareSlider;
import com.bitwig.extension.controller.api.HardwareSurface;
import com.bitwig.extension.controller.api.MidiIn;
import com.bitwig.extensions.framework.di.Component;
import com.yaeltex.common.YaeltexMidiProcessor;
import com.yaeltex.controls.RgbButton;

@Component
public class HwElements {
    
    private final HardwareSlider masterSlider;
    private final HardwareSlider crossFader;
    private final List<StripControl> stripControls = new ArrayList<>();
    
    private static final StripControlOffsets[] midiMappings = {
        new StripControlOffsets(0x14, 0x1C),
        new StripControlOffsets(0x16, 0x1E),
        new StripControlOffsets(0x18, 0x20),
        new StripControlOffsets(0x1A, 0x22),
        new StripControlOffsets(0x24, 0x28),
        new StripControlOffsets(0x26, 0x2A),
    };
    
    
    private record StripControlOffsets(int abOffset, int fxOffset) {
    }
    
    
    public HwElements(ControllerHost host, HardwareSurface surface, YaeltexMidiProcessor midiProcessor) {
        masterSlider = createSliderPitchBend("MASTER_FILTER", surface, midiProcessor, 6);
        crossFader = createSliderPitchBend("CROSS_FADER", surface, midiProcessor, 7);
        crossFader.value().addValueObserver(v -> FuseExtension.println(" > %f", v));
        for(int i=0;i<6;i++) {
            stripControls.add(createControl(i, surface, midiProcessor));
        }
    }
    
    private StripControl createControl(int index, HardwareSurface surface, YaeltexMidiProcessor midiProcessor) {
        final StripControlOffsets config = midiMappings[index];
        final HardwareSlider fader =
            createSliderPitchBend("MIX_FADER_%d".formatted(index + 1), surface, midiProcessor, index);
        final RgbButton abButton = new RgbButton(config.abOffset(), "AB_%d".formatted(index + 1), surface, midiProcessor);
        final RgbButton cueButton = new RgbButton(config.abOffset()+1, "CUE_%d".formatted(index + 1), surface, midiProcessor);
        final RgbButton fxButton = new RgbButton(config.fxOffset(), "FX_%d".formatted(index + 1), surface, midiProcessor);
        final RgbButton muteButton = new RgbButton(config.fxOffset()+1, "MUTE_%d".formatted(index + 1), surface, midiProcessor);
        int knobOffset = 0x50 + index*4;
        List<AbsoluteHardwareKnob> fxKnobs = new ArrayList<>();
        for(int i = 0; i< 4;i++) {
            fxKnobs.add(createKnob("CH_%d_FX%d".formatted(index+1, i+1),surface,midiProcessor,0, knobOffset+i));
        }
        final AbsoluteHardwareKnob plusKnob =
            createKnob("CH_%d_PLUS".formatted(index + 1), surface, midiProcessor, 0, 0x44 + index * 2);
        final AbsoluteHardwareKnob multKnob =
            createKnob("CH_%d_MULT".formatted(index + 1), surface, midiProcessor, 0, 0x45 + index * 2);
        return new StripControl(fader,abButton,cueButton,fxButton,muteButton,fxKnobs,plusKnob,multKnob);
    }
    
    private HardwareSlider createSliderPitchBend(
        String name, final HardwareSurface surface, final YaeltexMidiProcessor midiProcessor, final int channel) {
        HardwareSlider fader = surface.createHardwareSlider(name);
        final MidiIn midiIn = midiProcessor.getMidiIn();
        fader.setAdjustValueMatcher(midiIn.createAbsolutePitchBendValueMatcher(channel));
        return fader;
    }
    
    private AbsoluteHardwareKnob createKnob(String name, HardwareSurface surface, YaeltexMidiProcessor midiProcessor, int channel, int ccNr) {
        AbsoluteHardwareKnob knob = surface.createAbsoluteHardwareKnob(name);
        final MidiIn midiIn = midiProcessor.getMidiIn();
        knob.setAdjustValueMatcher(midiIn.createAbsoluteCCValueMatcher(channel, ccNr));
        return knob;
    }
    
    public HardwareSlider getCrossFader() {
        return crossFader;
    }
    
    public HardwareSlider getMasterSlider() {
        return masterSlider;
    }
    
    public List<StripControl> getStripControls() {
        return stripControls;
    }
}
