package com.yaeltex.seqarp168mk2;

import com.bitwig.extension.controller.api.ControllerHost;
import com.bitwig.extension.controller.api.HardwareSurface;
import com.bitwig.extensions.framework.di.Component;
import com.yaeltex.common.YaeltexMidiProcessor;
import com.yaeltex.controls.RgbButton;
import com.yaeltex.controls.RingEncoder;

@Component
public class SeqArpHardwareElements {
    
    private static final int[] ENCODER_INDEX = new int[] { //
        0, 1, 2, 3, 16, 17, 18, 19, //
        4, 5, 6, 7, 20, 21, 22, 23, //
        8, 9, 10, 11, 24, 25, 26, 27, //
        12, 13, 14, 15, 28, 29, 30, 31
    };
    
    private static final int[] BUTTON_OFFSET = new int[] {
        2, 0, 3, 1
    };
    
    private final RingEncoder[] encoders = new RingEncoder[32];
    private final RgbButton[] stepButtons = new RgbButton[32];
    private final RgbButton[] controlButtons = new RgbButton[8];
    
    public SeqArpHardwareElements(final ControllerHost host, final HardwareSurface surface,
        final YaeltexMidiProcessor midiProcessor) {
        surface.setPhysicalSize(430, 330);
        for (int index = 0; index < 32; index++) {
            encoders[index] =
                new RingEncoder(ENCODER_INDEX[index], "ENCODER_%d".formatted(index + 1), surface, midiProcessor,
                    RingEncoder.Mode.SIGNED_BIT);
            final int note = BUTTON_OFFSET[index / 8] * 8 + index % 8;
            stepButtons[index] = new RgbButton(40 + note, "STEP", surface, midiProcessor);
        }
        for (int index = 0; index < controlButtons.length; index++) {
            final int note = 32 + index;
            controlButtons[index] = new RgbButton(note, "CTRL", surface, midiProcessor);
        }
    }
    
    public RingEncoder getEncoder(final int index) {
        if (index >= 0 && index < encoders.length) {
            return encoders[index];
        }
        return null;
    }
    
    public RgbButton getStepButton(final int index) {
        if (index >= 0 && index < encoders.length) {
            return stepButtons[index];
        }
        return null;
    }
    
    public RgbButton getControlButton(final int index) {
        if (index >= 0 && index < controlButtons.length) {
            return controlButtons[index];
        }
        return null;
    }
}
