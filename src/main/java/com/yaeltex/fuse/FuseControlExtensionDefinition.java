package com.yaeltex.fuse;

import java.util.UUID;

import com.bitwig.extension.api.PlatformType;
import com.bitwig.extension.controller.AutoDetectionMidiPortNamesList;
import com.bitwig.extension.controller.ControllerExtensionDefinition;
import com.bitwig.extension.controller.api.ControllerHost;

public class FuseControlExtensionDefinition extends ControllerExtensionDefinition {
    private static final UUID DRIVER_ID = UUID.fromString("e69c13a1-34d0-43c9-bbb3-260e83d8ac02");
    
    public FuseControlExtensionDefinition() {
    }
    
    @Override
    public String getName() {
        return "Fuse";
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
        return "Fuse";
    }
    
    @Override
    public String getHelpFilePath() {
        return null;
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
        list.add(new String[] {"FUSE"}, new String[] {"FUSE"});
        list.add(new String[] {"Launch Control XL"}, new String[] {"Launch Control XL"});
    }
    
    @Override
    public FuseExtension createInstance(final ControllerHost host) {
        return new FuseExtension(this, host);
    }
}
