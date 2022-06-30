package com.torso.t1;

import com.bitwig.extension.controller.api.*;

import java.util.UUID;

public class FollowDevice {

    protected final int index;
    protected final SpecificBitwigDevice specificDevice;
    protected final Device followDevice;
    protected final ControllerHost host;


    public FollowDevice(final int index, final ControllerHost host, final Track track, final UUID bitwigDeviceId) {
        this.index = index;
        this.host = host;
        final DeviceBank deviceBank = track.createDeviceBank(1);
        deviceBank.setDeviceMatcher(host.createBitwigDeviceMatcher(bitwigDeviceId));

        followDevice = deviceBank.getDevice(0);
        specificDevice = followDevice.createSpecificBitwigDevice(bitwigDeviceId);
        followDevice.exists().addValueObserver(exists -> {
            if (exists) {
                host.println(String.format("<%d> DETECT %s ", index, getClass().getSimpleName()));
            }
        });
    }

    public void info() {
        host.println(String.format("FD %d class=%s exist=%s", index, getClass().getSimpleName(),
                followDevice.exists().get()));
    }

    protected Parameter createParameter(final String id) {
        final Parameter parameter = specificDevice.createParameter(id);
        parameter.markInterested();
        parameter.value().markInterested();
        return parameter;
    }
}
