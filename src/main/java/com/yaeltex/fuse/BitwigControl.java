package com.yaeltex.fuse;

import com.bitwig.extension.controller.api.ControllerHost;
import com.bitwig.extension.controller.api.CursorDeviceFollowMode;
import com.bitwig.extension.controller.api.CursorTrack;
import com.bitwig.extension.controller.api.PinnableCursorDevice;
import com.bitwig.extension.controller.api.Track;
import com.bitwig.extension.controller.api.TrackBank;
import com.bitwig.extensions.framework.di.Component;
import com.yaeltex.common.ColorUtil;

@Component
public class BitwigControl {
    private static final int NUM_SCENES = 16;
    private static final int NUM_TRACKS = 6;
    private static final int NUM_SENDS = 4;
    private final TrackBank trackBank;
    private final TrackBank effectTrackBank;
    private final CursorTrack cursorTrack;
    private final Track rootTrack;
    private final PinnableCursorDevice cursorDevice;
    private final PinnableCursorDevice primaryDevice;
    private final int[] trackColors = new int[NUM_TRACKS];
    
    public BitwigControl(final ControllerHost host) {
        rootTrack = host.getProject().getRootTrackGroup();
        trackBank = host.createTrackBank(NUM_TRACKS, NUM_SENDS, NUM_SCENES, false);
        cursorTrack = host.createCursorTrack(NUM_SENDS, NUM_SCENES);
        cursorTrack.exists().markInterested();
        cursorDevice = cursorTrack.createCursorDevice();
        
        effectTrackBank = host.createEffectTrackBank(NUM_SENDS, NUM_SENDS, NUM_SCENES);
        
        primaryDevice =
            cursorTrack.createCursorDevice("drumdetection", "Pad Device", 8, CursorDeviceFollowMode.FIRST_INSTRUMENT);
        for (int index = 0; index < NUM_TRACKS; index++) {
            prepareTrack(trackBank.getItemAt(index), index);
        }
    }
    
    private void prepareTrack(final Track track, final int index) {
        track.arm().markInterested();
        track.exists().markInterested();
        track.solo().markInterested();
        track.mute().markInterested();
        track.crossFadeMode().markInterested();
        track.color().addValueObserver((r, g, b) -> {
            trackColors[index] = ColorUtil.matchToIndex(r, g, b);
        });
    }
    
    public TrackBank getEffectTrackBank() {
        return effectTrackBank;
    }
    
    public TrackBank getTrackBank() {
        return trackBank;
    }
    
    public CursorTrack getCursorTrack() {
        return cursorTrack;
    }
    
    public Track getRootTrack() {
        return rootTrack;
    }
    
}
