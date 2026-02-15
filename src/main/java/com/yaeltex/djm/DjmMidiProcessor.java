package com.yaeltex.djm;

import com.bitwig.extension.controller.api.ControllerHost;
import com.bitwig.extension.controller.api.MidiOut;
import com.yaeltex.common.YaeltexMidiProcessor;

public class DjmMidiProcessor extends YaeltexMidiProcessor {
    
    private static final String DISPLAY_INIT_MSG = "F0 79 74 78 01 01 00 02 F7";
    private Runnable initCallback;
    
    public DjmMidiProcessor(final ControllerHost host, final int ports) {
        super(host, ports);
        for (int i = 0; i < ports; i++) {
            final MidiOut midiOut = host.getMidiOutPort(i);
            midiOut.sendSysex(DISPLAY_INIT_MSG);
            midiOut.sendMidi(0xB1, 0x00, 0x0);
            midiOut.sendMidi(0xB1, 0x01, 0x00);
            midiOut.sendMidi(0xB2, 0x00, 0x00);
            midiOut.sendMidi(0xB2, 0x01, 0x00);
        }
    }
    
    public void setInitCallback(final Runnable initCallback) {
        this.initCallback = initCallback;
    }
    
    public void sendText(final int port, final int displayIndex, final String text) {
        final String msg = "F0 79 74 78 00 01 04 01 %02X ".formatted(displayIndex) + toSysEx(text) + "F7";
        final MidiOut midiOut = host.getMidiOutPort(port);
        midiOut.sendSysex(msg);
    }
    
    @Override
    protected void handleMidiIn(final int status, final int data1, final int data2) {
        host.println("DJM MIDI => %02X %02X %02X".formatted(status, data1, data2));
    }
    
    @Override
    protected void handleSysEx(final String sysExString) {
        host.println("DJM SysEx = %s".formatted(sysExString));
        if (sysExString.equals("f079747801010401f7")) {
            if (initCallback != null) {
                initCallback.run();
            }
        }
    }
    
    @Override
    public void start() {
        super.start();
    }
    
    public static String toSysEx(final String text) {
        final StringBuilder sb = new StringBuilder();
        for (int i = 0; i < text.length(); i++) {
            final char c = convert(text.charAt(i));
            final String hexValue = Integer.toHexString((byte) c);
            sb.append(hexValue.length() < 2 ? "0" + hexValue : hexValue);
            sb.append(" ");
        }
        return sb.toString();
    }
    
    private static char convert(final char c) {
        if (c < 128) {
            return c;
        }
        switch (c) {
            case 'Á':
            case 'À':
            case 'Ä':
                return 'A';
            case 'É':
            case 'È':
                return 'E';
            case 'á':
            case 'à':
            case 'ä':
                return 'a';
            case 'Ö':
                return 'O';
            case 'Ü':
                return 'U';
            case 'è':
            case 'é':
                return 'e';
            case 'ö':
                return 'o';
            case 'ü':
                return 'u';
            case 'ß':
                return 's';
        }
        return '?';
    }
}
