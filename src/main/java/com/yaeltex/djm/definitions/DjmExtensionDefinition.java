package com.yaeltex.djm.definitions;

import java.util.UUID;

import com.bitwig.extension.api.PlatformType;
import com.bitwig.extension.controller.AutoDetectionMidiPortNamesList;
import com.bitwig.extension.controller.ControllerExtensionDefinition;
import com.bitwig.extension.controller.api.ControllerHost;
import com.yaeltex.djm.DjmControllerExtension;

public class DjmExtensionDefinition extends ControllerExtensionDefinition {
    private static final UUID DRIVER_ID = UUID.fromString("e69c13a1-34d0-f3c7-1bb3-260e83d8ac02");
    
    public DjmExtensionDefinition() {
    }
    
    @Override
    public String getName() {
        return "DJM";
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
        return "DJM";
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
        return 23;
    }
    
    @Override
    public int getNumMidiInPorts() {
        return 2;
    }
    
    @Override
    public int getNumMidiOutPorts() {
        return 2;
    }
    
    @Override
    public void listAutoDetectionMidiPortNames(final AutoDetectionMidiPortNamesList list,
        final PlatformType platformType) {
        list.add(new String[] {"DJM A", "DJM B"}, new String[] {"DJM A", "DJM B"});
    }
    
    @Override
    public DjmControllerExtension createInstance(final ControllerHost host) {
        return new DjmControllerExtension(this, host);
    }
}
