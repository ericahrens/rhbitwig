package com.yaeltex.seqarp168new;

import com.bitwig.extension.controller.api.Clip;
import com.bitwig.extension.controller.api.ControllerHost;
import com.bitwig.extension.controller.api.CursorTrack;
import com.bitwig.extension.controller.api.PinnableCursorDevice;
import com.bitwig.extension.controller.api.Track;
import com.bitwig.extension.controller.api.TrackBank;

public class SeqArpBitwigControl {
    private static final int NUM_SCENES = 16;
    private static final int NUM_TRACKS = 6;
    private static final int NUM_SENDS = 4;
    
    private final TrackBank trackBank;
    private final Track rootTrack;
    private final CursorTrack cursorTrack;
    private final PinnableCursorDevice cursorDevice;
    private final Clip cursorClip;
    
    
    public SeqArpBitwigControl(final ControllerHost host) {
        rootTrack = host.getProject().getRootTrackGroup();
        trackBank = host.createTrackBank(NUM_TRACKS, NUM_SENDS, NUM_SCENES, true);
        cursorTrack = host.createCursorTrack(NUM_SENDS, NUM_SCENES);
        cursorTrack.exists().markInterested();
        cursorDevice = cursorTrack.createCursorDevice();
        cursorClip = host.createLauncherCursorClip(32, 127);
    }
}
