package com.yaeltex.seqarp168mk2;

import java.util.UUID;

import com.bitwig.extension.api.PlatformType;
import com.bitwig.extension.controller.AutoDetectionMidiPortNamesList;
import com.bitwig.extension.controller.ControllerExtensionDefinition;
import com.bitwig.extension.controller.api.ControllerHost;

public class SeqArp168ExtensionDefinition extends ControllerExtensionDefinition {
    private static final UUID DRIVER_ID = UUID.fromString("dcd5c5e7-a62f-40a1-a372-fcbee7e91680");
    
    public SeqArp168ExtensionDefinition() {
    }
    
    @Override
    public String getName() {
        return "SEQARP168 Mk2";
    }
    
    @Override
    public String getAuthor() {
        return "R.Hawtin/E.Ahrens";
    }
    
    @Override
    public String getVersion() {
        return "0.1";
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
    public String getSupportFolderPath() {
        return "Controllers/Yaeltex/";
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
        if (platformType == PlatformType.WINDOWS) {
            list.add(new String[] {"SEQARP168"}, new String[] {"SEQARP168"});
        } else if (platformType == PlatformType.MAC) {
            list.add(new String[] {"SEQARP168"}, new String[] {"SEQARP168"});
        } else if (platformType == PlatformType.LINUX) {
            list.add(new String[] {"SEQARP168"}, new String[] {"SEQARP168"});
        }
    }
    
    @Override
    public SeqArp168Extension createInstance(final ControllerHost host) {
        return new SeqArp168Extension(this, host);
    }
}
