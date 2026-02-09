package com.yaeltex.djm;

import com.bitwig.extension.controller.api.ControllerHost;
import com.bitwig.extension.controller.api.MidiOut;
import com.yaeltex.common.YaeltexMidiProcessor;

public class DjmMidiProcessor extends YaeltexMidiProcessor {
    
    private static final String DISPLAY_INIT_MSG = "F0 79 74 78 01 01 00 02 F7";
    private final MidiOut midiOut;
    
    public DjmMidiProcessor(final ControllerHost host, final int ports) {
        super(host, ports);
        midiOut = host.getMidiOutPort(0);
        midiOut.sendSysex(DISPLAY_INIT_MSG);
        midiOut.sendSysex("F0 79 74 78 00 01 04 01 00 41 44 20 20 F7");
        midiOut.sendSysex("F0 79 74 78 00 01 04 01 01 42 43 20 20 F7");
        midiOut.sendMidi(0xB1, 0x00, 0x0);
        midiOut.sendMidi(0xB1, 0x01, 0x00);
        midiOut.sendMidi(0xB2, 0x00, 0x00);
        midiOut.sendMidi(0xB2, 0x01, 0x00);
        //midiOut.sendMidi(0xB2, 0x0A, 0x40);
        //        midiOut.sendSysex("F0 79 74 78 00 01 04 02 00 00 3F F7");
        //        midiOut.sendSysex("F0 79 74 78 00 01 04 09 00 30 F7");
    }
    
    
    @Override
    protected void handleMidiIn(final int status, final int data1, final int data2) {
        host.println("DJM MIDI => %02X %02X %02X".formatted(status, data1, data2));
    }
    
    @Override
    protected void handleSysEx(final String sysExString) {
        host.println("DJM SysEx = %s".formatted(sysExString));
    }
    
    @Override
    public void start() {
        super.start();
        
    }
}
