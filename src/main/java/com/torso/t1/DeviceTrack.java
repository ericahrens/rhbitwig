package com.torso.t1;

import com.bitwig.extension.controller.api.ControllerHost;
import com.bitwig.extension.controller.api.Track;

public class DeviceTrack {

    private final ArpDevice arpDevice;
    private final MapTranspose mapTransposeDevice;
    private String name = "";

    public DeviceTrack(final int index, final Track track, final ControllerHost host) {
        arpDevice = new ArpDevice(index, host, track);
        mapTransposeDevice = new MapTranspose(index, host, track);
    }

    public void setName(final String newName) {
        name = newName;
    }

    public String getName() {
        return name;
    }

    public ArpDevice getArpDevice() {
        return arpDevice;
    }

    public MapTranspose getMapTransposeDevice() {
        return mapTransposeDevice;
    }
}
