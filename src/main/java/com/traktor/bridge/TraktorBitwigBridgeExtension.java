package com.traktor.bridge;

import com.bitwig.extension.controller.ControllerExtension;
import com.bitwig.extension.controller.ControllerExtensionDefinition;
import com.bitwig.extension.controller.api.ControllerHost;
import com.bitwig.extension.controller.api.MidiIn;
import com.bitwig.extension.controller.api.MidiOut;

public class TraktorBitwigBridgeExtension extends ControllerExtension
{
   private ControllerHost host;
   private MidiIn midiIn;
   private MidiOut midiOut;
   private DeckManager deckManager;
   private HttpBridgeServer httpServer;

   protected TraktorBitwigBridgeExtension(final ControllerExtensionDefinition definition,
                                          final ControllerHost host)
   {
      super(definition, host);
   }

   @Override
   public void init()
   {
      host = getHost();

      host.println("========================================");
      host.println("TRAKTOR BITWIG BRIDGE");
      host.println("========================================");

      midiIn = host.getMidiInPort(0);
      midiOut = host.getMidiOutPort(0);

      host.println("MIDI Input: " + (midiIn != null ? "OK" : "NULL"));
      host.println("MIDI Output: " + (midiOut != null ? "OK" : "NULL"));

      deckManager = new DeckManager(host, midiOut);

      httpServer = new HttpBridgeServer(deckManager);
      httpServer.start();

      if (midiIn != null) {
         midiIn.setMidiCallback(this::onMidi);
         midiIn.setSysexCallback(this::onSysex);
         host.println("MIDI callback SET!");
      }

      host.println("========================================");
      host.println("READY!");
      host.println("  HTTP: http://localhost:8080");
      host.println("========================================");
   }

   private void onMidi(int status, int data1, int data2) {
      host.println(">>> MIDI IN: " + status + " " + data1 + " " + data2);
      if (deckManager != null) {
         deckManager.handleMidiMessage(status, data1, data2);
      }
   }

   private void onSysex(String data) {
      host.println(">>> SYSEX: " + data);
   }

   @Override
   public void flush() {}

   @Override
   public void exit() {
      host.println("Stopping Traktor Bitwig Bridge...");
      if (httpServer != null) httpServer.stop();
      if (deckManager != null) deckManager.shutdown();
      host.println("Stopped.");
   }

   public ControllerHost getBridgeHost() { return host; }
   public MidiOut getMidiOut() { return midiOut; }
   public DeckManager getDeckManager() { return deckManager; }
}