package com.allenheath.k2.set1;

import com.bitwig.extension.api.Color;
import com.bitwig.extension.controller.api.*;
import com.bitwig.extensions.framework.values.Midi;

public class HwElements {

    private final MultiStateHardwareLight[] channelLight = new MultiStateHardwareLight[8];
    private final StateButton[][] gridButtons = new StateButton[4][8];
    private final HardwareButton[] captureKeysFromDeckButton = new HardwareButton[4];
    private final HardwareSlider[] mainSliders = new HardwareSlider[8];
    private final MidiOut midiOut;

    public HwElements(HardwareSurface surface, ControllerHost host, MidiIn midiIn, MidiOut midiOut) {
        this.midiOut = midiOut;
        for (int i = 0; i < 8; i++) {
            final int index = i;
            final int channel = 0xD + i / 4;

            mainSliders[i] = surface.createHardwareSlider("SLIDER_" + i);
            mainSliders[i].setAdjustValueMatcher(midiIn.createAbsoluteCCValueMatcher(channel,0x10 + (index % 4)));
            mainSliders[i].setBounds(5 + i * 10 + (i / 4) * 5, 50, 8, 40);
            mainSliders[i].setLabel(String.valueOf(i + 1));

            
            channelLight[i] = surface.createMultiStateHardwareLight("CHANEL_STATE_" + i);
            channelLight[i].state() //
                    .onUpdateHardware(state -> updateButtonLed(state, channel, 0x34 + (index % 4)));
            placeLight(i,channelLight[i]);
        }
        for (int row = 0; row < 2; row++) {
            for (int col = 0; col < 8; col++) {
                int off = col % 4;
                int y = (1 - row) * 4;
                final StateButton button = new StateButton("GRID_%d_%d".formatted(row, col),
                    0x1C + y + off,
                    0xD + (col / 4),
                    surface,
                    midiIn,
                    midiOut);
                gridButtons[row][col] = button;
                button.getHwButton().setBounds(5+col*10+(col/4)*5, 25+row*10, 8, 7);
                button.getHwButton().setLabel(String.valueOf(row*8+col+1));
                button.getLight().setBounds(5+col*10+(col/4)*5, 25+row*10, 8, 7);
                button.getLight().setColorToStateFunction(RedGreenButtonState::toState);
            }
        }
        for (int i = 0; i < captureKeysFromDeckButton.length; i++) {
            HardwareButton button = surface.createHardwareButton("CAPTURE_" + i);
            captureKeysFromDeckButton[i] = button;
            button.isPressed().markInterested();
            int channel = 0xD + (i / 2);
            int noteValue = 0x34 + (i % 2) * 2;
            button.pressedAction()
                    .setPressureActionMatcher(midiIn.createNoteOnVelocityValueMatcher(channel, noteValue));
            button.releasedAction().setActionMatcher(midiIn.createNoteOffActionMatcher(channel, noteValue));
            button.setBounds(5+i*20+(i/2)*5, 15, 8, 7);
            button.setLabel("Deck " + (char)('A'+i));
        }
    }
    
    private void placeLight(final int index, final MultiStateHardwareLight multiStateHardwareLight) {
        multiStateHardwareLight.setBounds(5 + index*10+(index/4)*5, 5, 8, 7);
        multiStateHardwareLight.setLabel("");
        multiStateHardwareLight.setLabelColor(Color.fromRGB255(255,255,255));
        multiStateHardwareLight.setColorToStateFunction(RedGreenButtonState::toState);
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

    public HardwareButton getCaptureKeysFromDeckButton(int index) {
        return captureKeysFromDeckButton[index];
    }

    public MultiStateHardwareLight getChannelLight(int index) {
        return channelLight[index];
    }

    public StateButton getStateButton(int row, int col) {
        return gridButtons[row][col];
    }
    
    public HardwareSlider getSlider(int index) {
        return mainSliders[index];
    }

}
