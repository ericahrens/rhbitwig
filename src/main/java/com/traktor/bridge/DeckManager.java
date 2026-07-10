package com.traktor.bridge;

import com.bitwig.extension.controller.api.ControllerHost;
import com.bitwig.extension.controller.api.MidiOut;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

public class DeckManager
{
   private static final int BITWIG_SEMITONE_MIN = -48;
   private static final int BITWIG_SEMITONE_MAX = 48;
   private static final double BITWIG_CC_MAX = 127.0;

   private final ControllerHost host;
   private final MidiOut midiOut;
   private final VirtualMidiInput virtualMidi;
   private boolean isRunning;
   
   private final String[] deckKeys = new String[4];
   private final int[] deckKeyNotes = new int[4];
   private final int[] deckTransposeValues = new int[4];
   private final ScaleEngine scaleEngine;
   private int activeDeck = 0;
   private boolean[] deckActive = new boolean[4];
   
   private static final int[] K2_BUTTONS = {48, 51, 48, 51};
   private static final int[] K2_CHANNELS = {14, 14, 15, 15};
   private static final String[] DECK_NAMES = {"A", "B", "C", "D"};
   
   private static final Map<String, String> CAMELOT_TO_KEY = new HashMap<>();
   private static final Map<String, Integer> CAMELOT_TO_NOTE = new HashMap<>();
   
   static {
      CAMELOT_TO_KEY.put("1d", "B");    CAMELOT_TO_NOTE.put("1d", 59);
      CAMELOT_TO_KEY.put("2d", "F#");   CAMELOT_TO_NOTE.put("2d", 66);
      CAMELOT_TO_KEY.put("3d", "C#");   CAMELOT_TO_NOTE.put("3d", 61);
      CAMELOT_TO_KEY.put("4d", "G#");   CAMELOT_TO_NOTE.put("4d", 68);
      CAMELOT_TO_KEY.put("5d", "D#");   CAMELOT_TO_NOTE.put("5d", 63);
      CAMELOT_TO_KEY.put("6d", "A#");   CAMELOT_TO_NOTE.put("6d", 70);
      CAMELOT_TO_KEY.put("7d", "F");    CAMELOT_TO_NOTE.put("7d", 65);
      CAMELOT_TO_KEY.put("8d", "C");    CAMELOT_TO_NOTE.put("8d", 60);
      CAMELOT_TO_KEY.put("9d", "G");    CAMELOT_TO_NOTE.put("9d", 67);
      CAMELOT_TO_KEY.put("10d", "D");   CAMELOT_TO_NOTE.put("10d", 62);
      CAMELOT_TO_KEY.put("11d", "A");   CAMELOT_TO_NOTE.put("11d", 69);
      CAMELOT_TO_KEY.put("12d", "E");   CAMELOT_TO_NOTE.put("12d", 64);
      
      CAMELOT_TO_KEY.put("1m", "G#");   CAMELOT_TO_NOTE.put("1m", 68);
      CAMELOT_TO_KEY.put("2m", "D#");   CAMELOT_TO_NOTE.put("2m", 63);
      CAMELOT_TO_KEY.put("3m", "A#");   CAMELOT_TO_NOTE.put("3m", 70);
      CAMELOT_TO_KEY.put("4m", "F");    CAMELOT_TO_NOTE.put("4m", 65);
      CAMELOT_TO_KEY.put("5m", "C");    CAMELOT_TO_NOTE.put("5m", 60);
      CAMELOT_TO_KEY.put("6m", "G");    CAMELOT_TO_NOTE.put("6m", 67);
      CAMELOT_TO_KEY.put("7m", "D");    CAMELOT_TO_NOTE.put("7m", 62);
      CAMELOT_TO_KEY.put("8m", "A");    CAMELOT_TO_NOTE.put("8m", 69);
      CAMELOT_TO_KEY.put("9m", "E");    CAMELOT_TO_NOTE.put("9m", 64);
      CAMELOT_TO_KEY.put("10m", "B");   CAMELOT_TO_NOTE.put("10m", 59);
      CAMELOT_TO_KEY.put("11m", "F#");  CAMELOT_TO_NOTE.put("11m", 66);
      CAMELOT_TO_KEY.put("12m", "C#");  CAMELOT_TO_NOTE.put("12m", 61);
   }
   
   private static final String[] NOTES = {"C", "C#", "D", "D#", "E", "F", "F#", "G", "G#", "A", "A#", "B"};
   
   public DeckManager(final ControllerHost host, final MidiOut midiOut)
   {
      this.host = host;
      this.midiOut = midiOut;
      this.isRunning = true;
      this.scaleEngine = new ScaleEngine();
      this.virtualMidi = new VirtualMidiInput();
      
      for (int i = 0; i < deckKeys.length; i++) {
         deckKeys[i] = "C";
         deckKeyNotes[i] = 60;
         deckTransposeValues[i] = 0;
         deckActive[i] = false;
      }
      deckActive[0] = true;
      
      host.println("========================================");
      host.println("DeckManager - FINAL");
      host.println("  CC16=A, CC17=B, CC18=C, CC19=D");
      host.println("========================================");
   }
   
   public void setDeckKey(final int deckIndex, final String key)
   {
      if (deckIndex < 0 || deckIndex >= deckKeys.length) return;
      if (key == null || key.isEmpty()) return;
      
      String cleanKey = key.replace("~", "").trim();
      
      String standardKey = cleanKey;
      int midiNote = 60;
      String normalizedKey = cleanKey.toLowerCase().trim();
      if (normalizedKey.matches("\\d+[ab]")) {
         // Support classic Camelot notation: A=minor, B=major
         normalizedKey = normalizedKey.substring(0, normalizedKey.length() - 1)
         + (normalizedKey.endsWith("a") ? "m" : "d");
      }
      
      if (normalizedKey.matches("\\d+[dm]")) {
         if (CAMELOT_TO_KEY.containsKey(normalizedKey)) {
            standardKey = CAMELOT_TO_KEY.get(normalizedKey);
            midiNote = CAMELOT_TO_NOTE.get(normalizedKey);
         }
      } else {
         for (int i = 0; i < NOTES.length; i++) {
            if (NOTES[i].equalsIgnoreCase(normalizedKey)) {
               midiNote = 60 + i;
               standardKey = NOTES[i];
               break;
            }
         }
      }
      
      if (deckKeys[deckIndex].equals(standardKey) && deckKeyNotes[deckIndex] == midiNote) {
         return;
      }
      
      deckKeys[deckIndex] = standardKey;
      deckKeyNotes[deckIndex] = midiNote;
      
      if (deckIndex == activeDeck) {
         scaleEngine.setKey(cleanKey);
      }
      
      int transposeAmount = midiNote - 60;
      deckTransposeValues[deckIndex] = transposeAmount;

      int semiValue = toBitwigSemitoneCc(transposeAmount);

      host.println("DECK " + (deckIndex + 1) + " KEY: " + cleanKey + " -> " + standardKey);
      host.println("  (active deck will send CC16 = " + semiValue + ")");

      if (deckIndex == activeDeck) {
         host.println("SEND ACTIVE KEY -> deck=" + DECK_NAMES[deckIndex] + " cc=16 value=" + semiValue + " status=185");
         virtualMidi.sendMidi(0xB9, 16, semiValue);
         sendMidiMessage(0xB9, 16, semiValue);
      }
   }
   
   private void setActiveDeck(int deckIndex) {
      if (deckIndex < 0 || deckIndex >= 4) return;
      
      if (deckActive[deckIndex]) {
         return;
      }
      
      for (int i = 0; i < 4; i++) {
         deckActive[i] = false;
      }
      
      deckActive[deckIndex] = true;
      activeDeck = deckIndex;
      
      int transposeAmount = deckTransposeValues[deckIndex];
      int semiValue = toBitwigSemitoneCc(transposeAmount);
      host.println("SEND DECK SELECT -> deck=" + DECK_NAMES[deckIndex] + " cc=16 value=" + semiValue + " status=185");
      virtualMidi.sendMidi(0xB9, 16, semiValue);
      sendMidiMessage(0xB9, 16, semiValue);
      
      scaleEngine.setKey(deckKeys[deckIndex]);
      
      host.println("========================================");
      host.println("ACTIVE DECK: " + DECK_NAMES[deckIndex]);
      host.println("  Key: " + deckKeys[deckIndex]);
      host.println("  CC16 = " + semiValue);
      host.println("========================================");
   }
   
   public void handleMidiMessage(final int status, final int data1, final int data2) {
      final int command = status & 0xF0;
      final int channel = (status & 0x0F) + 1;
      
      // XONE:K2 бутони (от IAC)
      if (command == 0x90 && data2 > 0) {
         for (int i = 0; i < 4; i++) {
            if (K2_BUTTONS[i] == data1 && K2_CHANNELS[i] == channel) {
               host.println(">>> K2: Deck " + DECK_NAMES[i]);
               setActiveDeck(i);
               return;
            }
         }
         return;
      }
      
      // NOTE OFF - игнорираме
      if (command == 0x80) {
         return;
      }
      
      // CC - игнорираме нашите CC16-19
      if (command == 0xB0) {
          if (data1 >= 16 && data1 <= 19) {
              return;
          }
          sendMidiMessage(status, data1, data2);
          return;
      }
      
      // Note On канал 10 - quantize
      if (command == 0x90 && data2 > 0 && channel == 10) {
         int originalNote = data1;
         int quantizedNote = scaleEngine.quantizeNote(originalNote);
         if (quantizedNote != originalNote) {
            host.println("Quantized: " + originalNote + " -> " + quantizedNote);
         }
         sendMidiMessage(0x99, quantizedNote, data2);
         return;
      }
      
      sendMidiMessage(status, data1, data2);
   }
   
   public void handleMessage(final String command, final String data) {
      try {
         host.println("Processing command: " + command + " with data: " + data);
         
         if ("setKey".equals(command)) {
            JSONObject json = new JSONObject(data);
            int deck = json.optInt("deck", 1) - 1;
            String key = json.optString("key", "C");
            setDeckKey(deck, key);
         } else if ("enableScale".equals(command)) {
            scaleEngine.setEnabled(true);
            host.println("Scale Engine ENABLED");
         } else if ("disableScale".equals(command)) {
            scaleEngine.setEnabled(false);
            host.println("Scale Engine DISABLED");
         } else if ("setActiveDeck".equals(command)) {
            int deck = Integer.parseInt(data.trim()) - 1;
            if (deck >= 0 && deck < 4) {
               setActiveDeck(deck);
            }
         }
      } catch (Exception e) {
         host.println("Error: " + e.getMessage());
      }
   }

   public void activateDeck(final int deckIndex) {
      setActiveDeck(deckIndex);
   }
   
   public void processMessage(final String jsonMessage) {
      try {
         JSONObject json = new JSONObject(jsonMessage);
         String command = json.getString("command");
         String data = json.optString("data", "");
         handleMessage(command, data);
      } catch (Exception e) {
         host.println("Error parsing JSON: " + e.getMessage());
      }
   }
   
   private void sendMidiMessage(final int status, final int data1, final int data2) {
      if (midiOut != null) {
         midiOut.sendMidi(status, data1, data2);
         host.println("  >>> SENT: " + status + " " + data1 + " " + data2);
      }
   }

   private int toBitwigSemitoneCc(final int semitone) {
      final int clamped = Math.max(BITWIG_SEMITONE_MIN, Math.min(BITWIG_SEMITONE_MAX, semitone));
      final double normalized = (clamped - BITWIG_SEMITONE_MIN) /
         (double) (BITWIG_SEMITONE_MAX - BITWIG_SEMITONE_MIN);
      return (int) Math.round(normalized * BITWIG_CC_MAX);
   }
   
   public void shutdown() {
      isRunning = false;
      if (virtualMidi != null) {
         virtualMidi.close();
      }
      host.println("DeckManager shutting down...");
   }
   
   public boolean isRunning() { return isRunning; }
}