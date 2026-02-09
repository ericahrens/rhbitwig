package com.yaeltex.djm;

import java.util.ArrayList;
import java.util.List;

import com.bitwig.extension.controller.api.ControllerHost;
import com.bitwig.extension.controller.api.HardwareSurface;
import com.yaeltex.common.controls.RgbButton;
import com.yaeltex.common.controls.RingEncoder;
import com.yaeltex.common.controls.VuMeter;

public class DjmAHwElements {
    
    private final List<RgbButton> sideButtons = new ArrayList<>();
    private final List<RingEncoder> sculptEncoders = new ArrayList<>();
    private final List<RingEncoder> vocalEncoders = new ArrayList<>();
    private final List<RingEncoder> melodyEncoders = new ArrayList<>();
    private final List<RingEncoder> baselineEncoders = new ArrayList<>();
    private final List<RingEncoder> drumEncoders = new ArrayList<>();
    private final VuMeter vuLeft;
    private final VuMeter vuRight;
    private final int layoutLeftOffset;
    
    public DjmAHwElements(final ControllerHost host, final HardwareSurface surface,
        final DjmMidiProcessor midiProcessor, final int port) {
        layoutLeftOffset = port * 190;
        
        for (int i = 0; i < 16; i++) {
            final RgbButton sideButton =
                new RgbButton(port, 0x14 + i, "FX_EQ_%d".formatted(i + 1), surface, midiProcessor);
            sideButtons.add(sideButton);
        }
        for (int i = 0; i < 4; i++) {
            sculptEncoders.add(new RingEncoder(
                port, i, "SCULPT_%d".formatted(i + 1), surface, midiProcessor,
                RingEncoder.Mode.SIGNED_BIT, 0.01));
            vocalEncoders.add(new RingEncoder(
                port, i + 4, "VOCAL_%d".formatted(i + 1), surface, midiProcessor,
                RingEncoder.Mode.SIGNED_BIT, 0.01));
            melodyEncoders.add(new RingEncoder(
                port, i + 8, "MELODY_%d".formatted(i + 1), surface, midiProcessor,
                RingEncoder.Mode.SIGNED_BIT, 0.01));
            baselineEncoders.add(
                new RingEncoder(
                    port, i + 12, "BASELINE_%d".formatted(i + 1), surface, midiProcessor, RingEncoder.Mode.SIGNED_BIT,
                    0.01));
            drumEncoders.add(new RingEncoder(
                port, i + 16, "DRUM_%d".formatted(i + 1), surface, midiProcessor,
                RingEncoder.Mode.SIGNED_BIT, 0.01));
        }
        vuLeft = new VuMeter(0, port, surface, midiProcessor);
        vuRight = new VuMeter(1, port, surface, midiProcessor);
    }
    
    public VuMeter getVuLeft() {
        return vuLeft;
    }
    
    public VuMeter getVuRight() {
        return vuRight;
    }
    
    public List<RgbButton> getSideButtons() {
        return sideButtons;
    }
    
    public List<RingEncoder> getSculptEncoders() {
        return sculptEncoders;
    }
    
    public List<RingEncoder> getVocalEncoders() {
        return vocalEncoders;
    }
    
    public List<RingEncoder> getBaselineEncoders() {
        return baselineEncoders;
    }
    
    public List<RingEncoder> getDrumEncoders() {
        return drumEncoders;
    }
    
    public List<RingEncoder> getMelodyEncoders() {
        return melodyEncoders;
    }
}
