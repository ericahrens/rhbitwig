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
            final Track track = trackBank.getItemAt(index);
            final DeviceTrack deviceTrack = new DeviceTrack(index, track, host);
            deviceTracks.add(deviceTrack);
            track.name().addValueObserver(newName -> handleTrackNameChanged(deviceTrack, newName));
        }
    }

    private void handleTrackNameChanged(final DeviceTrack deviceTrack, final String newName) {
        final String oldName = deviceTrack.getName();
        final int oldTrackIndex = extractTrackNumber(oldName);
        final int trackNumber = extractTrackNumber(newName);
        if (oldTrackIndex > 0 && oldTrackIndex < TRACKS && oldTrackIndex != trackNumber) {
            deviceTrackMap.remove(oldTrackIndex);
            //host.println(String.format("Remove %d %s", oldTrackIndex, oldName));
        }
        if (trackNumber > 0 && trackNumber < TRACKS) {
            deviceTrackMap.put(trackNumber, deviceTracks.get(trackNumber - 1));
            //host.println(String.format("STORE %d %s", trackNumber, newName));
        }
        deviceTrack.setName(newName);
    }

    private int extractTrackNumber(final String value) {
        if (value.matches("^[T,t]\\d{1,2}-.*")) {
            final StringBuilder sb = new StringBuilder();
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
            if (command.equals("sustain")) {
                final DeviceTrack devTrack = deviceTrackMap.get(dataPack.getChannel());
                if (devTrack != null) {
                    dataPack.applyToDevice(devTrack);
                }
            }
        }
    }

    private void handleMessage_(final OscConnection connection, final OscMessage message) {
        host.println("MSG : " + message.getAddressPattern());
        final String[] split = message.getAddressPattern().split("/");
        if (split.length == 3) {
            final int track = oscTrack(split[1]);
            final String command = split[2];
            if (track != -1) {
                final DeviceTrack devTrack = deviceTrackMap.get(track);
                if (devTrack != null) {
                    getHost().println(String.format("[%d] command=%s %s", track, command, message.getArguments()));
                    switch (command) {
                        case "root":
                            devTrack.getMapTransposeDevice().setRootNote(message.getInt(0));
                            break;
                        case "steps":
                            devTrack.getArpDevice().setStepLength(message.getInt(0));
                            break;
                    }
                }
            }
        }
    }

    private int oscTrack(final String trackPart) {
        if (trackPart.length() > 1) {
            final char c1 = trackPart.charAt(0);
            if (c1 != 't') {
                return -1;
            }
            final char c2 = trackPart.charAt(1);
            if (trackPart.length() > 2) {
                final char c3 = trackPart.charAt(2);
                if (Character.isDigit(c2) && Character.isDigit(c3)) {
                    return (c2 - '0') * 10 + (c3 - '0');
                }
            } else {
                if (Character.isDigit(c2)) {
                    return c2 - '0';
                }
            }
        }
        return -1;
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
