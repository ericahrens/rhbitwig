package com.yaeltex.seqarp168mk2.sequencer;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import com.bitwig.extension.controller.api.ClipLauncherSlot;
import com.bitwig.extension.controller.api.ClipLauncherSlotBank;
import com.bitwig.extension.controller.api.CursorTrack;
import com.bitwig.extension.controller.api.DrumPad;
import com.bitwig.extension.controller.api.DrumPadBank;
import com.bitwig.extension.controller.api.InternalHardwareLightState;
import com.bitwig.extension.controller.api.NoteStep;
import com.bitwig.extension.controller.api.PinnableCursorClip;
import com.bitwig.extension.controller.api.PlayingNote;
import com.bitwig.extensions.framework.Layer;
import com.bitwig.extensions.framework.Layers;
import com.bitwig.extensions.framework.di.Component;
import com.bitwig.extensions.framework.values.IntValueObject;
import com.bitwig.extensions.framework.values.StepViewPosition;
import com.yaeltex.common.YaelTexColors;
import com.yaeltex.common.YaeltexButtonLedState;
import com.yaeltex.common.YaeltexMidiProcessor;
import com.yaeltex.controls.RgbButton;
import com.yaeltex.controls.RingEncoder;
import com.yaeltex.seqarp168mk2.BitwigViewControl;
import com.yaeltex.seqarp168mk2.SeqArpHardwareElements;

@Component
public class SequencerLayer extends Layer {
    
    private static final YaeltexButtonLedState playColor = YaeltexButtonLedState.of(YaelTexColors.VIOLET_RED, 2);
    private final BitwigViewControl viewControl;
    private final boolean[] notesPlaying = new boolean[16];
    private final YaeltexButtonLedState[] padColors = new YaeltexButtonLedState[16];
    private final YaeltexButtonLedState[] padColorsAlt = new YaeltexButtonLedState[16];
    private final YaeltexButtonLedState[] slotColors = new YaeltexButtonLedState[8];
    private YaeltexButtonLedState trackColor;
    private int noteOffset = 0;
    private int selectedPadIndex = -1;
    private final YaeltexMidiProcessor midiProcessor;
    private final PinnableCursorClip clip;
    private final NoteStep[] assignments = new NoteStep[32];
    private final Set<Integer> addedSteps = new HashSet<>();
    private final Set<Integer> modifiedSteps = new HashSet<>();
    private int playingStep;
    private boolean markIgnoreOrigLen = false;
    private final double gatePercent = 0.98;
    private final StepViewPosition positionHandler;
    private final int nonAccentVelocity = 75;
    private final NotesState operatorNoteState;
    private final IntValueObject accentValue = new IntValueObject(127, 0, 127);
    private final Layer seqButtonLayer;
    private final Layer seqLengthLayer;
    private boolean accentHeld;
    private boolean copyHeld;
    private NoteStep copyNote;
    private boolean randomHeld;
    
    public SequencerLayer(final Layers layers, final SeqArpHardwareElements hwElements,
        final BitwigViewControl viewControl, final YaeltexMidiProcessor midiProcessor) {
        super(layers, "SEQUENCER_LAYER");
        this.viewControl = viewControl;
        this.midiProcessor = midiProcessor;
        this.clip = viewControl.getCursorClip();
        positionHandler = new StepViewPosition(clip, 32, "YAELTEX");
        this.operatorNoteState = new NotesState(assignments, positionHandler, midiProcessor);
        
        this.seqButtonLayer = new Layer(layers, "SEQ_BUTTON_LAYER");
        this.seqLengthLayer = new Layer(layers, "SEQ_LENGTH_LAYER");
        
        setupClip();
        final DrumPadBank drumPadBank = this.viewControl.getDrumPadBank();
        drumPadBank.scrollPosition().addValueObserver(position -> {
            this.noteOffset = position;
            if (selectedPadIndex != -1) {
                this.clip.scrollToKey(this.noteOffset + selectedPadIndex);
            }
        });
        final CursorTrack cursorTrack = this.viewControl.getCursorTrack();
        cursorTrack.color().addValueObserver((r, g, b) -> trackColor = YaeltexButtonLedState.of(r, g, b));
        cursorTrack.playingNotes().addValueObserver(notes -> handleNotesPlaying(notes));
        bindPadSelection(hwElements, drumPadBank);
        bindClipLaunching(hwElements, cursorTrack);
        bindStepSequencer(hwElements);
        bindOperators(hwElements);
        final RgbButton retriggButton = hwElements.getControlButton(2);
        retriggButton.bindPressed(this, () -> this.clip.launch());
        retriggButton.bindLight(this, this::retrigState);
    }
    
    private YaeltexButtonLedState retrigState() {
        if (clip.exists().get()) {
            if (clip.clipLauncherSlot().isPlaybackQueued().get()) {
                return midiProcessor.blinkMid(YaeltexButtonLedState.GREEN);
            }
            if (clip.clipLauncherSlot().isPlaying().get()) {
                return midiProcessor.blinkSlow(YaeltexButtonLedState.GREEN);
            }
            return YaeltexButtonLedState.GREEN;
        }
        return YaeltexButtonLedState.OFF;
    }
    
    private void bindPadSelection(final SeqArpHardwareElements hwElements, final DrumPadBank drumPadBank) {
        for (int i = 0; i < drumPadBank.getSizeOfBank(); i++) {
            final int index = i;
            final RingEncoder encoder = hwElements.getEncoder(i);
            final DrumPad drumPad = drumPadBank.getItemAt(i);
            drumPad.color().addValueObserver((r, g, b) -> {
                padColors[index] = YaeltexButtonLedState.of(r, g, b);
                padColorsAlt[index] = padColors[index].offset(2);
            });
            drumPad.addIsSelectedInEditorObserver(selected -> handlePadSelection(index, selected));
            encoder.getButton().bindLight(this, () -> getPadColorState(index));
            encoder.getButton().bindPressed(this, () -> drumPad.selectInEditor());
            encoder.bindLight(this, () -> getPadColor(index));
            final IntValueObject level = new IntValueObject(0, 0, 127);
            drumPad.addVuMeterObserver(127, -1, false, newValue -> level.set(newValue));
            encoder.bind(this, inc -> this.modifySteps(index, inc), level);
        }
    }
    
    private void bindClipLaunching(final SeqArpHardwareElements hwElements, final CursorTrack cursorTrack) {
        final ClipLauncherSlotBank slotBank = cursorTrack.clipLauncherSlotBank();
        for (int i = 0; i < slotBank.getSizeOfBank(); i++) {
            final int index = i;
            final ClipLauncherSlot slot = slotBank.getItemAt(i);
            
            prepareSlot(index, slot);
            final RingEncoder encoder = hwElements.getEncoder(i + 16);
            encoder.getButton().bindLight(this, () -> getSlotColor(index, slot));
            encoder.getButton().bindIsPressed(this, pressed -> handleSlot(pressed, index, slot));
        }
    }
    
    private void bindStepSequencer(final SeqArpHardwareElements hwElements) {
        for (int i = 0; i < 32; i++) {
            final int index = i;
            final RgbButton button = hwElements.getStepButton(i);
            button.bindLight(this.seqButtonLayer, () -> getStepColor(index));
            button.bindIsPressed(this.seqButtonLayer, pressed -> handleSeqSelection(index, pressed));
            button.bindLight(this.seqLengthLayer, () -> getStepLength(index));
            button.bindPressed(this.seqLengthLayer, () -> setStepLength(index));
        }
    }
    
    public boolean isShuffleActive() {
        return clip.getShuffle().get();
    }
    
    public void toggleShuffle() {
        clip.getShuffle().toggle();
    }
    
    private void setStepLength(final int index) {
        positionHandler.setSteps(index);
    }
    
    private YaeltexButtonLedState getStepLength(final int index) {
        if (index < positionHandler.getSteps()) {
            return YaeltexButtonLedState.BLUE;
        }
        return YaeltexButtonLedState.OFF;
    }
    
    private void bindOperators(final SeqArpHardwareElements hwElements) {
        int index = 24;
        final RingEncoder velocityEncoder = hwElements.getEncoder(index++);
        velocityEncoder.getButton().bindLight(this, () -> lightState(YaeltexButtonLedState.ORANGE));
        velocityEncoder.bindLight(this, () -> lightState(YaeltexButtonLedState.RED));
        velocityEncoder.bind(this, operatorNoteState::modifyVelocity, operatorNoteState.getVelocityValue());
        
        final RingEncoder chanceEncoder = hwElements.getEncoder(index++);
        chanceEncoder.getButton().bindLight(this, () -> lightStateChance(YaeltexButtonLedState.BLUE));
        chanceEncoder.getButton().bindIsPressed(this, this::handleRandomPressed);
        chanceEncoder.bindLight(this, () -> lightStateChance(YaeltexButtonLedState.BLUE));
        chanceEncoder.bind(this, operatorNoteState::modifyRandom, operatorNoteState.getRandomValue());
        
        final RingEncoder ratchetEncoder = hwElements.getEncoder(index++);
        final YaeltexButtonLedState ratchetColor = YaeltexButtonLedState.of(YaelTexColors.DODGER_BLUE);
        ratchetEncoder.getButton().bindLight(this, () -> lightState(ratchetColor));
        ratchetEncoder.bindLight(this, () -> lightState(ratchetColor));
        ratchetEncoder.bind(this, operatorNoteState::modifyRatchet, operatorNoteState.getRatchetValue());
        index++;
        final RingEncoder noteLengthEncoder = hwElements.getEncoder(index++);
        noteLengthEncoder.getButton().bindLight(this, () -> lightState(YaeltexButtonLedState.DEEP_GREEN));
        noteLengthEncoder.bindLight(this, () -> lightState(YaeltexButtonLedState.DEEP_GREEN));
        noteLengthEncoder.bind(this, operatorNoteState::modifyNoteLength, operatorNoteState.getNoteLength());
        
        final RingEncoder accentEncoder = hwElements.getEncoder(index++);
        accentEncoder.getButton().bindLightPressed(this, YaeltexButtonLedState.ORANGE, YaeltexButtonLedState.WHITE);
        accentEncoder.getButton().bindIsPressed(this, pressed -> handleAccentState(pressed));
        accentEncoder.bindLight(this, () -> YaeltexButtonLedState.ORANGE);
        accentEncoder.bind(this, inc -> accentValue.increment(inc), accentValue);
        
        final RingEncoder copyEncoder = hwElements.getEncoder(index++);
        copyEncoder.getButton().bindLightPressed(this, YaeltexButtonLedState.RED_DIM, YaeltexButtonLedState.RED);
        copyEncoder.getButton().bindIsPressed(this, pressed -> handleCopyState(pressed));
        
        final RingEncoder lastStepEncoder = hwElements.getEncoder(index++);
        lastStepEncoder.getButton().bindLightPressed(this, YaeltexButtonLedState.OFF, YaeltexButtonLedState.BLUE);
        lastStepEncoder.getButton().bindIsPressed(this, pressed -> handleLastStepState(pressed));
    }
    
    private void handleRandomPressed(final boolean active) {
        this.randomHeld = active;
        if (active) {
            operatorNoteState.applyRadomFixed();
        }
    }
    
    private void handleLastStepState(final boolean active) {
        this.seqLengthLayer.setIsActive(active);
        this.seqButtonLayer.setIsActive(!active);
    }
    
    private void handleAccentState(final boolean active) {
        this.accentHeld = active;
    }
    
    private void handleCopyState(final boolean active) {
        this.copyHeld = active;
    }
    
    private YaeltexButtonLedState lightState(final YaeltexButtonLedState color) {
        return operatorNoteState.editMode() ? color : YaeltexButtonLedState.OFF;
    }
    
    private YaeltexButtonLedState lightStateChance(final YaeltexButtonLedState color) {
        return operatorNoteState.editMode() || randomHeld ? color : YaeltexButtonLedState.OFF;
    }
    
    private void modifySteps(final int index, final int inc) {
        if (index != selectedPadIndex) {
            clip.scrollToKey(index + noteOffset);
            this.selectedPadIndex = index;
        } else {
            movePattern(inc);
        }
    }
    
    private void movePattern(final int dir) {
        final List<NoteStep> notes = getOnNotes();
        final int availableSteps = positionHandler.getAvailableSteps();
        clip.clearStepsAtY(0, 0);
        
        for (final NoteStep noteStep : notes) {
            int pos = noteStep.x() + dir;
            if (pos < 0) {
                pos = availableSteps - 1;
            } else if (pos >= availableSteps) {
                pos = 0;
            }
            operatorNoteState.registerChanges(pos, noteStep);
            clip.setStep(pos, 0, (int) Math.round(noteStep.velocity() * 127), noteStep.duration());
        }
    }
    
    List<NoteStep> getOnNotes() {
        return Arrays.stream(assignments).filter(ns -> ns != null && ns.state() == NoteStep.State.NoteOn)
            .collect(Collectors.toList());
    }
    
    private void handlePadSelection(final int index, final boolean selected) {
        if (selected) {
            selectedPadIndex = index;
            if (selectedPadIndex >= 0 && selectedPadIndex < 16) {
                clip.scrollToKey(selectedPadIndex + noteOffset);
            }
        }
    }
    
    private YaeltexButtonLedState getStepColor(final int index) {
        if (index >= 0 && index < 32) {
            if (index == playingStep) {
                return YaeltexButtonLedState.WHITE;
            }
            final NoteStep.State state = assignments[index] == null ? NoteStep.State.Empty : assignments[index].state();
            if (state == NoteStep.State.Empty) {
                return YaeltexButtonLedState.OFF;
            } else if (state == NoteStep.State.NoteOn) {
                if (assignments[index].chance() <= 0.73) {
                    return selectedPadIndex >= 0 ? padColorsAlt[selectedPadIndex] : YaeltexButtonLedState.GREEN_DIM;
                }
                return selectedPadIndex >= 0 ? padColors[selectedPadIndex] : YaeltexButtonLedState.GREEN;
            }
        }
        return YaeltexButtonLedState.OFF;
    }
    
    private void setupClip() {
        clip.addNoteStepObserver(this::handNoteSteps);
        clip.playingStep().addValueObserver(this::handlePlayingStep);
        clip.getLoopLength().addValueObserver(clipLength -> {
            if (markIgnoreOrigLen) {
                markIgnoreOrigLen = false;
            }
        });
        clip.isPinned().markInterested();
        clip.exists().markInterested();
        clip.clipLauncherSlot().isPlaying().markInterested();
        clip.clipLauncherSlot().isPlaybackQueued().markInterested();
        clip.getShuffle().markInterested();
        //clip.launchMode().addValueObserver(mode -> SeqArp168Extension.println("MODE = %s", mode));
    }
    
    private void handNoteSteps(final NoteStep noteStep) {
        final int newStep = noteStep.x();
        assignments[newStep] = noteStep;
        operatorNoteState.handleNewStep(noteStep);
    }
    
    private void handlePlayingStep(final int playingStep) {
        if (playingStep == -1) {
            this.playingStep = -1;
        }
        this.playingStep = playingStep - positionHandler.getStepOffset();
    }
    
    private void handleSlot(final boolean pressed, final int index, final ClipLauncherSlot slot) {
        if (pressed && slot.hasContent().get()) {
            slot.launch();
            slot.select();
        }
    }
    
    private void prepareSlot(final int index, final ClipLauncherSlot slot) {
        slot.exists().markInterested();
        slot.isPlaying().markInterested();
        slot.hasContent().markInterested();
        slot.isPlaybackQueued().markInterested();
        slot.isStopQueued().markInterested();
        slot.color().addValueObserver((r, g, b) -> slotColors[index] = YaeltexButtonLedState.of(r, g, b));
    }
    
    private InternalHardwareLightState getSlotColor(final int index, final ClipLauncherSlot slot) {
        if (slot.hasContent().get()) {
            if (slot.isPlaybackQueued().get()) {
                return midiProcessor.blinkMid(slotColors[index]);
            } else if (slot.isStopQueued().get()) {
                return midiProcessor.blinkFast(slotColors[index]);
            } else if (slot.isPlaying().get()) {
                return midiProcessor.blinkSlow(YaeltexButtonLedState.GREEN);
            }
            return slotColors[index];
        }
        return YaeltexButtonLedState.OFF;
    }
    
    private YaeltexButtonLedState getPadColor(final int index) {
        return padColors[index];
    }
    
    private YaeltexButtonLedState getPadColorState(final int index) {
        if (notesPlaying[index]) {
            return playColor;
        }
        if (index == selectedPadIndex) {
            return YaeltexButtonLedState.WHITE;
        }
        return padColors[index];
    }
    
    private void handleNotesPlaying(final PlayingNote[] notes) {
        Arrays.fill(notesPlaying, false);
        for (final PlayingNote note : notes) {
            final int index = note.pitch() - noteOffset;
            if (index >= 0 && index < 16) {
                notesPlaying[index] = true;
            }
        }
    }
    
    private void handleSeqSelection(final int index, final boolean pressed) {
        final NoteStep note = assignments[index];
        if (randomHeld) {
            if (pressed) {
                handleRandomAction(index, note);
            }
        } else if (copyHeld) {
            if (pressed) {
                handleNoteCopyAction(index, note);
            }
        } else if (accentHeld) {
            if (pressed && !isSettableSlot(note)) {
                final int current = (int) (note.velocity() * 127);
                if (current == accentValue.get()) {
                    note.setVelocity((double) nonAccentVelocity / 127.0);
                } else {
                    note.setVelocity((double) accentValue.get() / 127.0);
                }
            }
        } else {
            if (!pressed) {
                final boolean hasBeenModified = operatorNoteState.editingOccured();
                operatorNoteState.removeStep(index);
                if (!hasBeenModified) {
                    removeNote(index, note);
                }
            } else {
                operatorNoteState.addStep(index);
                if (isSettableSlot(note)) {
                    clip.setStep(index, 0, getVelocity(), positionHandler.getGridResolution() * gatePercent);
                    addedSteps.add(index);
                }
            }
        }
    }
    
    private void handleRandomAction(final int index, final NoteStep note) {
        if (note == null) {
            return;
        }
        if (note.chance() < 1.0) {
            note.setChance(1.0);
        } else {
            note.setChance(0.7);
        }
    }
    
    private void handleNoteCopyAction(final int index, final NoteStep note) {
        if (copyNote != null) {
            if (index == copyNote.x()) {
                return;
            }
            final int vel = (int) Math.round(copyNote.velocity() * 127);
            final double duration = copyNote.duration();
            operatorNoteState.registerChanges(index, copyNote);
            clip.setStep(index, 0, vel, duration);
        } else if (note != null && note.state() == NoteStep.State.NoteOn) {
            copyNote = note;
        }
    }
    
    
    private static boolean isSettableSlot(final NoteStep note) {
        return note == null || note.state() == NoteStep.State.Empty || note.state() == NoteStep.State.NoteSustain;
    }
    
    private void removeNote(final int index, final NoteStep note) {
        if (note != null && note.state() == NoteStep.State.NoteOn && !addedSteps.contains(index)) {
            if (!modifiedSteps.contains(index)) {
                this.clip.toggleStep(index, 0, getVelocity());
            } else {
                modifiedSteps.remove(index);
            }
        }
        addedSteps.remove(index);
    }
    
    private int getVelocity() {
        return accentHeld ? accentValue.get() : nonAccentVelocity;
    }
    
    @Override
    protected void onActivate() {
        this.seqButtonLayer.setIsActive(true);
        seqLengthLayer.setIsActive(false);
    }
    
    @Override
    protected void onDeactivate() {
        seqButtonLayer.setIsActive(false);
        seqLengthLayer.setIsActive(false);
    }
}
