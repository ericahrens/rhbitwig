package com.allenheath.k2.set1;

import com.bitwig.extension.controller.api.HardwareSurface;
import com.bitwig.extension.controller.api.MidiIn;
import com.bitwig.extension.controller.api.MidiOut;

public class ButtonManager {
    private final HardwareSurface surface;
    private final MidiIn midiIn;
    private final MidiOut midiOut;

    public ButtonManager(final HardwareSurface surface, final MidiIn midiIn, final MidiOut midiOut) {
        this.surface = surface;
        this.midiIn = midiIn;
        this.midiOut = midiOut;
    }

    public StateButton createStateButton(final String id, final int noteValue, final int channel) {
        return new StateButton(id, noteValue, channel, surface, midiIn, midiOut);
    }

    public StateButton createDirectLedStateButton(final String id, final int noteValue, final int channel) {
        return new StateButton(id, noteValue, channel, surface, midiIn, midiOut, false);
    }

    public StateButton createDirectLedStateButton(final String id, final int inputNoteValue, final int ledNoteValue,
            final int channel) {
        return new StateButton(id, inputNoteValue, ledNoteValue, channel, surface, midiIn, midiOut, false);
    }
}
