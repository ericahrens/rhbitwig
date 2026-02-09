package com.yaeltex.djcontroller;

import com.bitwig.extension.controller.api.AbsoluteHardwareKnob;
import com.bitwig.extension.controller.api.ControllerHost;
import com.bitwig.extension.controller.api.HardwareSlider;
import com.bitwig.extension.controller.api.HardwareSurface;
import com.bitwig.extension.controller.api.MidiIn;
import com.bitwig.extension.controller.api.MultiStateHardwareLight;
import com.bitwig.extensions.framework.di.Component;
import com.yaeltex.common.YaeltexButtonLedState;
import com.yaeltex.common.YaeltexMidiProcessor;
import com.yaeltex.common.controls.RgbButton;
import com.yaeltex.common.controls.RingEncoder;

@Component
public class HardwareElements {
    
    public static final double ENCODER_STEP_SIZE = 0.0075;
    public static final int LEFT_OFFSET = 10;
    public static final int C2_OFFSET = 150;
    private static final String[] MAIN_LABELS = {"COPY", "MUTE/SOLO", "%", "LASTSTEP", "PAT1", "PAT2", "PAT3", "PAT4"};
    
    private final RingEncoder[] bottomEncoders1 = new RingEncoder[4];
    private final RingEncoder[] bottomEncoders2 = new RingEncoder[4];
    private final RingEncoder[] topRingEncoders2 = new RingEncoder[4];
    private final HardwareSlider[] sliders1 = new HardwareSlider[8];
    private final HardwareSlider[] sliders2 = new HardwareSlider[8];
    private final RgbButton[] stepButtons1 = new RgbButton[16];
    private final RgbButton[] stepButtons2 = new RgbButton[16];
    private final RgbButton[] selectButtons1 = new RgbButton[16];
    private final RgbButton[] selectButtons2 = new RgbButton[16];
    private final RgbButton[] mainButtons1 = new RgbButton[8];
    private final RgbButton[] mainButtons2 = new RgbButton[8];
    private final AbsoluteHardwareKnob[] smallKnobs1 = new AbsoluteHardwareKnob[16];
    private final AbsoluteHardwareKnob[] smallKnobs2 = new AbsoluteHardwareKnob[16];
    private final AbsoluteHardwareKnob[] largeKnobs1 = new AbsoluteHardwareKnob[12];
    private final AbsoluteHardwareKnob[] largeKnobs2 = new AbsoluteHardwareKnob[12];
    
    public HardwareElements(final ControllerHost host, final HardwareSurface surface,
        final YaeltexMidiProcessor midiProcessor) {
        surface.setPhysicalSize(300, 400);
        
        for (int i = 0; i < 4; i++) {
            topRingEncoders2[i] =
                new RingEncoder(
                    0, i, 0, "TENC_1_%d".formatted(i + 1), surface, midiProcessor, RingEncoder.Mode.SIGNED_BIT,
                    ENCODER_STEP_SIZE);
            bottomEncoders1[i] = new RingEncoder(
                0, 0x4 + i, 0, "ENC_1_%d".formatted(i + 1), surface, midiProcessor, RingEncoder.Mode.SIGNED_BIT,
                ENCODER_STEP_SIZE);
            bottomEncoders2[i] = new RingEncoder(
                0, 0x4 + i, 1, "ENC_2_%d".formatted(i + 1), surface, midiProcessor, RingEncoder.Mode.SIGNED_BIT,
                ENCODER_STEP_SIZE);
            bottomEncoders1[i].setBounds(LEFT_OFFSET + i * 34.0, 320, 28);
            bottomEncoders1[i].getButton().setLabel(" ");
            bottomEncoders2[i].setBounds(C2_OFFSET + i * 34.0, 320, 28);
            bottomEncoders1[i].getButton().setLabel(" ");
            topRingEncoders2[i].setBounds(C2_OFFSET + i * 34.0, 350, 28);
            topRingEncoders2[i].getButton().setLabel(" ");
        }
        
        for (int i = 0; i < 8; i++) {
            sliders1[i] = createSliderCc("SILDER_DR_%d".formatted(i + 1), surface, midiProcessor.getMidiIn(0), 8 + i);
            sliders2[i] = createSliderCc("SILDER_SYN_%d".formatted(i + 1), surface, midiProcessor.getMidiIn(1), 8 + i);
            mainButtons1[i] = new RgbButton(0, 0, 40 + i, "MAIN_BUTTON_1_%d".formatted(i + 1), surface, midiProcessor);
            mainButtons2[i] = new RgbButton(1, 0, 40 + i, "MAIN_BUTTON_2_%d".formatted(i + 1), surface, midiProcessor);
            sliders1[i].setBounds(LEFT_OFFSET + i * 15.0, 160, 11, 60);
            sliders2[i].setBounds(C2_OFFSET + i * 15.0, 160, 11, 60);
            final int row = i / 4;
            final int col = i % 4;
            mainButtons1[i].setBounds(LEFT_OFFSET + col * 34.0, row * 17 + 270, 28, 11);
            mainButtons1[i].setLabel(MAIN_LABELS[i]);
            mainButtons2[i].setBounds(C2_OFFSET + col * 30.0, row * 17 + 270, 28, 11);
            mainButtons2[i].setLabel(" ");
        }
        for (int i = 0; i < 16; i++) {
            final int row = i / 8;
            final int col = i % 8;
            stepButtons1[i] =
                new RgbButton(0, 0, 8 + i, "PATTERN_BUTTON_1_%d".formatted(i + 1), surface, midiProcessor);
            stepButtons2[i] =
                new RgbButton(1, 0, 8 + i, "PATTERN_BUTTON_2_%d".formatted(i + 1), surface, midiProcessor);
            selectButtons1[i] = new RgbButton(0, 0, 24 + i, "SEL_BUTTON_1_%d".formatted(i + 1), surface, midiProcessor);
            selectButtons2[i] = new RgbButton(1, 0, 24 + i, "SEL_BUTTON_2_%d".formatted(i + 1), surface, midiProcessor);
            
            stepButtons1[i].setBounds(LEFT_OFFSET + col * 15.0, row * 15 + 130, 11, 11);
            stepButtons1[i].setLabel(" ");
            stepButtons2[i].setBounds(C2_OFFSET + col * 15.0, row * 15 + 130, 11, 11);
            stepButtons2[i].setLabel(" ");
            
            selectButtons1[i].setBounds(LEFT_OFFSET + col * 15.0, row * 15 + 240, 11, 11);
            selectButtons1[i].setLabel(" ");
            selectButtons2[i].setBounds(C2_OFFSET + col * 15.0, row * 15 + 240, 11, 11);
            selectButtons2[i].setLabel(" ");
            
            smallKnobs1[i] = createKnob("SMALL_ENC_1_%d".formatted(i + 1), surface, midiProcessor.getMidiIn(0), 16 + i);
            smallKnobs2[i] = createKnob("SMALL_ENC_2_%d".formatted(i + 1), surface, midiProcessor.getMidiIn(1), 16 + i);
            smallKnobs1[i].setBounds(LEFT_OFFSET + col * 15.0, (1 - row) * 15 + 100, 11, 11);
            smallKnobs2[i].setBounds(C2_OFFSET + col * 15.0, (1 - row) * 15 + 100, 11, 11);
        }
        for (int i = 0; i < 12; i++) {
            final int ccNr = (2 - i / 4) * 4 + i % 4 + 32;
            largeKnobs1[i] = createKnob("LARGE_ENC_1_%d".formatted(i + 1), surface, midiProcessor.getMidiIn(0), ccNr);
            largeKnobs2[i] = createKnob("LARGE_ENC_2_%d".formatted(i + 1), surface, midiProcessor.getMidiIn(1), ccNr);
            final int row = i / 4;
            final int col = i % 4;
            largeKnobs1[i].setBounds(LEFT_OFFSET + col * 30.0, row * 30 + 10, 22, 22);
            largeKnobs2[i].setBounds(C2_OFFSET + col * 30.0, row * 30 + 10, 22, 22);
        }
        
    }
    
    public HardwareSlider[] getSliders1() {
        return sliders1;
    }
    
    public HardwareSlider[] getSliders2() {
        return sliders2;
    }
    
    public AbsoluteHardwareKnob[] getLargeKnobs1() {
        return largeKnobs1;
    }
    
    public AbsoluteHardwareKnob[] getLargeKnobs2() {
        return largeKnobs2;
    }
    
    public RingEncoder[] getBottomEncoders1() {
        return bottomEncoders1;
    }
    
    public RingEncoder[] getBottomEncoders2() {
        return bottomEncoders2;
    }
    
    public AbsoluteHardwareKnob[] getSmallKnobs1() {
        return smallKnobs1;
    }
    
    public AbsoluteHardwareKnob[] getSmallKnobs2() {
        return smallKnobs2;
    }
    
    public RgbButton[] getStepButtons1() {
        return stepButtons1;
    }
    
    public RgbButton[] getStepButtons2() {
        return stepButtons2;
    }
    
    public RgbButton[] getMainButtons1() {
        return mainButtons1;
    }
    
    public RgbButton[] getSelectButtons1() {
        return selectButtons1;
    }
    
    public RingEncoder[] getTopRingEncoders2() {
        return topRingEncoders2;
    }
    
    private HardwareSlider createSliderCc(final String name, final HardwareSurface surface, final MidiIn midiIn,
        final int ccNr) {
        final HardwareSlider fader = surface.createHardwareSlider(name);
        fader.setAdjustValueMatcher(midiIn.createAbsoluteCCValueMatcher(0, ccNr));
        return fader;
    }
    
    private AbsoluteHardwareKnob createKnob(final String name, final HardwareSurface surface, final MidiIn midiIn,
        final int ccNr) {
        final AbsoluteHardwareKnob knob = surface.createAbsoluteHardwareKnob(name);
        knob.setAdjustValueMatcher(midiIn.createAbsoluteCCValueMatcher(0, ccNr));
        final MultiStateHardwareLight knobLight = surface.createMultiStateHardwareLight(name + "_LIGHT");
        knob.setBackgroundLight(knobLight);
        knobLight.state().setValue(YaeltexButtonLedState.of(60));
        return knob;
    }
}
