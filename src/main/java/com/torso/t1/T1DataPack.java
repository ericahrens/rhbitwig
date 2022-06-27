package com.torso.t1;

import com.bitwig.extension.api.opensoundcontrol.OscMessage;

import java.util.ArrayList;
import java.util.List;

public class T1DataPack {
    private static final String[] notes = {"C", "C#", "D", "D#", "E", "F", "G", "G#", "A", "A#", "B"};
    private static final String[] scales = {"Chromatic", "Major", "Minor", "Hiro", "Iwatoo", "Tetra", "User"};
    private static final String[] divisions = {"1/1", "1/2", "1/4", "1/8", "1/16", "1/32", "1/3", "1/6", "1/12", "1/24", "1/48", "1/2D", "1/4D", "1/8D", "1/16D"};
    private int steps;
    private int numNotes;
    private int pulses;
    private int scale;
    private int rootNote;
    private int channel;
    private int division;
    private final List<Integer> offsets = new ArrayList<>();
    private final List<Integer> pulseLoc = new ArrayList<>();
    private int sustain;

    public void applyData(final String command, final OscMessage message) {
        switch (command) {
            case "steps":
                steps = message.getInt(0);
                break;
            case "numNotes":
                numNotes = message.getInt(0);
                break;
            case "pulses":
                pulses = message.getInt(0);
                break;
            case "division":
                division = toIndex(message.getString(0), divisions);
                break;
            case "channel":
                channel = message.getInt(0);
                break;
            case "root":
                rootNote = toIndex(message.getString(0), notes);
                break;
            case "scale":
                scale = toIndex(message.getString(0), scales);
                break;
            case "sustain":
                sustain = message.getInt(0);
                break;
        }
    }

    public void applyToDevice(final DeviceTrack deviceTrack) {
        deviceTrack.getMapTransposeDevice().setRootNote(rootNote);
        deviceTrack.getMapTransposeDevice().setScale(scale);
        final ArpDevice arp = deviceTrack.getArpDevice();
        arp.setStepLength(steps);
        arp.setGateLength(sustain);
    }

    public int getChannel() {
        return channel;
    }

    private int toIndex(final String stringValue, final String[] list) {
        for (int i = 0; i < list.length; i++) {
            if (list[i].equals(stringValue)) {
                return i;
            }
        }
        return -1;
    }

}
