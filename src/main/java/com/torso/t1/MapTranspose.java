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
        final int[] mapping = new int[12];
        for (int i = 0; i < inKeyNotes.size(); i++) {
            final int inKey = inKeyNotes.get(i);
            final int next = i + 1 < inKeyNotes.size() ? inKeyNotes.get(i + 1) : -1;
            mapping[inKey] = inKey;
            final int diff = next - inKey;
            if (next == -1) {
                for (int j = inKey + 1; j < 12; j++) {
                    mapping[j] = inKey;
                }
            } else if (diff == 2) {
                mapping[inKey + 1] = inKey;
            } else if (diff == 3) {
                mapping[inKey + 1] = inKey;
                mapping[inKey + 2] = next;
            } else if (diff == 4) {
                mapping[inKey + 1] = inKey;
                mapping[inKey + 2] = inKey;
                mapping[inKey + 3] = next;
            }
        }
        for (int i = 0; i < mapping.length; i++) {
            pitchClasses[i].set(mapping[i], 12);
        }
    }

    public void setRootNote(final int noteIndex) {
        if (noteIndex != -1) {
            rootNote.value().set(noteIndex, 12);
        }
    }

}
