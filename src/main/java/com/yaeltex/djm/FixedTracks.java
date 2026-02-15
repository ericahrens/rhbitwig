package com.yaeltex.djm;

public enum FixedTracks implements PageIdent {
    TRACK_1("VdeckA"),
    TRACK_2("VdeckB"),
    TRACK_3("VdeckC"),
    TRACK_4("VdeckD");
    
    private final String name;
    
    FixedTracks(final String name) {
        this.name = name;
    }
    
    @Override
    public String getName() {
        return name;
    }
}
