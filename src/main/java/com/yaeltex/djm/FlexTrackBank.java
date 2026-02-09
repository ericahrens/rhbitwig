package com.yaeltex.djm;

import java.util.ArrayList;
import java.util.List;

import com.bitwig.extension.controller.api.ControllerHost;
import com.bitwig.extension.controller.api.Track;
import com.bitwig.extension.controller.api.TrackBank;

public class FlexTrackBank {
    
    private final TrackBank overviewTrackBank;
    private final List<SingleTrackBank> tracks = new ArrayList<>();
    
    private static class SingleTrackBank {
        
        private final TrackBank trackBank;
        private final Track track;
        private final int index;
        
        public SingleTrackBank(final ControllerHost host, final int index, final int sends, final int scenes) {
            trackBank = host.createTrackBank(1, sends, scenes);
            track = trackBank.getItemAt(0);
            this.index = index;
        }
        
        public int getIndex() {
            return index;
        }
        
        public Track getTrack() {
            return track;
        }
    }
    
    public FlexTrackBank(final ControllerHost host, final int overviewSize, final int size, final int sends,
        final int scenes) {
        overviewTrackBank = host.createTrackBank(overviewSize, 1, 1);
        for (int i = 0; i < overviewSize; i++) {
        
        }
        for (int i = 0; i < size; i++) {
            tracks.add(new SingleTrackBank(host, i, sends, scenes));
        }
    }
    
    public Track getTrack(final int index) {
        return tracks.get(index).getTrack();
    }
    
}
