package com.torso.t1;

import com.bitwig.extension.controller.api.ControllerHost;
import com.bitwig.extension.controller.api.Parameter;
import com.bitwig.extension.controller.api.Track;

import java.util.List;
import java.util.UUID;

public class MapTranspose extends FollowDevice {
    public static final UUID BITWIG_MAP_TRANSPOSE = UUID.fromString("284a1949-29d5-4dd4-8315-86cef92fd2cd");
    private final Parameter[] pitchClasses = new Parameter[12];
    private final Parameter rootNote;

    private final int[][] scalesSettings = {{0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11}, //
            {0, 0, 2, 2, 4, 5, 5, 7, 7, 9, 9, 11}, // major
            {0, 0, 2, 3, 3, 5, 7, 7, 8, 8, 10, 10}};

    public MapTranspose(final int index, final ControllerHost host, final Track track) {
        super(index, host, track, BITWIG_MAP_TRANSPOSE);
        for (int i = 0; i < 12; i++) {
            final Parameter pitchClass = specificDevice.createParameter(String.format("OUTPUT_PITCH_CLASS_%02d", i));
            pitchClass.value().markInterested();
            pitchClasses[i] = pitchClass;
        }
        rootNote = createParameter("ROOT");
    }

    public void setScale(final List<Integer> inKeyNotes) {
        host.println(String.format("SCALE %s",inKeyNotes));
        final int[] mapping = new int[12];
        for (int i=0;i<inKeyNotes.size();i++)
        {
            int inKey = inKeyNotes.get(i);
            int next = i+1<inKeyNotes.size() ? inKeyNotes.get(i+1) : -1;
            mapping[inKey] = inKey;
            int diff = next - inKey;
            host.println(String.format("in=%d nx=%d diff=%d",inKey, next, diff));
            if(next == -1) {
                for(int j=inKey+1;j<12;j++) {
                    host.println(" " + j + " > " + inKey);
                    mapping[j] = inKey;
                }
            } else if( diff == 2) {
                mapping[inKey+1] = inKey;
            } else if(diff == 3) {
                mapping[inKey+1] = inKey;
                mapping[inKey+2] = next;
            }
        }
        for(int i=0;i<mapping.length;i++) {
            pitchClasses[i].set(mapping[i],12);
        }
    }

    public void setRootNote(final int noteIndex) {
        if (noteIndex != -1) {
            rootNote.value().set(noteIndex, 11);
        }
    }

}
