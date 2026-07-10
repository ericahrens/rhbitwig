package com.akai.fire.sequence;

import java.util.ArrayList;
import java.util.List;

import com.bitwig.extension.controller.api.NoteOccurrence;
import com.bitwig.extension.controller.api.NoteStep;

public class ClipboardManager {
    
    public static class ClippedNote {
        public int x;
        public double velocity;
        public double pan;
        public double timbre;
        public double pressure;
        public double chance;
        public int repeatCount;
        public double repeatCurve;
        public double repeatVelocityCurve;
        public double repeatVelocityEnd;
        public int recurrenceLength;
        public int recurrenceMask;
        public NoteOccurrence occurrence;
        public double duration;
        public double velocitySpread;
        public int transpose;
        
        public ClippedNote(NoteStep note) {
            this.x = note.x();
            this.velocity = note.velocity();
            this.pan = note.pan();
            this.timbre = note.timbre();
            this.pressure = note.pressure();
            this.chance = note.chance();
            this.repeatCount = note.repeatCount();
            this.repeatCurve = note.repeatCurve();
            this.repeatVelocityCurve = note.repeatVelocityCurve();
            this.repeatVelocityEnd = note.repeatVelocityEnd();
            this.recurrenceLength = note.recurrenceLength();
            this.recurrenceMask = note.recurrenceMask();
            this.occurrence = note.occurrence();
            this.duration = note.duration();
            this.velocitySpread = note.velocitySpread();
            this.transpose = 0;
        }
    }
    
    private List<ClippedNote> clipboard = new ArrayList<>();
    private boolean hasContent = false;
    
    public void copyNotes(List<NoteStep> notes) {
        clipboard.clear();
        for (NoteStep note : notes) {
            if (note != null) {
                clipboard.add(new ClippedNote(note));
            }
        }
        hasContent = !clipboard.isEmpty();
    }
    
    public List<ClippedNote> getClipboard() {
        return clipboard;
    }
    
    public boolean hasContent() {
        return hasContent;
    }
    
    public void clear() {
        clipboard.clear();
        hasContent = false;
    }
    
    public int size() {
        return clipboard.size();
    }
}