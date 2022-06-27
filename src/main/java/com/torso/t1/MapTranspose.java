package com.torso.t1;

import com.bitwig.extension.controller.api.ControllerHost;
import com.bitwig.extension.controller.api.Parameter;
import com.bitwig.extension.controller.api.Track;

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

    public void setScale(final Integer anInt) {
        if (anInt < scalesSettings.length) {
            final int[] scales = scalesSettings[anInt];
            for (int i = 0; i < 12; i++) {
                pitchClasses[i].value().set(scales[i], 12);
            }
        }
    }

    public void setRootNote(final int noteIndex) {
        if (noteIndex != -1) {
            rootNote.value().set(noteIndex, 11);
        }
    }

}
