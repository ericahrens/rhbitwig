package com.torso.t1;

import com.bitwig.extension.api.opensoundcontrol.OscAddressSpace;
import com.bitwig.extension.api.opensoundcontrol.OscConnection;
import com.bitwig.extension.api.opensoundcontrol.OscMessage;
import com.bitwig.extension.api.opensoundcontrol.OscModule;
import com.bitwig.extension.controller.ControllerExtension;
import com.bitwig.extension.controller.ControllerExtensionDefinition;
import com.bitwig.extension.controller.api.*;
import com.bitwig.extensions.framework.Layer;
import com.bitwig.extensions.framework.Layers;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TorsoT1OscControllerExtension extends ControllerExtension {

    private static final int TRACKS = 16;
    private HardwareSurface surface;
    private Layers layers;
    private Layer mainLayer;
    private OscModule oscModule;
    private TrackBank trackBank;
    private ControllerHost host;
    private final Map<Integer, DeviceTrack> deviceTrackMap = new HashMap<>();
    private final List<DeviceTrack> deviceTracks = new ArrayList<>();

    protected TorsoT1OscControllerExtension(final ControllerExtensionDefinition definition, final ControllerHost host) {
        super(definition, host);
    }

    @Override
    public void init() {
        host = getHost();
        layers = new Layers(this);
        surface = host.createHardwareSurface();
        mainLayer = new Layer(layers, "MainLayer");
        oscModule = host.getOscModule();
        final int port = setUpPreferences();
        final OscAddressSpace address = oscModule.createAddressSpace();
        address.registerDefaultMethod(this::handleMessage);
        oscModule.createUdpServer(port, address);
        setupDevices();

        host.showPopupNotification("Intialize Torso T1 - OSC");
        mainLayer.activate();
    }

    private int setUpPreferences() {
        final Preferences preferences = getHost().getPreferences(); // THIS
        final SettableRangedValue portSetting = preferences.getNumberSetting("Port", "Server", 4000, 10000, 1, "",
                8500);
        portSetting.markInterested();
        final int port = (int) (portSetting.get() * 6000) + 4000;
        host.println("Initial Port " + port);
        return port;
    }


    private void setupDevices() {
        trackBank = host.createTrackBank(TRACKS, 1, 1);
        for (int index = 0; index < TRACKS; index++) {
            final int trackIndex = index;
            final Track track = trackBank.getItemAt(index);
            final DeviceTrack deviceTrack = new DeviceTrack(index, track, host);
            deviceTracks.add(deviceTrack);
            track.name().addValueObserver(newName -> handleTrackNameChanged(deviceTrack, newName, trackIndex));
        }
    }

    private void handleTrackNameChanged(final DeviceTrack deviceTrack, final String newName, final int trackIndex) {
        final String oldName = deviceTrack.getName();
        final int oldTrackIndex = extractTrackNumber(oldName);
        final int trackNumber = extractTrackNumber(newName);
        if (oldTrackIndex > 0 && oldTrackIndex < TRACKS && oldTrackIndex != trackNumber) {
            deviceTrackMap.remove(oldTrackIndex);
        }
        if (trackNumber > 0 && trackNumber < TRACKS) {
            deviceTrackMap.put(trackNumber, deviceTracks.get(trackIndex));
        }
        deviceTrack.setName(newName);
    }

    private int extractTrackNumber(final String value) {
        if (value.matches("^[T,t]\\d{1,2}-.*")) {
            if (value.length() < 4) {
                return -1;
            }
            final char c1 = value.charAt(1);
            final char c2 = value.charAt(2);
            if (Character.isDigit(c1) && Character.isDigit(c2)) {
                return (c1 - '0') * 10 + (c2 - '0');
            }
            if (Character.isDigit(c1)) {
                return c1 - '0';
            }
        }
        return -1;
    }

    T1DataPack dataPack = new T1DataPack();

    private void handleMessage(final OscConnection connection, final OscMessage message) {
        final String[] split = message.getAddressPattern().split("/");
        if (split.length == 3 && split[1].equals("t1")) {
            final String command = split[2];
            if (command.equals("channel")) {
                dataPack = new T1DataPack();
            }
            dataPack.applyData(command, message);
            if (command.equals("division")) {
                final int channel = dataPack.getChannel();
                if (channel == 0) {
                    deviceTrackMap.values().forEach(deviceTrack -> dataPack.applyToDevice(deviceTrack));
                } else {
                    final DeviceTrack devTrack = deviceTrackMap.get(channel);
                    if (devTrack != null) {
                        devTrack.getMapTransposeDevice().info();
                        devTrack.getArpDevice().info();
                        dataPack.applyToDevice(devTrack);
                    }
                }
            }
        }
    }

    @Override
    public void exit() {
        getHost().showPopupNotification("Exit Torso T1");
    }

    @Override
    public void flush() {
        surface.updateHardware();
    }


}
