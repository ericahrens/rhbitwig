package com.allenheath.k2.set1;

import com.bitwig.extension.api.PlatformType;
import com.bitwig.extension.controller.AutoDetectionMidiPortNamesList;
import com.bitwig.extension.controller.ControllerExtensionDefinition;
import com.bitwig.extension.controller.api.ControllerHost;

import java.util.UUID;

public class AllenHeathK2ExtensionDefinition extends ControllerExtensionDefinition {
    private static final UUID DRIVER_ID = UUID.fromString("867bdb9f-25cc-4733-9715-85b093b8e162");

    public AllenHeathK2ExtensionDefinition() {
    }

    @Override
    public String getName() {
        return "K2 DJSet 2022";
    }

    @Override
    public String getAuthor() {
        return "E.Ahrens/R.Hawtin";
    }

    @Override
    public String getVersion() {
        return "1.1";
    }

    @Override
    public UUID getId() {
        return DRIVER_ID;
    }

    @Override
    public String getHardwareVendor() {
        return "Allen & Heath";
    }

    @Override
    public String getHardwareModel() {
        return "Xone:K2";
    }

    @Override
    public int getRequiredAPIVersion() {
        return 16;
    }

    @Override
    public int getNumMidiInPorts() {
        return 1;
    }

    @Override
    public int getNumMidiOutPorts() {
        return 1;
    }

    @Override
    public void listAutoDetectionMidiPortNames(final AutoDetectionMidiPortNamesList list,
                                               final PlatformType platformType) {
        final String[] inputNames = new String[1];
        final String[] outputNames = new String[1];

        switch (platformType) {
            case LINUX:
                inputNames[0] = "XONE:K2";
                outputNames[0] = "XONE:K2";
                break;

            case WINDOWS:
                inputNames[0] = "XONE:K2";
                outputNames[0] = "XONE:K2";
                break;

            case MAC:
                inputNames[0] = "XONE:K2";
                outputNames[0] = "XONE:K2";
                break;
        }

        list.add(inputNames, outputNames);
    }

    @Override
    public AllenHeathK2ControllerExtension createInstance(final ControllerHost host) {
        return new AllenHeathK2ControllerExtension(this, host);
    }
}
