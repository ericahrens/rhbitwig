package com.yaeltex.fuse;

import java.util.ArrayList;
import java.util.List;

import com.bitwig.extension.controller.api.AbsoluteHardwareKnob;
import com.bitwig.extension.controller.api.ControllerHost;
import com.bitwig.extension.controller.api.HardwareSlider;
import com.bitwig.extension.controller.api.HardwareSurface;
import com.bitwig.extension.controller.api.MidiIn;
import com.bitwig.extension.controller.api.MultiStateHardwareLight;
import com.bitwig.extension.controller.api.RelativePosition;
import com.bitwig.extensions.framework.di.Component;
import com.yaeltex.common.YaeltexButtonLedState;
import com.yaeltex.common.YaeltexMidiProcessor;
import com.yaeltex.controls.RgbButton;
import com.yaeltex.controls.RingEncoder;

@Component
public class HwElements {
    
    private static final double MID_X_OFF = 200;
    private static final StripControlOffsets[] midiMappings = {
        new StripControlOffsets(0x14, 0x1C),
        new StripControlOffsets(0x16, 0x1E),
        new StripControlOffsets(0x18, 0x20),
        new StripControlOffsets(0x1A, 0x22),
        new StripControlOffsets(0x24, 0x28),
        new StripControlOffsets(0x26, 0x2A),
    };
    private static final SynthControlOffset[] controlCc = {
        new SynthControlOffset(0xC, 0x14, 0xC),
        new SynthControlOffset(0x10, 0x18, 0x10),
        new SynthControlOffset(0x20, 0x1C),
        new SynthControlOffset(0x24, 0x28),
        new SynthControlOffset(0x34, 0x38),
        new SynthControlOffset(0x3C, 0x40),
    };
    
    private final static String[] ADSR_LABEL = {"A", "D", "S", "R"};
    private final HardwareSlider masterSlider;
    private final HardwareSlider crossFader;
    private final List<StripControl> stripControls = new ArrayList<>();
    private final SynthControl1 synthControl1;
    private final SynthControl1 synthControl2;
    private final SynthControl2 synthControl3;
    private final SynthControl2 synthControl4;
    private final SynthControl2 synthControl5;
    private final SynthControl2 synthControl6;
    private final RgbButton[] masterButtons = new RgbButton[8];
    private final RgbButton[] fxButtons = new RgbButton[8];
    private final AbsoluteHardwareKnob[] masterControls = new AbsoluteHardwareKnob[4];
    private final AbsoluteHardwareKnob[] fxControls = new AbsoluteHardwareKnob[4];
    private final RingEncoder[] encoders = new RingEncoder[4];
    
    private record StripControlOffsets(int abOffset, int fxOffset) {
    }
    
    private record SynthControlOffset(int faderCc, int knobOffset, int buttonOffset) {
        public SynthControlOffset(final int faderCc, final int knobOffset) {
            this(faderCc, knobOffset, -1);
        }
    }
    
    public HwElements(final ControllerHost host, final HardwareSurface surface,
        final YaeltexMidiProcessor midiProcessor) {
        surface.setPhysicalSize(430, 330);
        masterSlider = createSliderPitchBend("MASTER_FILTER", surface, midiProcessor, 6);
        crossFader = createSliderPitchBend("CROSS_FADER", surface, midiProcessor, 7);
        //crossFader.value().addValueObserver(v -> FuseExtension.println(" > %f", v));
        for (int i = 0; i < 6; i++) {
            stripControls.add(createStripControl(i, surface, midiProcessor));
        }
        synthControl1 = createSynthControl1(0, surface, midiProcessor);
        synthControl2 = createSynthControl1(1, surface, midiProcessor);
        synthControl3 = createSynthControl2(0, surface, midiProcessor);
        synthControl4 = createSynthControl2(1, surface, midiProcessor);
        synthControl5 = createSynthControl2(2, surface, midiProcessor);
        synthControl6 = createSynthControl2(3, surface, midiProcessor);
        
        for (int i = 0; i < 4; i++) {
            masterControls[i] = createKnob("MASTER_KNOB_%d".formatted(i + 1), surface, midiProcessor, 0, 0x8 + i);
            fxControls[i] = createKnob("FX_KNOB_%d".formatted(i + 1), surface, midiProcessor, 0, 0x4 + i);
        }
        
        for (int i = 0; i < 8; i++) {
            masterButtons[i] = new RgbButton(0x2C + i, "MASTER_BUTTON_%d".formatted(i + 1), surface, midiProcessor);
            fxButtons[i] = new RgbButton(0x04 + i, "FX_BUTTON_%d".formatted(i + 1), surface, midiProcessor);
        }
        
        for (int i = 0; i < encoders.length; i++) {
            encoders[i] = new RingEncoder(0, i, "ENCODER_%d".formatted(i + 1), surface, midiProcessor);
        }
        
        layoutControl1();
    }
    
    private HardwareSlider createSliderPitchBend(final String name, final HardwareSurface surface,
        final YaeltexMidiProcessor midiProcessor, final int channel) {
        final HardwareSlider fader = surface.createHardwareSlider(name);
        final MidiIn midiIn = midiProcessor.getMidiIn();
        fader.setAdjustValueMatcher(midiIn.createAbsolutePitchBendValueMatcher(channel));
        return fader;
    }
    
    private StripControl createStripControl(final int index, final HardwareSurface surface,
        final YaeltexMidiProcessor midiProcessor) {
        final StripControlOffsets config = midiMappings[index];
        final HardwareSlider fader =
            createSliderPitchBend("MIX_FADER_%d".formatted(index + 1), surface, midiProcessor, index);
        final RgbButton abButton =
            new RgbButton(config.abOffset(), "AB_%d".formatted(index + 1), surface, midiProcessor);
        final RgbButton cueButton =
            new RgbButton(config.abOffset() + 1, "CUE_%d".formatted(index + 1), surface, midiProcessor);
        final RgbButton fxButton =
            new RgbButton(config.fxOffset(), "FX_%d".formatted(index + 1), surface, midiProcessor);
        final RgbButton muteButton =
            new RgbButton(config.fxOffset() + 1, "MUTE_%d".formatted(index + 1), surface, midiProcessor);
        final int knobOffset = 0x50 + index * 4;
        final List<AbsoluteHardwareKnob> fxKnobs = new ArrayList<>();
        for (int i = 0; i < 4; i++) {
            fxKnobs.add(
                createKnob("CH_%d_FX%d".formatted(index + 1, i + 1), surface, midiProcessor, 0, knobOffset + i));
        }
        final AbsoluteHardwareKnob plusKnob =
            createKnob("CH_%d_PLUS".formatted(index + 1), surface, midiProcessor, 0, 0x44 + index * 2);
        final AbsoluteHardwareKnob multKnob =
            createKnob("CH_%d_MULT".formatted(index + 1), surface, midiProcessor, 0, 0x45 + index * 2);
        final StripControl control =
            new StripControl(index, fader, abButton, cueButton, fxButton, muteButton, fxKnobs, plusKnob, multKnob);
        control.applySurface();
        return control;
    }
    
    private SynthControl1 createSynthControl1(final int index, final HardwareSurface surface,
        final YaeltexMidiProcessor midiProcessor) {
        final SynthControlOffset config = controlCc[index];
        final HardwareSlider cutoff =
            createSliderCc("SC_CUTOFF_%d".formatted(index + 1), surface, midiProcessor, 0, config.faderCc());
        final HardwareSlider resonance =
            createSliderCc("SC_RESONANCE_%d".formatted(index + 1), surface, midiProcessor, 0, config.faderCc() + 1);
        final HardwareSlider modulation =
            createSliderCc("SC_MOD_%d".formatted(index + 1), surface, midiProcessor, 0, config.faderCc() + 2);
        final HardwareSlider amount =
            createSliderCc("SC_AMT_%d".formatted(index + 1), surface, midiProcessor, 0, config.faderCc() + 3);
        final AbsoluteHardwareKnob[] adsrs = new AbsoluteHardwareKnob[4];
        for (int i = 0; i < 4; i++) {
            adsrs[i] = createKnob("SC_%s_%d".formatted(ADSR_LABEL[i], index + 1), surface, midiProcessor, 0,
                config.knobOffset() + i);
        }
        final RgbButton[] buttons = new RgbButton[4];
        for (int i = 0; i < 4; i++) {
            buttons[i] =
                new RgbButton(config.buttonOffset() + i, "SC_BUTTON_%d_%d".formatted(i + 1, index + 1), surface,
                    midiProcessor);
        }
        
        return new SynthControl1(index, cutoff, resonance, modulation, amount, adsrs, buttons);
    }
    
    private SynthControl2 createSynthControl2(final int index, final HardwareSurface surface,
        final YaeltexMidiProcessor midiProcessor) {
        final SynthControlOffset config = controlCc[index + 2];
        final AbsoluteHardwareKnob modulation =
            createKnob("SC2_MOD_%d".formatted(index + 1), surface, midiProcessor, 0, config.faderCc());
        final AbsoluteHardwareKnob amount =
            createKnob("SC2_AMT_%d".formatted(index + 1), surface, midiProcessor, 0, config.faderCc() + 1);
        final AbsoluteHardwareKnob cutoff =
            createKnob("SC2_CUTOFF_%d".formatted(index + 1), surface, midiProcessor, 0, config.faderCc() + 2);
        final AbsoluteHardwareKnob resonance =
            createKnob("SC2_RESONANCE_%d".formatted(index + 1), surface, midiProcessor, 0, config.faderCc() + 3);
        final AbsoluteHardwareKnob[] adsrs = new AbsoluteHardwareKnob[4];
        for (int i = 0; i < 4; i++) {
            adsrs[i] = createKnob("SC2_%s_%d".formatted(ADSR_LABEL[i], index + 1), surface, midiProcessor, 0,
                config.knobOffset() + i);
        }
        
        return new SynthControl2(index, cutoff, resonance, modulation, amount, adsrs);
    }
    
    private AbsoluteHardwareKnob createKnob(final String name, final HardwareSurface surface,
        final YaeltexMidiProcessor midiProcessor, final int channel, final int ccNr) {
        final AbsoluteHardwareKnob knob = surface.createAbsoluteHardwareKnob(name);
        final MidiIn midiIn = midiProcessor.getMidiIn();
        knob.setAdjustValueMatcher(midiIn.createAbsoluteCCValueMatcher(channel, ccNr));
        final MultiStateHardwareLight knobLight = surface.createMultiStateHardwareLight(name + "_LIGHT");
        knob.setBackgroundLight(knobLight);
        knobLight.state().setValue(YaeltexButtonLedState.of(60));
        return knob;
    }
    
    private void layoutControl1() {
        masterSlider.setBounds(300, 255, 120, 25);
        masterSlider.setIsHorizontal(true);
        crossFader.setBounds(300, 290, 120, 25);
        crossFader.setIsHorizontal(true);
        double leftOff = MID_X_OFF;
        double topOff = 40;
        final double sliderWidth = 13;
        final double buttonSize = 10;
        final double knobSize = 15;
        layoutSliders(synthControl1, leftOff, topOff, sliderWidth);
        layoutSliders(synthControl2, leftOff + 130, topOff, sliderWidth);
        for (int i = 0; i < 4; i++) {
            layoutControl1(synthControl1, leftOff + 50, topOff, buttonSize, knobSize, i);
            layoutControl1(synthControl2, leftOff + 95, topOff, buttonSize, knobSize, i);
        }
        topOff = 130;
        final double lgKnobSize = 20;
        final double spacing = 30;
        layoutSynth2Adsr(synthControl3, leftOff, topOff, knobSize);
        layoutSynthLgKnobs(synthControl3, leftOff + 90, topOff, lgKnobSize);
        
        layoutSynth2Adsr(synthControl4, leftOff + 100, topOff + spacing, knobSize);
        layoutSynthLgKnobs(synthControl4, leftOff, topOff + spacing, lgKnobSize);
        
        layoutSynth2Adsr(synthControl5, leftOff + 100, topOff + spacing * 2, knobSize);
        layoutSynthLgKnobs(synthControl5, leftOff, topOff + spacing * 2, lgKnobSize);
        
        layoutSynth2Adsr(synthControl6, leftOff, topOff + spacing * 3, knobSize);
        layoutSynthLgKnobs(synthControl6, leftOff + 90, topOff + spacing * 3, lgKnobSize);
        
        topOff = 5;
        leftOff = MID_X_OFF - 80;
        final double encoderSize = 24;
        for (int i = 0; i < 4; i++) {
            fxControls[i].setBounds(leftOff + knobSize * 1.3 * i, topOff, knobSize, knobSize);
            masterControls[i].setBounds(MID_X_OFF + knobSize * 1.3 * i, 290, knobSize, knobSize);
            encoders[i].setBounds(MID_X_OFF + encoderSize * i * 1.2, topOff, encoderSize);
        }
        for (int i = 0; i < 8; i++) {
            fxButtons[i].setBounds(
                350 + (i % 4) * buttonSize * 1.2, topOff + (i / 4) * buttonSize * 1.2, buttonSize, buttonSize);
            fxButtons[i].setLabel("%s".formatted(i + 1));
            masterButtons[i].setBounds(
                MID_X_OFF + (i % 4) * buttonSize * 1.2, 255 + (i / 4) * buttonSize * 1.2, buttonSize, buttonSize);
            masterButtons[i].setLabel("%s".formatted(i + 1));
        }
    }
    
    private HardwareSlider createSliderCc(final String name, final HardwareSurface surface,
        final YaeltexMidiProcessor midiProcessor, final int channel, final int ccNr) {
        final HardwareSlider fader = surface.createHardwareSlider(name);
        final MidiIn midiIn = midiProcessor.getMidiIn();
        fader.setAdjustValueMatcher(midiIn.createAbsoluteCCValueMatcher(channel, ccNr));
        return fader;
    }
    
    private void layoutSliders(final SynthControl1 control, final double leftOff, final double topOff,
        final double sliderWidth) {
        control.cutoff().setBounds(leftOff, topOff, sliderWidth * 0.9, 70);
        control.cutoff().setLabel("Cut");
        control.cutoff().setLabelPosition(RelativePosition.BELOW);
        control.resonance().setBounds(leftOff + sliderWidth, topOff, sliderWidth * 0.9, 70);
        control.resonance().setLabel("Res");
        control.resonance().setLabelPosition(RelativePosition.BELOW);
        control.mod().setBounds(leftOff + sliderWidth * 2, topOff, sliderWidth * 0.9, 70);
        control.mod().setLabel("MOD");
        control.mod().setLabelPosition(RelativePosition.BELOW);
        control.amount().setBounds(leftOff + sliderWidth * 3, topOff, sliderWidth * 0.9, 70);
        control.amount().setLabel("AMT");
        control.amount().setLabelPosition(RelativePosition.BELOW);
    }
    
    private void layoutControl1(final SynthControl1 control, final double leftOff, final double topOff,
        final double buttonSize, final double knobSize, final int index) {
        final AbsoluteHardwareKnob knob = control.adsrKnobs()[index];
        knob.setBounds(leftOff + (index % 2) * knobSize * 1.2, topOff + (index / 2) * knobSize * 1.2 + 40,
            knobSize * 0.9, knobSize * 0.9);
        knob.setLabelPosition(RelativePosition.BELOW);
        knob.setLabel("%s".formatted(ADSR_LABEL[index]));
        final RgbButton button = control.buttons()[index];
        button.setBounds(leftOff + (index % 2) * knobSize * 1.2, topOff + (index / 2) * knobSize * 1.2, buttonSize,
            buttonSize);
        button.setLabel("%d".formatted(index + 1));
    }
    
    private void layoutSynth2Adsr(final SynthControl2 control, final double leftOff, final double topOff,
        final double knobSize) {
        for (int i = 0; i < 4; i++) {
            final AbsoluteHardwareKnob knob = control.adsrKnobs()[i];
            knob.setBounds(leftOff + i * knobSize * 1.2, topOff, knobSize, knobSize);
            knob.setLabel("%s".formatted(ADSR_LABEL[i]));
            knob.setLabelPosition(RelativePosition.BELOW);
        }
    }
    
    private void layoutSynthLgKnobs(final SynthControl2 control, final double lgLeftOff, final double topOff,
        final double lgKnobSize) {
        final double wfactor = 0.9;
        control.mod().setBounds(lgLeftOff, topOff, lgKnobSize * wfactor, lgKnobSize * wfactor);
        control.mod().setLabel("MOD");
        control.mod().setLabelPosition(RelativePosition.BELOW);
        control.amount().setBounds(lgLeftOff + lgKnobSize, topOff, lgKnobSize * wfactor, lgKnobSize * wfactor);
        control.amount().setLabel("AMT");
        control.amount().setLabelPosition(RelativePosition.BELOW);
        control.cutoff().setBounds(lgLeftOff + lgKnobSize * 2, topOff, lgKnobSize * wfactor, lgKnobSize * wfactor);
        control.cutoff().setLabel("Cut");
        control.cutoff().setLabelPosition(RelativePosition.BELOW);
        control.resonance().setBounds(lgLeftOff + lgKnobSize * 3, topOff, lgKnobSize * wfactor, lgKnobSize * wfactor);
        control.resonance().setLabel("Res");
        control.resonance().setLabelPosition(RelativePosition.BELOW);
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
    
    public SynthControl1 getSynthControl1() {
        return synthControl1;
    }
    
    public SynthControl1 getSynthControl2() {
        return synthControl2;
    }
    
    public SynthControl2 getSynthControl3() {
        return synthControl3;
    }
    
    public SynthControl2 getSynthControl4() {
        return synthControl4;
    }
    
    public SynthControl2 getSynthControl5() {
        return synthControl5;
    }
    
    public SynthControl2 getSynthControl6() {
        return synthControl6;
    }
    
    public RingEncoder[] getEncoders() {
        return encoders;
    }
    
    public AbsoluteHardwareKnob[] getMasterControls() {
        return masterControls;
    }
    
    public AbsoluteHardwareKnob[] getFxControls() {
        return fxControls;
    }
    
    public RgbButton getFxMainButton(final int index) {
        if (index >= 0 && index < fxButtons.length) {
            return fxButtons[index];
        }
        return fxButtons[0];
    }
    
    public RgbButton getMasterButton(final int index) {
        if (index >= 0 && index < masterButtons.length) {
            return masterButtons[index];
        }
        return masterButtons[0];
    }
}
