package com.traktor.bridge;

import java.util.HashMap;
import java.util.Map;

public class ScaleEngine {
    private int currentRoot = 60;
    private boolean isMinor = false;
    private String currentCamelot = "8d";
    private boolean isEnabled = true;
    
    private static final int[] MAJOR_SCALE = {0, 2, 4, 5, 7, 9, 11};
    private static final int[] MINOR_SCALE = {0, 2, 3, 5, 7, 8, 10};
    
    private static final Map<String, Integer> CAMELOT_TO_ROOT = new HashMap<>();
    
    static {
        CAMELOT_TO_ROOT.put("1d", 59);  CAMELOT_TO_ROOT.put("2d", 66);
        CAMELOT_TO_ROOT.put("3d", 61);  CAMELOT_TO_ROOT.put("4d", 68);
        CAMELOT_TO_ROOT.put("5d", 63);  CAMELOT_TO_ROOT.put("6d", 70);
        CAMELOT_TO_ROOT.put("7d", 65);  CAMELOT_TO_ROOT.put("8d", 60);
        CAMELOT_TO_ROOT.put("9d", 67);  CAMELOT_TO_ROOT.put("10d", 62);
        CAMELOT_TO_ROOT.put("11d", 69); CAMELOT_TO_ROOT.put("12d", 64);
        
        CAMELOT_TO_ROOT.put("1m", 68);  CAMELOT_TO_ROOT.put("2m", 63);
        CAMELOT_TO_ROOT.put("3m", 70);  CAMELOT_TO_ROOT.put("4m", 65);
        CAMELOT_TO_ROOT.put("5m", 60);  CAMELOT_TO_ROOT.put("6m", 67);
        CAMELOT_TO_ROOT.put("7m", 62);  CAMELOT_TO_ROOT.put("8m", 69);
        CAMELOT_TO_ROOT.put("9m", 64);  CAMELOT_TO_ROOT.put("10m", 59);
        CAMELOT_TO_ROOT.put("11m", 66); CAMELOT_TO_ROOT.put("12m", 61);
    }
    
    public ScaleEngine() {
        System.out.println("ScaleEngine initialized");
    }
    
    public void setKey(String camelot) {
        if (camelot == null || camelot.isEmpty()) return;
        String normalized = camelot.toLowerCase().trim();
        if (normalized.matches("\\d+[ab]")) {
            // Support classic Camelot notation: A=minor, B=major
            normalized = normalized.substring(0, normalized.length() - 1)
                + (normalized.endsWith("a") ? "m" : "d");
        }
        if (!CAMELOT_TO_ROOT.containsKey(normalized)) {
            System.out.println("Unknown Camelot key: " + camelot);
            return;
        }
        currentCamelot = normalized;
        currentRoot = CAMELOT_TO_ROOT.get(normalized);
        isMinor = normalized.contains("m");
        System.out.println("ScaleEngine: Key set to " + camelot + 
                          " (Root: " + currentRoot + ", " + (isMinor ? "Minor" : "Major") + ")");
    }
    
    public int quantizeNote(int midiNote) {
        if (!isEnabled) return midiNote;
        int octave = midiNote / 12;
        int noteInOctave = midiNote % 12;
        int[] scale = isMinor ? MINOR_SCALE : MAJOR_SCALE;
        
        int bestMatch = scale[0];
        int bestDiff = Integer.MAX_VALUE;
        for (int scaleNote : scale) {
            int diff = Math.abs(noteInOctave - scaleNote);
            if (diff < bestDiff) {
                bestDiff = diff;
                bestMatch = scaleNote;
            }
        }
        
        int rootOffset = currentRoot - 60;
        int correctedNote = octave * 12 + bestMatch + rootOffset;
        correctedNote = Math.max(0, Math.min(127, correctedNote));
        return correctedNote;
    }
    
    public boolean isEnabled() { return isEnabled; }
    public void setEnabled(boolean enabled) { isEnabled = enabled; }
    public String getCurrentKey() { return currentCamelot; }
    public int getCurrentRoot() { return currentRoot; }
    public boolean isMinor() { return isMinor; }
}