package com.torso.t1;

import com.bitwig.extension.api.opensoundcontrol.OscMessage;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class T1DataPack {
    private static final String[] notes = {"C", "C#", "D", "D#", "E", "F", "G", "G#", "A", "A#", "B"};
    private static final String[] divisions = {"1/1", "1/2", "1/4", "1/8", "1/16", "1/32", "1/64", "1/3", "1/6", "1/12", "1/24", "1/48"};
    private int steps;
    private int numNotes;
    private int pulses;
    private int rootNote;
    private int channel;
    private int division;
    private List<Integer> offsets = new ArrayList<>();
    private List<Integer> pulseLoc = new ArrayList<>();
    private final List<Integer> inKeyNotes = new ArrayList<>();
    private int sustain;

    public void applyData(final String command, final OscMessage message) {
        switch (command) {
            case "steps":
                steps = message.getInt(0);
                break;
            case "numNotes":
                numNotes = message.getInt(0);
                break;
            case "notes":
                offsets = message.getArguments()
                        .stream()
                        .filter(Integer.class::isInstance)
                        .map(Integer.class::cast)
                        .map(this::notValueToArpOffset) //
                        .collect(Collectors.toList());
                break;
            case "pulses":
                pulses = message.getInt(0);
                break;
            case "pulseLoc":
                pulseLoc = message.getArguments()
                        .stream()
                        .filter(Integer.class::isInstance)
                        .map(Integer.class::cast)
                        .collect(Collectors.toList());
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
                final int size = message.getArguments().size();
                for (int i = 0; i < size; i++) {
                    inKeyNotes.add(message.getInt(i));
                }
            case "sustain":
                sustain = message.getInt(0);
                break;
        }
    }

    private int notValueToArpOffset(final int noteval) {
        return Math.max(-24, Math.min(noteval - 60, 24));
    }

    public void applyToDevice(final DeviceTrack deviceTrack) {
        deviceTrack.getMapTransposeDevice().setRootNote(rootNote);
        deviceTrack.getMapTransposeDevice().setScale(inKeyNotes);
        final ArpDevice arp = deviceTrack.getArpDevice();
        arp.setStepLength(steps);
        arp.setGateLength(sustain);
        arp.setRate(division);
        arp.setOffsets(offsets, steps);
        arp.setPulseLocations(pulseLoc, pulses, steps);
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
