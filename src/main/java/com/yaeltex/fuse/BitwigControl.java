package com.yaeltex.fuse;

import com.bitwig.extension.controller.api.Clip;
import com.bitwig.extension.controller.api.ControllerHost;
import com.bitwig.extension.controller.api.CursorDeviceFollowMode;
import com.bitwig.extension.controller.api.CursorRemoteControlsPage;
import com.bitwig.extension.controller.api.CursorTrack;
import com.bitwig.extension.controller.api.PinnableCursorDevice;
import com.bitwig.extension.controller.api.Track;
import com.bitwig.extension.controller.api.TrackBank;
import com.bitwig.extensions.framework.di.Component;
import com.yaeltex.common.YaelTexColors;

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
    private final CursorRemoteControlsPage deviceRemotePages;
    private final Clip cursorClip;
    private int selectedTrackIndex;
    private final int[] trackColors = new int[NUM_TRACKS];
    
    public BitwigControl(ControllerHost host) {
        rootTrack = host.getProject().getRootTrackGroup();
        trackBank = host.createTrackBank(NUM_TRACKS, NUM_SENDS, NUM_SCENES, true);
        cursorTrack = host.createCursorTrack(NUM_SENDS, NUM_SCENES);
        cursorTrack.exists().markInterested();
        cursorDevice = cursorTrack.createCursorDevice();
        deviceRemotePages = cursorDevice.createCursorRemoteControlsPage(8);
        cursorClip = host.createLauncherCursorClip(32, 127);
        
        effectTrackBank = host.createEffectTrackBank(NUM_SENDS, NUM_SENDS, NUM_SCENES);
        
        //cursorClip = cursorTrack.createLauncherCursorClip(32, 127);
        primaryDevice =
            cursorTrack.createCursorDevice("drumdetection", "Pad Device", 8, CursorDeviceFollowMode.FIRST_INSTRUMENT);
        for (int index = 0; index < NUM_TRACKS; index++) {
            prepareTrack(trackBank.getItemAt(index), index);
        }
    }
    
    private void prepareTrack(final Track track, int index) {
        track.arm().markInterested();
        track.exists().markInterested();
        track.solo().markInterested();
        track.mute().markInterested();
        track.crossFadeMode().markInterested();
        track.color().addValueObserver((r, g, b) -> {
            trackColors[index] = YaelTexColors.toColor(r, g, b);
        });
        track.addIsSelectedInMixerObserver(select -> {
            if (select) {
                this.selectedTrackIndex = index;
            }
        });
    }
    
    public TrackBank getEffectTrackBank() {
        return effectTrackBank;
    }
    
    public TrackBank getTrackBank() {
        return trackBank;
    }
    
    public Clip getCursorClip() {
        return cursorClip;
    }
    
    public CursorTrack getCursorTrack() {
        return cursorTrack;
    }
    
    public Track getRootTrack() {
        return rootTrack;
    }
    
    public CursorRemoteControlsPage getDeviceRemotePages() {
        return deviceRemotePages;
    }
}
