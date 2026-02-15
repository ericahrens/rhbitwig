package com.yaeltex.djm;

import java.util.ArrayList;
import java.util.List;

import com.bitwig.extension.api.Color;
import com.bitwig.extension.api.graphics.GraphicsOutput;
import com.bitwig.extension.controller.api.AbsoluteHardwareKnob;
import com.bitwig.extension.controller.api.HardwareSlider;
import com.bitwig.extension.controller.api.HardwareSurface;
import com.bitwig.extension.controller.api.MidiIn;
import com.bitwig.extension.controller.api.RelativePosition;
import com.yaeltex.common.controls.RgbButton;
import com.yaeltex.common.controls.VuMeter;

public class DjmBHwElements {
    private final static String[] DECKS = {"A", "B", "C", "D"};
    private final static String[] TRACK_SLIDERS = {"MD1", "MD2", "MD3", "FLG", "REV", "DLY", "MEL", "DRM"};
    private final static String[] MASTER_KNOBS = {"MST1", "UN1", "GAIN1", "GAIN2", "MST2", "UN2", "GAIN3", "GAIN4"};
    private final static String[] MAIN_KNOBS =
        {"MON EQ HIGH", "MON EQ LOW", "LEVEL", "CUE1", "CUE2 FLG", "MFLT HPF RES", "MFLT LPF", "MFLT HPF"};
    private final static String[] EFX_NAMES = {"CUE", "FLT", "FLG", "EFX"};
    private final VuMeter vuLeft;
    private final VuMeter vuRight;
    private final List<VuMeter> deckMeters = new ArrayList<>();
    private final List<HardwareSlider> deckSliders = new ArrayList<>();
    private final List<HardwareSlider> trackSliders = new ArrayList<>();
    private final List<VuMeter> trackMeters = new ArrayList<>();
    private final List<RgbButton> masterButtons = new ArrayList<>();
    private final List<RgbButton> trackButtons = new ArrayList<>();
    
    private final List<AbsoluteHardwareKnob> masterGainKnobs = new ArrayList<>();
    private final List<AbsoluteHardwareKnob> largeKnobs = new ArrayList<>();
    
    int layoutLeftOffset = 0;
    int topOffset = 10;
    
    private enum KnobMapping {
        MST1("MST1", 0);
        private final String name;
        private final int midiCc;
        
        KnobMapping(final String name, final int midiCc) {
            this.name = name;
            this.midiCc = midiCc;
        }
    }
    
    public DjmBHwElements(final HardwareSurface surface, final DjmMidiProcessor midiProcessor, final int port) {
        final MidiIn midiIn = midiProcessor.getMidiIn(port);
        layoutLeftOffset = port * 190;
        vuLeft = new VuMeter(0, port, surface, midiProcessor);
        vuRight = new VuMeter(1, port, surface, midiProcessor);
        
        //        final Bitmap meterBitmap = host.createBitmap(20, 100, BitmapFormat.ARGB32);
        //        meterBitmap.render(this::render);
        //        final HardwarePixelDisplay pixelDisplay = surface.createHardwarePixelDisplay("VU1", meterBitmap);
        //        pixelDisplay.setBounds(10, 10, 20, 20);
        
        // Sculpt FQ Button => Frequency 
        
        for (int i = 0; i < 8; i++) {
            deckMeters.add(new VuMeter(2 + i, port, surface, midiProcessor));
            final HardwareSlider slider = surface.createHardwareSlider("DJMB %s".formatted(TRACK_SLIDERS[i]));
            slider.setAdjustValueMatcher(midiIn.createAbsoluteCCValueMatcher(0, 0xC + i));
            deckSliders.add(slider);
            layoutChannelSlider(i, slider);
            
            final int xIndex = i % 4;
            final String id = xIndex == 0 ? "DJMB FLG %d".formatted(xIndex + 1) : "DJMB FLT %d".formatted(xIndex + 1);
            final RgbButton masterButton = new RgbButton(port, i, id, surface, midiProcessor);
            
            masterButtons.add(masterButton);
            layoutTopButton(i, masterButton, 15);
            
            final AbsoluteHardwareKnob masterSectionKnob =
                surface.createAbsoluteHardwareKnob("%s".formatted(MASTER_KNOBS[i]));
            masterSectionKnob.setAdjustValueMatcher(midiIn.createAbsoluteCCValueMatcher(0, i));
            masterGainKnobs.add(masterSectionKnob);
            layoutMasterKnob(i, masterSectionKnob, MASTER_KNOBS[i]);
            
            
            final AbsoluteHardwareKnob mainKnob = surface.createAbsoluteHardwareKnob("%s".formatted(MAIN_KNOBS[i]));
            mainKnob.setAdjustValueMatcher(midiIn.createAbsoluteCCValueMatcher(0, i + 8));
            largeKnobs.add(mainKnob);
            layoutMainKnob(i, mainKnob, MAIN_KNOBS[i]);
            
        }
        for (int i = 0; i < 16; i++) {
            final int rowIndex = i / 8;
            final int channelIndex = i % 8 / 2;
            final int typeIndex = (i % 8) % 2 + rowIndex * 2;
            
            final RgbButton button =
                new RgbButton(
                    port, 0x8 + i, "DJMB %s %d".formatted(EFX_NAMES[typeIndex], channelIndex + 1), surface,
                    midiProcessor);
            trackButtons.add(button);
            layoutTrackButton(i, button, 15, "%s %d".formatted(EFX_NAMES[typeIndex], channelIndex + 1));
        }
        for (int i = 0; i < 4; i++) {
            trackMeters.add(new VuMeter(10 + i, port, surface, midiProcessor));
            final HardwareSlider slider = surface.createHardwareSlider("DJMB DECK %s".formatted(DECKS[i]));
            //slider.setAdjustValueMatcher(midiIn.createAbsoluteCCValueMatcher(0, 0x18 + i));
            slider.setAdjustValueMatcher(midiIn.createAbsolutePitchBendValueMatcher(i));
            trackSliders.add(slider);
            layoutDeckSlider(i, slider);
        }
    }
    
    private void render(final GraphicsOutput graphicsOutput) {
        graphicsOutput.rectangle(0, 0, 20, 20);
        graphicsOutput.setColor(Color.whiteColor());
        graphicsOutput.fill();
    }
    
    public List<HardwareSlider> getTrackSliders() {
        return trackSliders;
    }
    
    public List<RgbButton> getMasterButtons() {
        return masterButtons;
    }
    
    public List<VuMeter> getDeckMeters() {
        return deckMeters;
    }
    
    public List<HardwareSlider> getDeckSliders() {
        return deckSliders;
    }
    
    public List<VuMeter> getTrackMeters() {
        return trackMeters;
    }
    
    public VuMeter getVuLeft() {
        return vuLeft;
    }
    
    public VuMeter getVuRight() {
        return vuRight;
    }
    
    public List<AbsoluteHardwareKnob> getLargeKnobs() {
        return largeKnobs;
    }
    
    public List<AbsoluteHardwareKnob> getMasterGainKnobs() {
        return masterGainKnobs;
    }
    
    public List<RgbButton> getTrackButtons() {
        return trackButtons;
    }
    
    private void layoutMainKnob(final int index, final AbsoluteHardwareKnob knob, final String label) {
        final int size = 23;
        knob.setBounds(layoutLeftOffset + 15, topOffset + 50 + (size + 10) * index, size * 0.9, size * 0.9);
        knob.setLabel(label);
        knob.setLabelPosition(RelativePosition.BELOW);
    }
    
    private void layoutMasterKnob(final int index, final AbsoluteHardwareKnob knob, final String label) {
        final int xIndex = index % 4;
        final int yIndex = index / 4;
        final int size = 15;
        knob.setBounds(layoutLeftOffset + 50 + size * xIndex, topOffset + (size + 5) * yIndex, size * 0.9, size * 0.9);
        knob.setLabel(label);
        knob.setLabelPosition(RelativePosition.BELOW);
    }
    
    private void layoutTopButton(final int index, final RgbButton button, final int size) {
        final int xIndex = index % 4;
        final int yIndex = index / 4;
        button.setBounds(
            layoutLeftOffset + 110 + size * xIndex, topOffset + (size + 5) * yIndex, size * 0.9, size * 0.9);
        button.setLabel(yIndex == 0 ? "FLG %d".formatted(xIndex + 1) : "FLT %d".formatted(xIndex + 1));
    }
    
    private void layoutTrackButton(final int index, final RgbButton button, final int size, final String name) {
        final int xIndex = index % 8;
        final int yIndex = index / 8;
        button.setBounds(
            layoutLeftOffset + 50 + size * xIndex, topOffset + 135 + (size + 5) * yIndex, size * 0.9, size * 0.9);
        button.setLabel(name);
    }
    
    private void layoutDeckSlider(final int index, final HardwareSlider slider) {
        slider.setBounds(layoutLeftOffset + 50 + 30 * index, topOffset + 180, 30 * 0.9, 120);
        slider.setLabel("Deck %s".formatted(DECKS[index]));
        slider.setLabelPosition(RelativePosition.BELOW);
    }
    
    private void layoutChannelSlider(final int index, final HardwareSlider slider) {
        slider.setBounds(layoutLeftOffset + 50 + 15 * index, 50, 15 * 0.9, 60);
        slider.setLabel(TRACK_SLIDERS[index]);
        slider.setLabelPosition(RelativePosition.BELOW);
    }
    
}
