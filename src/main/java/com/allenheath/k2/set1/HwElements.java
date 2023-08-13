package com.allenheath.k2.set1;

import com.bitwig.extension.controller.api.*;
import com.bitwig.extensions.rh.Midi;

public class HwElements {

    private final MultiStateHardwareLight[] channelLight = new MultiStateHardwareLight[8];
    private final StateButton[][] gridButtons = new StateButton[4][8];
    private final MidiOut midiOut;

    public HwElements(HardwareSurface surface, ControllerHost host, MidiIn midiIn, MidiOut midiOut) {
        this.midiOut = midiOut;
        for (int i = 0; i < 8; i++) {
            final int index = i;
            final int channel = 0xD + i / 4;
            channelLight[i] = surface.createMultiStateHardwareLight("CHANEL_STATE_" + i);
            channelLight[i].state() //
                    .onUpdateHardware(state -> updateButtonLed(state, channel, 0x34 + (index % 4)));
        }
        for (int row = 0; row < 2; row++) {
            for (int col = 0; col < 8; col++) {
                int off = col % 4;
                int y = (1 - row) * 4;
                gridButtons[row][col] = new StateButton("GRID_%d_%d".formatted(row, col), 0x1C + y + off,
                        0xD + (col / 4), surface, midiIn, midiOut);
            }
        }
    }

    private void updateButtonLed(InternalHardwareLightState state, int channel, int noteValue) {
        if (state instanceof RedGreenButtonState rgbState) {
            final RedGreenColor color = rgbState.getColor();
            if (color == null || color == RedGreenColor.OFF) {
                midiOut.sendMidi(Midi.NOTE_ON + channel, noteValue, 0);
            } else {
                midiOut.sendMidi(Midi.NOTE_ON + channel, noteValue + color.getOffset(), 127);
            }
        } else {
            midiOut.sendMidi(Midi.NOTE_ON + channel, noteValue, 0);
        }
    }

    public MultiStateHardwareLight getChannelLight(int index) {
        return channelLight[index];
    }

    public StateButton getStateButton(int row, int col) {
        return gridButtons[row][col];
    }
}
