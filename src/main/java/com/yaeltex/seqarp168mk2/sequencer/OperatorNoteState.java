package com.yaeltex.seqarp168mk2.sequencer;

import java.util.HashMap;
import java.util.List;
import java.util.Objects;

import com.akai.fire.sequence.IntSetValue;
import com.bitwig.extension.controller.api.NoteStep;
import com.bitwig.extensions.framework.values.IntValueObject;
import com.yaeltex.common.YaeltexMidiProcessor;

public class OperatorNoteState {
    private static final int[] LINE_MARKINGS = {10, 20, 30, 40, 45, 50, 58, 64, 73, 83, 94, 100, 112, 127};
    private final NoteStep[] assignments;
    private final IntSetValue heldSteps = new IntSetValue();
    private final NoteDoubleValueHandler panValue =
        new NoteDoubleValueHandler(note -> (note.pan() + 1) / 2, (note, value) -> note.setPan(value));
    private final NoteDoubleValueHandler randomValue =
        new NoteDoubleValueHandler(note -> note.chance(), (note, value) -> note.setChance(value));
    private final NoteDoubleValueHandler velocityValue =
        new NoteDoubleValueHandler(note -> note.velocity(), (note, value) -> note.setVelocity(value));
    private final NoteDoubleValueHandler velcurveValue = new NoteDoubleValueHandler(note -> note.repeatVelocityCurve(),
        (note, value) -> note.setRepeatVelocityCurve(value));
    private final NoteDoubleValueHandler pressureValue =
        new NoteDoubleValueHandler(note -> note.pressure(), (note, value) -> note.setPressure(value));
    private final NoteDoubleValueHandler timbreValue =
        new NoteDoubleValueHandler(note -> note.timbre(), (note, value) -> note.setTimbre(value));
    
    private final IntValueObject ratchetValue = new IntValueObject(0, 0, 127);
    private final IntValueObject noteLength = new IntValueObject(0, 0, 127);
    
    private boolean modified = false;
    private final HashMap<Integer, NoteStep> expectedNoteChanges = new HashMap<>();
    private long firstHoldTime = -1;
    private final YaeltexMidiProcessor midiProcessor;
    private boolean editMode = false;
    private boolean pendingEditMode = true;
    
    @FunctionalInterface
    private interface NoteGetDouble {
        double get(NoteStep note);
    }
    
    @FunctionalInterface
    private interface NoteSetDouble {
        void set(NoteStep note, double value);
    }
    
    private abstract static class NoteValueHandler {
        protected final IntValueObject valueObject = new IntValueObject(0, 0, 127);
        
        public abstract void modify(List<NoteStep> notes, int inc);
        
        public void set(final int value) {
            valueObject.set(0);
        }
        
        public IntValueObject getValueObject() {
            return valueObject;
        }
    }
    
    private static class NoteDoubleValueHandler extends NoteValueHandler {
        private final NoteGetDouble accessor;
        private final NoteSetDouble setter;
        
        public NoteDoubleValueHandler(final NoteGetDouble accessor, final NoteSetDouble setter) {
            this.accessor = accessor;
            this.setter = setter;
        }
        
        public void modify(final List<NoteStep> notes, final int inc) {
            for (final NoteStep step : notes) {
                int value = (int) (accessor.get(step) * 127);
                value = value + inc;
                if (value >= 0 && value < 128) {
                    final double newValue = (double) value / 127.0;
                    setter.set(step, newValue);
                    step.setChance(newValue);
                    valueObject.set((int) (newValue * 127));
                }
            }
        }
        
        public IntValueObject getValueObject() {
            return valueObject;
        }
        
        public void apply(final NoteStep step) {
            valueObject.set((int) (accessor.get(step) * 127));
        }
    }
    
    public OperatorNoteState(final NoteStep[] assignments, final YaeltexMidiProcessor host) {
        this.assignments = assignments;
        this.midiProcessor = host;
    }
    
    public void addStep(final int index) {
        heldSteps.add(index);
        if (heldSteps.size() == 1) {
            firstHoldTime = System.currentTimeMillis();
            pendingEditMode = true;
            midiProcessor.delayAction(this::intoEditAction, 500);
        }
        updateStateValues();
    }
    
    public void removeStep(final int index) {
        heldSteps.remove(index);
        if (heldSteps.size() == 0) {
            firstHoldTime = -1;
            editMode = false;
            pendingEditMode = false;
        }
        updateStateValues();
    }
    
    private void intoEditAction() {
        if (pendingEditMode) {
            editMode = true;
        }
    }
    
    private void updateStateValues() {
        final List<NoteStep> selectedNotes = getSelectedNoteSteps();
        if (selectedNotes.size() == 0) {
            randomValue.set(0);
            ratchetValue.set(0);
            velocityValue.set(0);
            velcurveValue.set(0);
            timbreValue.set(0);
            pressureValue.set(0);
            panValue.set(0);
            modified = false;
        } else if (selectedNotes.size() == 1) {
            final NoteStep step = selectedNotes.get(0);
            randomValue.apply(step);
            pressureValue.apply(step);
            timbreValue.apply(step);
            velocityValue.apply(step);
            velcurveValue.apply(step);
            panValue.apply(step);
            ratchetValue.set(toRatchetValue(step.repeatCount()));
        }
    }
    
    private static int toRatchetValue(final int value) {
        if (value < LINE_MARKINGS.length - 1) {
            return LINE_MARKINGS[value];
        }
        return 127;
    }
    
    private List<NoteStep> getSelectedNoteSteps() {
        return heldSteps.stream() //
            .map(index -> assignments[index])//
            .filter(Objects::nonNull) //
            .toList();
    }
    
    public IntValueObject getRandomValue() {
        return randomValue.getValueObject();
    }
    
    public IntValueObject getRatchetValue() {
        return ratchetValue;
    }
    
    public IntValueObject getTimbreValue() {
        return timbreValue.getValueObject();
    }
    
    public IntValueObject getVelocityValue() {
        return velocityValue.getValueObject();
    }
    
    public IntValueObject getPressureValue() {
        return pressureValue.getValueObject();
    }
    
    public IntValueObject getVelcurveValue() {
        return velcurveValue.getValueObject();
    }
    
    public boolean editMode() {
        return editMode;
    }
    
    public IntValueObject getNoteLength() {
        return noteLength;
    }
    
    public void modifyRandom(final int inc) {
        if (!heldSteps.hasValues()) {
            return;
        }
        randomValue.modify(getSelectedNoteSteps(), inc);
        modified = true;
    }
    
    public void modifyVelcurve(final int inc) {
        if (!heldSteps.hasValues()) {
            return;
        }
        velcurveValue.modify(getSelectedNoteSteps(), inc);
        
        modified = true;
    }
    
    public void modifyVelocity(final int inc) {
        if (!heldSteps.hasValues()) {
            return;
        }
        velocityValue.modify(getSelectedNoteSteps(), inc);
        modified = true;
    }
    
    public void modifyTimbre(final int inc) {
        if (!heldSteps.hasValues()) {
            return;
        }
        timbreValue.modify(getSelectedNoteSteps(), inc);
        modified = true;
    }
    
    public void modifyPressure(final int inc) {
        if (!heldSteps.hasValues()) {
            return;
        }
        pressureValue.modify(getSelectedNoteSteps(), inc);
        modified = true;
    }
    
    public void modifyRatchet(final int inc) {
        if (!heldSteps.hasValues()) {
            return;
        }
        final List<NoteStep> selected = getSelectedNoteSteps();
        for (final NoteStep step : selected) {
            final int value = step.repeatCount() + inc;
            if (value >= 0 && value < 128) {
                step.setRepeatCount(value);
                ratchetValue.set(toRatchetValue(value));
            }
        }
        modified = true;
    }
    
    public boolean editingOccured() {
        return modified || (firstHoldTime > 0 && (System.currentTimeMillis() - firstHoldTime) > 500);
    }
    
    public void registerChanges(final int pos, final NoteStep noteStep) {
        expectedNoteChanges.put(pos, noteStep);
    }
    
    public void handleNewStep(final NoteStep noteStep) {
        final int newStep = noteStep.x();
        if (expectedNoteChanges.containsKey(newStep)) {
            final NoteStep previousStep = expectedNoteChanges.get(newStep);
            expectedNoteChanges.remove(newStep);
            applyValues(noteStep, previousStep);
        }
    }
    
    private void applyValues(final NoteStep dest, final NoteStep src) {
        dest.setChance(src.chance());  // ************
        dest.setTimbre(src.timbre()); // ************
        dest.setPressure(src.pressure());  // TODO
        dest.setRepeatCount(src.repeatCount());  // ************
        dest.setRepeatVelocityCurve(src.repeatVelocityCurve());
        dest.setRepeatVelocityEnd(src.repeatVelocityEnd());
        dest.setPan(src.pan());  // TODO
        dest.setRecurrence(src.recurrenceLength(), src.recurrenceMask());
        dest.setOccurrence(src.occurrence());
        // TODO NOTE Length
    }
    
}
