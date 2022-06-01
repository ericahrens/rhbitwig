package com.yaeltex;

import com.bitwig.extension.api.PlatformType;
import com.bitwig.extension.controller.AutoDetectionMidiPortNamesList;
import com.bitwig.extension.controller.ControllerExtensionDefinition;
import com.bitwig.extension.controller.api.ControllerHost;

import java.util.UUID;

public class YaeltexArpControlExtensionDefinition extends ControllerExtensionDefinition {
    private static final UUID DRIVER_ID = UUID.fromString("dcd5c5e7-b52f-40a0-9372-fcbee7e91687");

    public YaeltexArpControlExtensionDefinition() {
    }

    @Override
    public String getName() {
        return "YaeltexArpControl";
    }

    @Override
    public String getAuthor() {
        return "R.Hawtin/E.Ahrens";
    }

    @Override
    public String getVersion() {
        return "0.5";
    }

    @Override
    public UUID getId() {
        return DRIVER_ID;
    }

    @Override
    public String getHardwareVendor() {
        return "Yaeltex";
    }

    @Override
    public String getHardwareModel() {
        return "SEQARP168";
    }

    @Override
    public String getHelpFilePath() {
        return "Controllers/Yaeltex/Hawtin Yaeltex SEQ ARP 168.pdf";
    }

    @Override
    public String getSupportFolderPath() {
        return "Controllers/Yaeltex/";
    }

    @Override
    public int getRequiredAPIVersion() {
        return 13;
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
        if (platformType == PlatformType.WINDOWS) {
            list.add(new String[]{"SEQARP168"}, new String[]{"SEQARP168"});
        } else if (platformType == PlatformType.MAC) {
            list.add(new String[]{"SEQARP168"}, new String[]{"SEQARP168"});
        } else if (platformType == PlatformType.LINUX) {
            list.add(new String[]{"SEQARP168"}, new String[]{"SEQARP168"});
        }
    }

    @Override
    public YaeltexArpControlExtension createInstance(final ControllerHost host) {
        return new YaeltexArpControlExtension(this, host);
    }
}
