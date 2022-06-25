package com.torso.t1;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import com.allenheath.k2.set1.DirectParameterControl;
import com.allenheath.k2.set1.PadAssignment;
import com.allenheath.k2.set1.PadContainer;
import com.allenheath.k2.set1.SpecialParam;
import com.allenheath.k2.set1.SpecialVst3Devices;
import com.allenheath.k2.set1.SpecialVstDevices;
import com.allenheath.k2.set1.StateButton;
import com.allenheath.k2.set1.ViewCursorControl;
import com.bitwig.extension.api.opensoundcontrol.*;
import com.bitwig.extension.controller.ControllerExtension;
import com.bitwig.extension.controller.ControllerExtensionDefinition;
import com.bitwig.extension.controller.api.ControllerHost;
import com.bitwig.extension.controller.api.CursorDevice;
import com.bitwig.extension.controller.api.DocumentState;
import com.bitwig.extension.controller.api.HardwareSurface;
import com.bitwig.extension.controller.api.MidiIn;
import com.bitwig.extension.controller.api.MidiOut;
import com.bitwig.extension.controller.api.NoteInput;
import com.bitwig.extension.controller.api.SettableStringValue;
import com.bitwig.extension.controller.api.SpecificBitwigDevice;
import com.bitwig.extension.controller.api.Track;
import com.bitwig.extension.controller.api.TrackBank;
import com.bitwig.extensions.framework.Layer;
import com.bitwig.extensions.framework.Layers;

public class TorsoT1OscControllerExtension extends ControllerExtension {

    private static final int TRACKS = 16;
    private HardwareSurface surface;
    private Layers layers;
    private Layer mainLayer;
    private OscModule oscModule;
    private TrackBank trackBank;

    protected TorsoT1OscControllerExtension(final ControllerExtensionDefinition definition,
                                              final ControllerHost host) {
        super(definition, host);
    }

    @Override
    public void init() {
        final ControllerHost host = getHost();
        layers = new Layers(this);
        surface = host.createHardwareSurface();
        mainLayer = new Layer(layers, "MainLayer");
        oscModule = host.getOscModule();
        final OscAddressSpace address = oscModule.createAddressSpace();
        address.registerDefaultMethod(this::handleMessage);
        oscModule.createUdpServer(8000,address);
        trackBank = host.createTrackBank(TRACKS, 1, 1);
        for(int i=0;i<TRACKS;i++){
            final Track track = trackBank.getItemAt(i);
            final CursorDevice device = track.createCursorDevice();
            final SpecificBitwigDevice arpdevice =
               device.createSpecificBitwigDevice(UUID.fromString("4d407a2b-c91b-4e4c-9a89-c53c19fe6251"));
        }
        
        host.showPopupNotification("Intialize Torso T1 - OSC");
        mainLayer.activate();
    }

    private void handleMessage(OscConnection connection, OscMessage message) {
        //host.println(String.format("type=%s pat=%s args=%s",message.getTypeTag(),message.getAddressPattern(),message.getArguments()));
        final String[] split = message.getAddressPattern().split("/");
        getHost().println(String.format("=> %s %s",message.getAddressPattern(), Arrays.toString(split)));
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
