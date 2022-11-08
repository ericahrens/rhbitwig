package com.torso.t1;

import com.bitwig.extension.controller.api.ControllerHost;
import com.bitwig.extension.controller.api.Track;

import java.util.UUID;

public class NoteGridDevice extends FollowDevice {
    public static final UUID BITWIG_NOTE_GRID_DEVICE = UUID.fromString("264d6f4e-5067-46c9-a4fa-a75a295d9e01");

    public NoteGridDevice(final int index, final ControllerHost host, final Track track) {
        super(index, host, track, BITWIG_NOTE_GRID_DEVICE);
        if (index == 0) {
            host.println(">>>>>>>> not grid device ");
            followDevice.exists().addValueObserver(ex -> host.println("t1 note grid exist" + ex));
            followDevice.addDirectParameterIdObserver(parameters -> {
                host.println("################## NOTE GRIDDER ############");
                for (final String parameter : parameters) {
                    host.println(" " + parameter);
                }
                host.println("########################################");
            });
        }
    }


}
