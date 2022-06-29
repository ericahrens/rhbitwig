package com.torso.t1;

import java.util.UUID;

import com.bitwig.extension.api.PlatformType;
import com.bitwig.extension.controller.AutoDetectionMidiPortNamesList;
import com.bitwig.extension.controller.ControllerExtensionDefinition;
import com.bitwig.extension.controller.api.ControllerHost;

public class TorsoT1OscExtensionDefinition extends ControllerExtensionDefinition {
    private static final UUID DRIVER_ID = UUID.fromString("867bdb9f-25cc-4733-9715-85b093b8e16b");

    public TorsoT1OscExtensionDefinition() {
    }

    @Override
    public String getName() {
        return "Torso T1";
    }

    @Override
    public String getAuthor() {
        return "E.Ahrens/R.Hawtin";
    }

    @Override
    public String getVersion() {
        return "0.1b";
    }

    @Override
    public UUID getId() {
        return DRIVER_ID;
    }

    @Override
    public String getHardwareVendor() {
        return "Torso";
    }

    @Override
    public String getHardwareModel() {
        return "Torso T-1";
    }

    @Override
    public int getRequiredAPIVersion() {
        return 16;
    }

    @Override
    public int getNumMidiInPorts() {
        return 0;
    }

    @Override
    public int getNumMidiOutPorts() {
        return 0;
    }

    @Override
    public String getSupportFolderPath() {
        return "Controllers/Torso/";
    }

    @Override
    public void listAutoDetectionMidiPortNames(final AutoDetectionMidiPortNamesList list,
                                               final PlatformType platformType) {
   }

    @Override
    public TorsoT1OscControllerExtension createInstance(final ControllerHost host) {
        return new TorsoT1OscControllerExtension(this, host);
    }
}
