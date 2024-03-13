package com.yaeltex.seqarp168mk2;

public enum Mode {
    ARP, SEQUENCER, DEVICE;
    
    public Mode next() {
        return switch (this) {
            case ARP -> SEQUENCER;
            case SEQUENCER -> DEVICE;
            case DEVICE -> ARP;
        };
    }
}
