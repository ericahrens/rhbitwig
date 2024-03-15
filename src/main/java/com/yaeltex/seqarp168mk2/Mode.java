package com.yaeltex.seqarp168mk2;

public enum Mode {
    ARP,
    SEQUENCER,
    REMOTES,
    SENDS;
    
    public Mode next() {
        return switch (this) {
            case ARP -> SEQUENCER;
            case SEQUENCER -> SENDS;
            case SENDS -> REMOTES;
            case REMOTES -> ARP;
        };
    }
}
