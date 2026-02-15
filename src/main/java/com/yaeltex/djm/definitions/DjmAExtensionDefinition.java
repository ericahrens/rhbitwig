package com.yaeltex.djm.definitions;

import java.util.UUID;

import com.bitwig.extension.api.PlatformType;
import com.bitwig.extension.controller.AutoDetectionMidiPortNamesList;
import com.bitwig.extension.controller.ControllerExtensionDefinition;
import com.bitwig.extension.controller.api.ControllerHost;
import com.yaeltex.djm.extensions.DjmAControllerExtension;

public class DjmAExtensionDefinition extends ControllerExtensionDefinition {
    private static final UUID DRIVER_ID = UUID.fromString("e69c13a1-34d0-f3c7-1bb3-260e83d8ac02");
    
    public DjmAExtensionDefinition() {
    }
    
    @Override
    public String getName() {
        return "DJM A";
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
        return "DJM A";
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
        return 24;
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
        list.add(new String[] {"DJM A"}, new String[] {"DJM A"});
    }
    
    @Override
    public DjmAControllerExtension createInstance(final ControllerHost host) {
        return new DjmAControllerExtension(this, host);
    }
}
