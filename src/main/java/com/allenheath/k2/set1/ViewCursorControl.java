package com.allenheath.k2.set1;

import com.bitwig.extension.controller.api.*;
import com.bitwig.extensions.rh.SpecialDevices;
import com.rhcommons.InKeyScale;
import com.rhcommons.MapTranspose;

import java.util.ArrayList;
import java.util.List;

public class ViewCursorControl {
    private final CursorTrack cursorTrack;
    private final DeviceBank deviceBank;
    private final PinnableCursorDevice primaryDevice;
    private final DeviceBank drumBank;
    private final DrumPadBank drumPadBank;
    private final TrackBank trackBank;
    private final List<MapTranspose> transposeDevices = new ArrayList<>();
    // private final Device drumDevice;

    public ViewCursorControl(final ControllerHost host, final List<DirectParameterControl> parameterControls,
                             final int tracks) {
        super();
        trackBank = host.createTrackBank(tracks, 8, 8);
        cursorTrack = host.createCursorTrack(8, 8);

        trackBank.followCursorTrack(cursorTrack);
        cursorTrack.clipLauncherSlotBank().cursorIndex().addValueObserver(index -> {
            // RemoteConsole.out.println(" => {}", index);
        });

        deviceBank = cursorTrack.createDeviceBank(8);
        primaryDevice = cursorTrack.createCursorDevice("drumdetection", "Pad Device", 8,
                CursorDeviceFollowMode.FIRST_INSTRUMENT);
        primaryDevice.hasDrumPads().markInterested();
        primaryDevice.exists().markInterested();
        final DeviceMatcher drumMatcher = host.createBitwigDeviceMatcher(SpecialDevices.DRUM.getUuid());
        drumBank = cursorTrack.createDeviceBank(1);
        drumBank.setDeviceMatcher(drumMatcher);
        // drumDevice = drumBank.getItemAt(0);
        drumPadBank = primaryDevice.createDrumPadBank(16);
        for (int i = 0; i < 16; i++) {
            final DrumPad pad = drumPadBank.getItemAt(i);
            for (int j = 0; j < 8; j++) {
                final Send sendItem = pad.sendBank().getItemAt(j);
                sendItem.exists().markInterested();
                sendItem.value().markInterested();
            }
        }
        setUpDevices(host, parameterControls, tracks);
    }

    private void setUpDevices(final ControllerHost host, final List<DirectParameterControl> controls,
                              final int tracks) {
        for (int i = 0; i < tracks; i++) {
            final int index = i;
            final Track track = trackBank.getItemAt(i);
            final CursorDevice cursorDevice = track.createCursorDevice();
            for (final DirectParameterControl directParameterControl : controls) {
                setUpTrackDevice(i, track, cursorDevice, directParameterControl, host);
            }
            MapTranspose mapTransposeDevice = new MapTranspose(index, host, track);
            transposeDevices.add(mapTransposeDevice);
        }
    }

    private void setUpTrackDevice(final int index, final Track track, final CursorDevice cursorDevice,
                                  final DirectParameterControl parameterControl, final ControllerHost host) {

        final DeviceBank db = track.createDeviceBank(1);
        db.setDeviceMatcher(parameterControl.getDeviceType().createDeviceMatcher(host));
        parameterControl.register(db.getItemAt(0), cursorDevice);
    }

    public TrackBank getTrackBank() {
        return trackBank;
    }

    public CursorTrack getCursorTrack() {
        return cursorTrack;
    }

    public DeviceBank getDeviceBank() {
        return deviceBank;
    }

    public PinnableCursorDevice getPrimaryDevice() {
        return primaryDevice;
    }

    public DeviceBank getDrumBank() {
        return drumBank;
    }

    public DrumPadBank getDrumPadBank() {
        return drumPadBank;
    }

    public void setScale(String scaleValue) {
        int n = scaleValue.length();
        String type = scaleValue.substring(n - 1, n);
        int number = Integer.parseInt(scaleValue.substring(0, n - 1)) - 1;
        int baseNote = (6 * 12 + 11 - ("A".equals(type) ? 3 : 0) - number * 5) % 12;

        transposeDevices.stream().filter(device -> device.exists()).forEach(device -> {
            if ("A".equals(type)) {
                device.setScale(InKeyScale.MINOR);
                device.setRootNote(baseNote);
            } else {
                device.setScale(InKeyScale.MAJOR);
                device.setRootNote(baseNote);
            }
        });
    }
}
