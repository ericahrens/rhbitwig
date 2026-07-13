package com.traktor.bridge;

import com.bitwig.extension.controller.ControllerExtension;
import com.bitwig.extension.controller.ControllerExtensionDefinition;
import com.bitwig.extension.controller.AutoDetectionMidiPortNamesList;
import com.bitwig.extension.api.PlatformType;
import com.bitwig.extension.controller.api.ControllerHost;

import java.util.UUID;

public class TraktorBitwigBridgeExtensionDefinition extends ControllerExtensionDefinition
{
   private static final UUID DRIVER_ID = UUID.fromString("12345678-1234-1234-1234-123456789abc");
   private static final String DRIVER_NAME = "Traktor Bitwig Bridge";
   private static final String DRIVER_AUTHOR = "Traktor Bridge";
   private static final String DRIVER_VERSION = "0.2";
   
   @Override
   public UUID getId() { return DRIVER_ID; }
   @Override
   public String getName() { return DRIVER_NAME; }
   @Override
   public String getAuthor() { return DRIVER_AUTHOR; }
   @Override
   public String getVersion() { return DRIVER_VERSION; }
   @Override
   public int getRequiredAPIVersion() { return 24; }
   @Override
   public int getNumMidiInPorts() { return 1; }
   @Override
   public int getNumMidiOutPorts() { return 1; }
   
   @Override
   public ControllerExtension createInstance(final ControllerHost host) {
      return new TraktorBitwigBridgeExtension(this, host);
   }
   
   @Override
   public void listAutoDetectionMidiPortNames(final AutoDetectionMidiPortNamesList list, final PlatformType platformType) {
      switch (platformType) {
         case MAC:
            list.add(new String[]{"IAC Driver"}, new String[]{"IAC Driver"});
            break;
         default:
            list.add(new String[]{"Traktor Virtual In"}, new String[]{"Traktor Virtual Out"});
            break;
      }
   }
   
   @Override
   public String getHardwareModel() { return "Traktor Bitwig Bridge"; }
   @Override
   public String getHardwareVendor() { return "Traktor Bridge"; }
}
