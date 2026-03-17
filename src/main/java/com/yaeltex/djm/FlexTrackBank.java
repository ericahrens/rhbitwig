package com.yaeltex.djm;

import java.util.HashMap;
import java.util.Map;

import com.bitwig.extension.controller.api.Channel;
import com.bitwig.extension.controller.api.ControllerHost;
import com.bitwig.extension.controller.api.CursorTrack;
import com.bitwig.extension.controller.api.Track;
import com.bitwig.extension.controller.api.TrackBank;

public class FlexTrackBank {
    
    private final TrackBank overviewTrackBank;
    
    private static class SingleTrackBank {
        
        private final FixedTracks ident;
        private final CursorTrack followTrack;
        
        public SingleTrackBank(final ControllerHost host, final FixedTracks ident, final int sends, final int scenes) {
            followTrack = host.createCursorTrack("cursor"+ident.getName(), ident.getName(), sends, scenes, false);
            
            this.ident = ident;
            followTrack.isPinned().markInterested();
            followTrack.name().markInterested();
            followTrack.name().addValueObserver(name->{
                DjmControllerExtension.println(" FC <%s> %s",ident,name);
            });
        }
        
        public void moveTo(final Channel channel) {
            DjmControllerExtension.println(" MOV %s",channel.name().get());
            followTrack.selectChannel(channel);
            followTrack.isPinned().set(true);
        }
        
        public FixedTracks getIdent() {
            return ident;
        }
        
        public Track getTrack() {
            return followTrack;
        }
    }
    
    private final Map<FixedTracks, SingleTrackBank> lookup = new HashMap<>();
    private final Map<String, SingleTrackBank> nameLookup = new HashMap<>();
    
    public FlexTrackBank(final ControllerHost host, final int overviewSize, final int sends, final int scenes) {
        overviewTrackBank = host.createTrackBank(overviewSize, 1, 1);
        for (int i = 0; i < overviewSize; i++) {
            final int index = i;
            final Track track = overviewTrackBank.getItemAt(index);
            track.name().addValueObserver(name -> trackNameChanged(index, name, track));
        }
        final FixedTracks[] fixedTracks = FixedTracks.values();
        for (int i = 0; i < fixedTracks.length; i++) {
            if(!fixedTracks[i].getName().isEmpty()){
                lookup.put(fixedTracks[i], new SingleTrackBank(host, fixedTracks[i], sends, scenes));
                nameLookup.put(fixedTracks[i].getName(), new SingleTrackBank(host, fixedTracks[i], sends, scenes));
            }
        }
    }
    
    private void trackNameChanged(final int index, final String name, final Channel track) {
        final SingleTrackBank fixedTrack = nameLookup.get(name);
        if (fixedTrack != null) {
            DjmControllerExtension.println(" MOVE TRACK %d %s <= %s",index,name, fixedTrack.ident);
            fixedTrack.moveTo(track);
        }
    }
    
    public Track getTrack(final FixedTracks ident) {
        return lookup.get(ident).getTrack();
    }
    
}
