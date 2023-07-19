package com.novation.launchpadProMk3;

import com.bitwig.extension.api.PlatformType;
import com.bitwig.extension.controller.AutoDetectionMidiPortNamesList;
import com.bitwig.extension.controller.ControllerExtensionDefinition;
import com.bitwig.extension.controller.api.ControllerHost;

import java.util.UUID;

public class LaunchPadProMk3ExtensionDefinition extends ControllerExtensionDefinition {
    private static final UUID DRIVER_ID = UUID.fromString("3c6b9cd4-0ffd-11ec-82a8-0242ac130003");

    public LaunchPadProMk3ExtensionDefinition() {
    }

    @Override
    public String getName() {
        return "Launchpad Drum Sequencer";
    }

    @Override
    public String getAuthor() {
        return "R.Hawtin/E.Ahrens";
    }

    @Override
    public String getVersion() {
        return "1.04";
    }

    @Override
    public UUID getId() {
        return DRIVER_ID;
    }

    @Override
    public String getHardwareVendor() {
        return "Novation";
    }

    @Override
    public String getHardwareModel() {
        return "Launchpad Pro Mk3";
    }

    @Override
    public String getHelpFilePath() {
        return "Controllers/Novation/Hawtin Novation LaunchPad Pro MKIII.pdf";
    }

    @Override
    public int getRequiredAPIVersion() {
        return 18;
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
                inputNames[0] = "LPProMK3 MIDI";
                outputNames[0] = "LPProMK3 MIDI";
                break;

            case WINDOWS:
                inputNames[0] = "LPProMK3 MIDI";
                outputNames[0] = "LPProMK3 MIDI";
                break;

            case MAC:
                inputNames[0] = "Launchpad Pro MK3 LPProMK3 MIDI";
                outputNames[0] = "Launchpad Pro MK3 LPProMK3 MIDI";
                break;
        }

        list.add(inputNames, outputNames);
    }

    @Override
    public LaunchpadProMk3ControllerExtension createInstance(final ControllerHost host) {
        return new LaunchpadProMk3ControllerExtension(this, host);
    }
}
