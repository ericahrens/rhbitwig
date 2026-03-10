package com.yaeltex.djcontroller;

import java.util.UUID;

import com.bitwig.extension.api.PlatformType;
import com.bitwig.extension.controller.AutoDetectionMidiPortNamesList;
import com.bitwig.extension.controller.ControllerExtensionDefinition;
import com.bitwig.extension.controller.api.ControllerHost;

public class DjControllerExtensionDefinition extends ControllerExtensionDefinition {
    private static final UUID DRIVER_ID = UUID.fromString("dcd5c5e7-a62f-40a1-a372-fcbee7eA1681");
    
    public DjControllerExtensionDefinition() {
    }
    
    @Override
    public String getName() {
        return "DJ Controller";
    }
    
    @Override
    public String getAuthor() {
        return "R.Hawtin/E.Ahrens";
    }
    
    @Override
    public String getVersion() {
        return "0.2";
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
        return "DJCONTROLLER";
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
        return 2;
    }
    
    @Override
    public int getNumMidiOutPorts() {
        return 2;
    }
    
    @Override
    public void listAutoDetectionMidiPortNames(final AutoDetectionMidiPortNamesList list,
        final PlatformType platformType) { // SSL V-MIDI Port 1
        list.add(new String[] {"RHDJ 1", "RHDJ 2"}, new String[] {"RHDJ 1", "RHDJ 2"});
        //list.add(new String[] {"RHDJ 1", "SSL V-MIDI Port 1"}, new String[] {"RHDJ 1", "SSL V-MIDI Port 1"});
    }
    
    @Override
    public DjControllerExtension createInstance(final ControllerHost host) {
        return new DjControllerExtension(this, host);
    }
}
