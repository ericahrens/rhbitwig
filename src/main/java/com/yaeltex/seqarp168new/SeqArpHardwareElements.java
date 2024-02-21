package com.yaeltex.seqarp168new;

import com.bitwig.extension.controller.api.ControllerHost;
import com.bitwig.extension.controller.api.HardwareSurface;
import com.bitwig.extensions.framework.di.Component;
import com.yaeltex.common.YaeltexMidiProcessor;
import com.yaeltex.controls.RingEncoder;

@Component
public class SeqArpHardwareElements {
    
    private static final int[] ENCODER_INDEX = new int[] { //
        0, 1, 2, 3, 16, 17, 18, 19, //
        4, 5, 6, 7, 20, 21, 22, 23, //
        8, 9, 10, 11, 24, 25, 26, 27, //
        12, 13, 14, 15, 28, 29, 30, 31
    };
    
    private final RingEncoder[] encoders = new RingEncoder[32];
    
    public SeqArpHardwareElements(final ControllerHost host, final HardwareSurface surface,
        final YaeltexMidiProcessor midiProcessor) {
        surface.setPhysicalSize(430, 330);
        for (int index = 0; index < 32; index++) {
            encoders[index] =
                new RingEncoder(0, ENCODER_INDEX[index], "ENCODER_%d".formatted(index + 1), surface, midiProcessor);
        }
    }
}
