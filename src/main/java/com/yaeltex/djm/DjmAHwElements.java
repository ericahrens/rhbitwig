package com.yaeltex.djm;

import java.util.ArrayList;
import java.util.List;

import com.bitwig.extension.controller.api.AbsoluteHardwareKnob;
import com.bitwig.extension.controller.api.ControllerHost;
import com.bitwig.extension.controller.api.HardwareSurface;
import com.bitwig.extension.controller.api.MidiIn;
import com.bitwig.extension.controller.api.RelativePosition;
import com.yaeltex.common.controls.RgbButton;
import com.yaeltex.common.controls.RingEncoder;
import com.yaeltex.common.controls.VuMeter;

public class DjmAHwElements {
    public static final int KNOB_SIZE = 24;
    public static final int VERT_GAP = 6;
    private final String[] BUTTONS = {
        "AUX2", "AUX4", "LF", "CT", "AUX1", "AUX3", "FQ", "EQ", "A1", "A2", "DM", "FT", "FX1", "FX2", "FX3", "FX4"
    };
    private final int[] BUTTON_LAYOUT = {1, 3, 5, 7, 0, 2, 4, 6, 9, 11, 13, 15, 8, 10, 12, 14};
    private final List<RgbButton> sideButtons = new ArrayList<>();
    private final List<DjmRingEncoder> sculptEncoders = new ArrayList<>();
    private final List<DjmRingEncoder> vocalEncoders = new ArrayList<>();
    private final List<DjmRingEncoder> melodyEncoders = new ArrayList<>();
    private final List<DjmRingEncoder> baselineEncoders = new ArrayList<>();
    private final List<DjmRingEncoder> drumEncoders = new ArrayList<>();
    private final List<AbsoluteHardwareKnob> aux1Knobs = new ArrayList<>();
    private final List<AbsoluteHardwareKnob> aux2Knobs = new ArrayList<>();
    private final List<AbsoluteHardwareKnob> lpfKnobs = new ArrayList<>();
    private final List<AbsoluteHardwareKnob> hpfKnobs = new ArrayList<>();
    private final int layoutLeftOffset;
    private final VuMeter vuLeft;
    private final VuMeter vuRight;
    private final double topOffset = 10;
    
    // ENCODERS Sculpt configured to SignedBit   Left =  01-07  Right = 41-47
    
    public DjmAHwElements(final ControllerHost host, final HardwareSurface surface,
        final DjmMidiProcessor midiProcessor, final int port) {
        layoutLeftOffset = port * 190;
        final MidiIn midiIn = midiProcessor.getMidiIn(port);
        for (int i = 0; i < 4; i++) {
            final AbsoluteHardwareKnob aux1Knob = surface.createAbsoluteHardwareKnob("DJMA AUX1 %d".formatted(i + 1));
            aux1Knob.setAdjustValueMatcher(midiIn.createAbsoluteCCValueMatcher(0, 20 + i));
            aux1Knobs.add(aux1Knob);
            layoutMainKnob(i, 0, aux1Knob, "AUX1 %d".formatted(i + 1));
            
            final AbsoluteHardwareKnob aux2Knob = surface.createAbsoluteHardwareKnob("DJMA AUX2 %d".formatted(i + 1));
            aux2Knob.setAdjustValueMatcher(midiIn.createAbsoluteCCValueMatcher(0, i + 24));
            aux2Knobs.add(aux2Knob);
            layoutMainKnob(i, 1, aux2Knob, "AUX2 %d".formatted(i + 1));
            
            final AbsoluteHardwareKnob lpfKnob = surface.createAbsoluteHardwareKnob("DJMA LPF %d".formatted(i + 1));
            lpfKnob.setAdjustValueMatcher(midiIn.createAbsoluteCCValueMatcher(0, i + 28));
            lpfKnobs.add(lpfKnob);
            layoutMainKnob(i, 2, lpfKnob, "LPF %d".formatted(i + 1));
            
            final AbsoluteHardwareKnob hpfKnob = surface.createAbsoluteHardwareKnob("DJMA HPF %d".formatted(i + 1));
            hpfKnob.setAdjustValueMatcher(midiIn.createAbsoluteCCValueMatcher(0, i + 32));
            hpfKnobs.add(hpfKnob);
            layoutMainKnob(i, 4, hpfKnob, "HPF %d".formatted(i + 1));
        }
        
        for (int i = 0; i < 16; i++) {
            final RgbButton sideButton =
                new RgbButton(
                    port, 0x14 + i, "DJM %s %d".formatted(BUTTONS[BUTTON_LAYOUT[i]], i + 1), surface,
                    midiProcessor);
            sideButtons.add(sideButton);
            layoutButton(BUTTON_LAYOUT[i], sideButton, BUTTONS[i]);
        }
        for (int i = 0; i < 4; i++) {
            final DjmRingEncoder sculptEncoder =
                new DjmRingEncoder(port, i, "SCULPT %d".formatted(i + 1), surface, midiProcessor);
            sculptEncoders.add(sculptEncoder);
            layoutEncoder(i, 3, sculptEncoder, "SCULPT %d".formatted(i + 1));
            
            final DjmRingEncoder vocalEncoder =
                new DjmRingEncoder(port, i + 4, "VOCAL %d".formatted(i + 1), surface, midiProcessor);
            vocalEncoders.add(vocalEncoder);
            layoutEncoder(i, 5, vocalEncoder, "VOCAL %d".formatted(i + 1));
            
            
            final DjmRingEncoder melodyEncoder =
                new DjmRingEncoder(port, i + 8, "MELODY %d".formatted(i + 1), surface, midiProcessor);
            melodyEncoders.add(melodyEncoder);
            layoutEncoder(i, 6, melodyEncoder, "MELODY %d".formatted(i + 1));
            
            final DjmRingEncoder baseLineEncoder =
                new DjmRingEncoder(port, i + 12, "BASELINE %d".formatted(i + 1), surface, midiProcessor);
            baselineEncoders.add(baseLineEncoder);
            layoutEncoder(i, 7, baseLineEncoder, "BASELINE %d".formatted(i + 1));
            
            final DjmRingEncoder drumEncoder =
                new DjmRingEncoder(port, i + 16, "DRUM %d".formatted(i + 1), surface, midiProcessor);
            drumEncoders.add(drumEncoder);
            layoutEncoder(i, 8, drumEncoder, "DRUM %d".formatted(i + 1));
        }
        vuLeft = new VuMeter(0, port, surface, midiProcessor);
        vuRight = new VuMeter(1, port, surface, midiProcessor);
    }
    
    private void layoutEncoder(final int index, final int row, final RingEncoder encoder, final String name) {
        encoder.setBounds(
            layoutLeftOffset + 10 + (KNOB_SIZE + 2) * index, topOffset + (KNOB_SIZE + VERT_GAP) * row, KNOB_SIZE);
        encoder.setLabel(name);
    }
    
    private void layoutEncoder(final int index, final int row, final DjmRingEncoder encoder, final String name) {
        encoder.setBounds(
            layoutLeftOffset + 10 + (KNOB_SIZE + 2) * index, topOffset + (KNOB_SIZE + VERT_GAP) * row, KNOB_SIZE);
        encoder.setLabel(name);
    }
    
    public List<AbsoluteHardwareKnob> getAux1Knobs() {
        return aux1Knobs;
    }
    
    public List<AbsoluteHardwareKnob> getAux2Knobs() {
        return aux2Knobs;
    }
    
    public List<AbsoluteHardwareKnob> getLpfKnobs() {
        return lpfKnobs;
    }
    
    public List<AbsoluteHardwareKnob> getHpfKnobs() {
        return hpfKnobs;
    }
    
    private void layoutMainKnob(final int index, final int row, final AbsoluteHardwareKnob knob, final String name) {
        knob.setBounds(
            layoutLeftOffset + 10 + (KNOB_SIZE + 2) * index, topOffset + (KNOB_SIZE + VERT_GAP) * row, KNOB_SIZE * 0.9,
            KNOB_SIZE * 0.9);
        knob.setLabel(name);
        knob.setLabelPosition(RelativePosition.BELOW);
    }
    
    private void layoutButton(final int index, final RgbButton button, final String name) {
        final int xIndex = index % 2;
        final int yIndex = index / 2;
        final int width = 10;
        final int height = 30;
        button.setBounds(
            layoutLeftOffset + 120 + (width + 5) * xIndex, topOffset + KNOB_SIZE + (height + 5) * yIndex,
            width * 0.9, height * 0.9);
        button.setLabel(name);
    }
    
    public void refreshHardware() {
        for (final DjmRingEncoder encoder : sculptEncoders) {
            encoder.refresh();
        }
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
    
    public List<DjmRingEncoder> getSculptEncoders() {
        return sculptEncoders;
    }
    
//    public List<DjmRingEncoder> getVocalEncoders() {
//        return vocalEncoders;
//    }
//
//    public List<DjmRingEncoder> getBaselineEncoders() {
//        return baselineEncoders;
//    }
//
//    public List<DjmRingEncoder> getDrumEncoders() {
//        return drumEncoders;
//    }
//
//    public List<DjmRingEncoder> getMelodyEncoders() {
//        return melodyEncoders;
//    }
    
}
