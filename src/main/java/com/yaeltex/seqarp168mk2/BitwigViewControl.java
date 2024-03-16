package com.yaeltex.seqarp168mk2;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import com.bitwig.extension.controller.api.ControllerHost;
import com.bitwig.extension.controller.api.CursorDeviceFollowMode;
import com.bitwig.extension.controller.api.CursorTrack;
import com.bitwig.extension.controller.api.Device;
import com.bitwig.extension.controller.api.DeviceBank;
import com.bitwig.extension.controller.api.DeviceMatcher;
import com.bitwig.extension.controller.api.DrumPadBank;
import com.bitwig.extension.controller.api.PinnableCursorClip;
import com.bitwig.extension.controller.api.PinnableCursorDevice;
import com.bitwig.extension.controller.api.Track;
import com.bitwig.extension.controller.api.TrackBank;
import com.bitwig.extensions.framework.di.Component;
import com.yaeltex.devices.SpecialDevices;
import com.yaeltex.seqarp168.QuantizeMode;
import com.yaeltex.seqarp168mk2.device.ArpInstance;
import com.yaeltex.seqarp168mk2.device.DeviceSlotState;
import com.yaeltex.seqarp168mk2.device.FocusDevice;

@Component
public class BitwigViewControl {
    private static final int NUM_SCENES = 8;
    private static final int NUM_TRACKS = 8;
    private static final int NUM_SENDS = 4;
    
    private final TrackBank trackBank;
    private final Track rootTrack;
    private final CursorTrack cursorTrack;
    private final PinnableCursorDevice cursorDevice;
    private final FocusDevice arpDevice1;
    private final FocusDevice arpDevice2;
    private final DeviceMatcher arpDeviceMatcher;
    private final List<ArpInstance> arpInstances = new ArrayList<>();
    private final Device focusArpDevice;
    private final DeviceBank drumBank;
    private final PinnableCursorDevice drumFollowDevice;
    private final DrumPadBank drumPadBank;
    private final PinnableCursorClip cursorClip;
    
    
    public BitwigViewControl(final ControllerHost host) {
        rootTrack = host.getProject().getRootTrackGroup();
        trackBank = host.createTrackBank(NUM_TRACKS, NUM_SENDS, NUM_SCENES, true);
        cursorTrack = host.createCursorTrack(NUM_SENDS, NUM_SCENES);
        trackBank.followCursorTrack(cursorTrack);
        cursorTrack.exists().markInterested();
        cursorTrack.name().markInterested();
        cursorDevice = cursorTrack.createCursorDevice();
        cursorClip = cursorTrack.createLauncherCursorClip("SQClip", "SQClip", 32, 1);
        arpDeviceMatcher = host.createBitwigDeviceMatcher(SpecialDevices.ARPEGGIATOR.getUuid());
        drumFollowDevice =
            cursorTrack.createCursorDevice("drumdetection", "Pad Device", 4, CursorDeviceFollowMode.FIRST_INSTRUMENT);
        drumFollowDevice.hasDrumPads().markInterested();
        drumFollowDevice.exists().markInterested();
        final DeviceMatcher drumMatcher =
            host.createBitwigDeviceMatcher(com.bitwig.extensions.framework.values.SpecialDevices.DRUM.getUuid());
        drumBank = cursorTrack.createDeviceBank(1);
        drumBank.setDeviceMatcher(drumMatcher);
        //drumDevice = drumBank.getItemAt(0);
        drumPadBank = drumFollowDevice.createDrumPadBank(16);
        
        final DeviceBank deviceBank = cursorTrack.createDeviceBank(1);
        deviceBank.setDeviceMatcher(arpDeviceMatcher);
        focusArpDevice = deviceBank.getItemAt(0);
        focusArpDevice.exists().addValueObserver(exists -> {
            handleGeneralArpFocusChanged();
        });
        cursorTrack.position().addValueObserver(pos -> {
            if (focusArpDevice.exists().get()) {
                handleGeneralArpFocusChanged();
            }
        });
        
        arpDevice1 = new FocusDevice(0, host, this);
        arpDevice2 = new FocusDevice(1, host, this);
    }
    
    private void handleGeneralArpFocusChanged() {
        if (arpDevice1.getSlotState() != DeviceSlotState.LOCKED) {
            arpDevice1.link(cursorTrack, focusArpDevice);
        } else if (arpDevice2.getSlotState() != DeviceSlotState.LOCKED) {
            arpDevice2.link(cursorTrack, focusArpDevice);
        }
    }
    
    public Track getRootTrack() {
        return rootTrack;
    }
    
    public CursorTrack getCursorTrack() {
        return cursorTrack;
    }
    
    public DeviceMatcher getArpDeviceMatcher() {
        return arpDeviceMatcher;
    }
    
    public FocusDevice getArpDevice1() {
        return arpDevice1;
    }
    
    public FocusDevice getArpDevice2() {
        return arpDevice2;
    }
    
    public ArpInstance getArpInstance(final String trackName, final String presetName) {
        final Optional<ArpInstance> arpOpt = arpInstances //
            .stream() //
            .filter(arp -> arp.matches(trackName, presetName)) //
            .findFirst();
        
        if (arpOpt.isEmpty()) {
            final ArpInstance arpInstance = new ArpInstance(trackName, presetName, QuantizeMode.NEAREST_VALUE);
            arpInstances.add(arpInstance);
            return arpInstance;
        } else {
            return arpOpt.get();
        }
    }
    
    public PinnableCursorDevice getCursorDevice() {
        return cursorDevice;
    }
    
    public PinnableCursorClip getCursorClip() {
        return cursorClip;
    }
    
    public DrumPadBank getDrumPadBank() {
        return drumPadBank;
    }
    
    public TrackBank getTrackBank() {
        return trackBank;
    }
}
