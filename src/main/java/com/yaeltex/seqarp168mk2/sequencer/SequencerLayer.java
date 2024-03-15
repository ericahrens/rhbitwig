package com.yaeltex.seqarp168mk2.sequencer;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import com.akai.fire.sequence.IntSetValue;
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
import com.bitwig.extensions.framework.values.StepViewPosition;
import com.yaeltex.common.YaeltexButtonLedState;
import com.yaeltex.common.YaeltexMidiProcessor;
import com.yaeltex.controls.RgbButton;
import com.yaeltex.controls.RingEncoder;
import com.yaeltex.seqarp168mk2.BitwigViewControl;
import com.yaeltex.seqarp168mk2.SeqArp168Extension;
import com.yaeltex.seqarp168mk2.SeqArpHardwareElements;

@Component
public class SequencerLayer extends Layer {
    
    private final BitwigViewControl viewControl;
    private final boolean[] notesPlaying = new boolean[16];
    private final YaeltexButtonLedState[] padColors = new YaeltexButtonLedState[16];
    private final YaeltexButtonLedState[] slotColors = new YaeltexButtonLedState[8];
    private YaeltexButtonLedState trackColor;
    private int noteOffset = 0;
    private int selectedIndex = -1;
    private final YaeltexMidiProcessor midiProcessor;
    private final PinnableCursorClip clip;
    private final NoteStep[] assignments = new NoteStep[32];
    private final HashMap<Integer, NoteStep> expectedNoteChanges = new HashMap<>();
    private final Set<Integer> addedSteps = new HashSet<>();
    private final Set<Integer> modifiedSteps = new HashSet<>();
    private int playingStep;
    private boolean markIgnoreOrigLen = false;
    private final double gatePercent = 0.98;
    private final StepViewPosition positionHandler;
    private final IntSetValue heldSteps = new IntSetValue();
    private final int inputVelocity = 100;
    
    public SequencerLayer(final Layers layers, final SeqArpHardwareElements hwElements,
        final BitwigViewControl viewControl, final YaeltexMidiProcessor midiProcessor) {
        super(layers, "SEQUENCER_LAYER");
        this.viewControl = viewControl;
        this.midiProcessor = midiProcessor;
        this.clip = viewControl.getCursorClip();
        
        positionHandler = new StepViewPosition(clip, 32, "YAELTEXT");
        setupClip();
        final DrumPadBank drumPadBank = this.viewControl.getDrumPadBank();
        drumPadBank.scrollPosition().addValueObserver(position -> {
            this.noteOffset = position;
            if (selectedIndex != -1) {
                this.clip.scrollToKey(this.noteOffset + selectedIndex);
            }
        });
        final CursorTrack cursorTrack = this.viewControl.getCursorTrack();
        cursorTrack.color().addValueObserver((r, g, b) -> trackColor = YaeltexButtonLedState.of(r, g, b));
        cursorTrack.playingNotes().addValueObserver(notes -> handleNotesPlaying(notes));
        for (int i = 0; i < drumPadBank.getSizeOfBank(); i++) {
            final int index = i;
            final RingEncoder encoder = hwElements.getEncoder(i);
            final DrumPad drumPad = drumPadBank.getItemAt(i);
            drumPad.color().addValueObserver((r, g, b) -> padColors[index] = YaeltexButtonLedState.of(r, g, b));
            drumPad.addIsSelectedInEditorObserver(selected -> handlePadSelection(index, selected));
            encoder.getButton().bindLight(this, () -> getPadColorState(index));
            encoder.getButton().bindPressed(this, () -> drumPad.selectInEditor());
            encoder.bindLight(this, () -> getPadColorState(index));
            encoder.bind(this, inc -> this.modifySteps(index, inc));
        }
        final ClipLauncherSlotBank slotBank = cursorTrack.clipLauncherSlotBank();
        for (int i = 0; i < slotBank.getSizeOfBank(); i++) {
            final int index = i;
            final ClipLauncherSlot slot = slotBank.getItemAt(i);
            
            prepareSlot(index, slot);
            final RingEncoder encoder = hwElements.getEncoder(i + 16);
            encoder.getButton().bindLight(this, () -> getSlotColor(index, slot));
            encoder.getButton().bindIsPressed(this, pressed -> handleSlot(pressed, index, slot));
        }
        for (int i = 0; i < 32; i++) {
            final int index = i;
            final RgbButton button = hwElements.getStepButton(i);
            button.bindLight(this, () -> getStepColor(index));
            button.bindIsPressed(this, pressed -> handleSeqSelection(index, pressed));
        }
    }
    
    private void modifySteps(final int index, final int inc) {
        if (index != selectedIndex) {
            clip.scrollToKey(index + noteOffset);
            this.selectedIndex = index;
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
            expectedNoteChanges.put(pos, noteStep);
            clip.setStep(pos, 0, (int) Math.round(noteStep.velocity() * 127), noteStep.duration());
        }
    }
    
    List<NoteStep> getOnNotes() {
        return Arrays.stream(assignments).filter(ns -> ns != null && ns.state() == NoteStep.State.NoteOn)
            .collect(Collectors.toList());
    }
    
    private void handlePadSelection(final int index, final boolean selected) {
        if (selected) {
            selectedIndex = index;
            if (selectedIndex >= 0 && selectedIndex < 16) {
                clip.scrollToKey(selectedIndex + noteOffset);
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
                return selectedIndex >= 0 ? padColors[selectedIndex] : YaeltexButtonLedState.GREEN;
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
        clip.launchMode().addValueObserver(mode -> SeqArp168Extension.println("MODE = %s", mode));
    }
    
    private void handNoteSteps(final NoteStep noteStep) {
        final int newStep = noteStep.x();
        assignments[newStep] = noteStep;
        if (expectedNoteChanges.containsKey(newStep)) {
            final NoteStep previousStep = expectedNoteChanges.get(newStep);
            expectedNoteChanges.remove(newStep);
            //applyValues(noteStep, previousStep);
        }
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
    
    private YaeltexButtonLedState getPadColorState(final int index) {
        if (notesPlaying[index]) {
            final YaeltexButtonLedState color =
                padColors[index] == YaeltexButtonLedState.OFF ? padColors[index] : trackColor;
            
            return padColors[index];
        }
        if (index == selectedIndex) {
            return YaeltexButtonLedState.WHITE;
        }
        return notesPlaying[index] ? YaeltexButtonLedState.GREEN : YaeltexButtonLedState.OFF;
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
        if (!pressed) {
            heldSteps.remove(index);
            if (note != null && note.state() == NoteStep.State.NoteOn && !addedSteps.contains(index)) {
                if (!modifiedSteps.contains(index)) {
                    this.clip.toggleStep(index, 0, inputVelocity);
                } else {
                    modifiedSteps.remove(index);
                }
            }
            addedSteps.remove(index);
        } else {
            heldSteps.add(index);
            if (note == null || note.state() == NoteStep.State.Empty || note.state() == NoteStep.State.NoteSustain) {
                clip.setStep(index, 0, inputVelocity, positionHandler.getGridResolution() * gatePercent);
                addedSteps.add(index);
            }
        }
    }
    
}
