package com.yaeltex.djcontroller;

import java.util.Arrays;

import com.bitwig.extension.controller.api.Arpeggiator;
import com.bitwig.extension.controller.api.ClipLauncherSlot;
import com.bitwig.extension.controller.api.ClipLauncherSlotBank;
import com.bitwig.extension.controller.api.CursorRemoteControlsPage;
import com.bitwig.extension.controller.api.CursorTrack;
import com.bitwig.extension.controller.api.DrumPad;
import com.bitwig.extension.controller.api.DrumPadBank;
import com.bitwig.extension.controller.api.InternalHardwareLightState;
import com.bitwig.extension.controller.api.NoteInput;
import com.bitwig.extension.controller.api.NoteStep;
import com.bitwig.extension.controller.api.PlayingNote;
import com.bitwig.extensions.framework.Layer;
import com.bitwig.extensions.framework.Layers;
import com.bitwig.extensions.framework.di.Component;
import com.rhcommons.NoteStepStore;
import com.yaeltex.common.IntValueObject;
import com.yaeltex.common.YaelTexColors;
import com.yaeltex.common.YaeltexButtonLedState;
import com.yaeltex.common.YaeltexMidiProcessor;
import com.yaeltex.common.bindings.EncoderIntShortRangeBinding;
import com.yaeltex.common.controls.RgbButton;
import com.yaeltex.common.controls.RingEncoder;
import com.yaeltex.common.sequencing.AbstractSequencerLayer;

@Component
public class SequencerLayer extends AbstractSequencerLayer {
    private static final YaeltexButtonLedState playColor = YaeltexButtonLedState.of(YaelTexColors.VIOLET_RED, 2);
    private static final YaeltexButtonLedState muteColor = YaeltexButtonLedState.of(120);
    private static final double STD_CHANCE = 0.5;
    private final RhdjViewControl viewControl;
    
    private final boolean[] notesPlaying = new boolean[16];
    private final Integer[] notesToDrumTable = new Integer[128];
    private static final int[] PAD_NOTES = new int[] {24, 25, 26, 27, 28, 29, 30, 31};
    
    private static final int[] ARP_INDICATOR_MAPPINGS = new int[] {10, 64, 127};
    private static final double[] ARP_RATES = new double[] {0.5, 0.25, 0.125};
    
    private int noteOffset = 0;
    private int selectedPadIndex = -1;
    private int playingStep;
    private boolean markIgnoreOrigLen = false;
    private final NoteStep[] assignments = new NoteStep[16];
    private final NoteStep[] copyNotes = new NoteStep[16];
    private int copyBufferIndex = -1;
    private final CursorTrack cursorTrack;
    private final Layer seqButtonLayer;
    private final Layer seqLengthLayer;
    private final Layer muteButtonLayer;
    private final Layer selectButtonLayer;
    private final boolean muteState = false;
    private boolean copyHeld;
    private boolean randomHeld;
    private ClipLauncherSlot copySlot;
    private boolean noteRepeatEnabled = false;
    private final Arpeggiator arp;
    private final NoteInput noteInput;
    private final IntValueObject selectedArpIndex = new IntValueObject(1, 0, 2);
    private SelectMode selectMode = SelectMode.MUTE;
    private final CursorRemoteControlsPage trackRemotes;
    
    
    private enum SelectMode {
        MUTE,
        TYPE_SELECT
    }
    
    public SequencerLayer(final Layers layers, final HardwareElements hwElements, final RhdjViewControl viewControl,
        final YaeltexMidiProcessor midiProcessor) {
        super(layers, midiProcessor, viewControl.getCursorClip(), 8, 16, 4);
        this.viewControl = viewControl;
        cursorTrack = viewControl.getDrumCursorTrack();
        trackRemotes = cursorTrack.createCursorRemoteControlsPage(8);
        noteInput = midiProcessor.getMidiIn(0).createNoteInput("REPEAT", "80????", "90????");
        arp = noteInput.arpeggiator();
        initArp();
        Arrays.fill(notesToDrumTable, -1);
        
        this.seqButtonLayer = new Layer(layers, "SEQ_BUTTON_LAYER");
        this.seqLengthLayer = new Layer(layers, "SEQ_LENGTH_LAYER");
        this.muteButtonLayer = new Layer(layers, "MUTE_LAYER");
        this.selectButtonLayer = new Layer(layers, "SELECT_BUTTON_LAYER");
        setupClip();
        final DrumPadBank drumPadBank = this.viewControl.getDrumPadBank();
        drumPadBank.scrollPosition().addValueObserver(position -> {
            this.noteOffset = position;
            if (selectedPadIndex != -1) {
                this.clip.scrollToKey(this.noteOffset + selectedPadIndex);
            }
        });
        final CursorTrack cursorTrack = this.viewControl.getDrumCursorTrack();
        cursorTrack.playingNotes().addValueObserver(notes -> handleNotesPlaying(notes));
        bindPadSelection(hwElements, drumPadBank);
        bindClipLaunching(hwElements, cursorTrack);
        bindStepSequencer(hwElements);
        bindModes(hwElements);
        bindNoteRepeat(hwElements);
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
    
    private void handleNotesPlaying(final PlayingNote[] notes) {
        Arrays.fill(notesPlaying, false);
        for (final PlayingNote note : notes) {
            final int index = note.pitch() - noteOffset;
            if (index >= 0 && index < 16) {
                notesPlaying[index] = true;
            }
        }
    }
    
    private void bindPadSelection(final HardwareElements hwElements, final DrumPadBank drumPadBank) {
        final RgbButton[] selectButtons = hwElements.getSelectButtons1();
        for (int i = 0; i < drumPadBank.getSizeOfBank(); i++) {
            final int index = i;
            final RgbButton muteButton = selectButtons[8 + i];
            final RgbButton selectButton = selectButtons[i];
            final DrumPad drumPad = drumPadBank.getItemAt(i);
            drumPad.color().addValueObserver((r, g, b) -> {
                padColors[index] = YaeltexButtonLedState.of(r, g, b);
                padColorsAlt[index] = padColors[index].offset(1).intensity(10);
            });
            drumPad.mute().markInterested();
            drumPad.addIsSelectedInEditorObserver(selected -> handlePadSelection(index, selected));
            selectButton.bindLight(this, () -> getPadColorState(index));
            selectButton.bindPressed(this, () -> handlePadSelection(index, drumPad));
            muteButton.bindPressed(muteButtonLayer, () -> drumPad.mute().toggle());
            muteButton.bindLight(
                muteButtonLayer, () -> drumPad.mute().get() ? YaeltexButtonLedState.ORANGE : YaeltexButtonLedState.OFF);
            muteButton.bindToggleValue(
                selectButtonLayer, trackRemotes.getParameter(i), YaeltexButtonLedState.RED, YaeltexButtonLedState.BLUE);
        }
    }
    
    private void bindClipLaunching(final HardwareElements hwElements, final CursorTrack cursorTrack) {
        final ClipLauncherSlotBank slotBank = cursorTrack.clipLauncherSlotBank();
        for (int i = 0; i < slotBank.getSizeOfBank(); i++) {
            final int index = i;
            final ClipLauncherSlot slot = slotBank.getItemAt(i);
            prepareSlot(index, slot);
            final RgbButton button = hwElements.getMainButtons1()[i + 4];
            button.bindLight(this, () -> getSlotColor(index, slot));
            button.bindIsPressed(this, pressed -> handleSlot(pressed, index, slot));
        }
    }
    
    private void bindStepSequencer(final HardwareElements hwElements) {
        final RgbButton[] stepButtons = hwElements.getStepButtons1();
        for (int i = 0; i < stepButtons.length; i++) {
            final int index = i;
            final RgbButton button = stepButtons[i];
            button.bindLight(this.seqButtonLayer, () -> getStepColor(index));
            button.bindIsPressed(this.seqButtonLayer, pressed -> handleSeqSelection(index, pressed));
            button.bindLight(this.seqLengthLayer, () -> getStepLength(index));
            button.bindPressed(this.seqLengthLayer, () -> setStepLength(index));
        }
    }
    
    private void bindModes(final HardwareElements hwElements) {
        final RgbButton[] modeButtons = hwElements.getMainButtons1();
        final RgbButton copyButton = modeButtons[0];
        //copyButton.bindLightPressed(this, pressed -> pressed ? YaeltexButtonLedState.WHITE : YaeltexButtonLedState
        // .OFF);
        copyButton.bindLight(this, () -> this.copyHeld ? YaeltexButtonLedState.WHITE : YaeltexButtonLedState.OFF);
        copyButton.bindIsPressed(this, this::handleCopyPressed);
        final RgbButton muteSelectButton = modeButtons[1];
        muteSelectButton.bindLight(
            this, () -> selectMode == SelectMode.MUTE ? YaeltexButtonLedState.ORANGE : YaeltexButtonLedState.BLUE);
        muteSelectButton.bindPressed(this, this::toggleSelectMode);
        
        final RgbButton randomButton = modeButtons[2];
        randomButton.bindLightPressed(
            this, pressed -> pressed ? YaeltexButtonLedState.BLUE : YaeltexButtonLedState.OFF);
        randomButton.bindIsPressed(this, pressed -> randomHeld = pressed);
        
        final RgbButton lastStepButton = modeButtons[3];
        lastStepButton.bindIsPressed(this, pressed -> this.seqLengthLayer.setIsActive(pressed));
        lastStepButton.bindLightPressed(
            this, pressed -> pressed ? YaeltexButtonLedState.WHITE : YaeltexButtonLedState.OFF);
        
    }
    
    private void toggleSelectMode() {
        if (selectMode == SelectMode.MUTE) {
            this.selectMode = SelectMode.TYPE_SELECT;
        } else {
            this.selectMode = SelectMode.MUTE;
        }
        updateMuteButtonRow();
    }
    
    private void bindNoteRepeat(final HardwareElements hwElements) {
        final RingEncoder noteRepeatEncoder = hwElements.getBottomEncoders2()[3];
        final RgbButton button = noteRepeatEncoder.getButton();
        button.bindPressed(this, this::toggleNoteRepeat);
        button.bindLight(this, () -> noteRepeatEnabled ? YaeltexButtonLedState.BLUE : YaeltexButtonLedState.OFF);
        this.addBinding(
            new EncoderIntShortRangeBinding(noteRepeatEncoder, selectedArpIndex, 0, ARP_INDICATOR_MAPPINGS));
        selectedArpIndex.addValueObserver(arpIndex -> arp.rate().set(ARP_RATES[arpIndex]));
        noteRepeatEncoder.bindLight(this, () -> YaeltexButtonLedState.BLUE);
    }
    
    private void initArp() {
        arp.isEnabled().markInterested();
        arp.usePressureToVelocity().markInterested();
        arp.octaves().markInterested();
        arp.rate().markInterested();
        arp.usePressureToVelocity().set(false);
        // arp.shuffle().set(true);
        arp.mode().set("all"); // that's the note repeat way
        arp.octaves().set(0);
        arp.humanize().set(0);
        arp.isFreeRunning().set(false);
    }
    
    private void toggleNoteRepeat() {
        this.noteRepeatEnabled = !this.noteRepeatEnabled;
        if (this.noteRepeatEnabled) {
            enableArp();
            enableNotePlay();
        } else {
            arp.isEnabled().set(false);
            disableNotePlay();
        }
    }
    
    private void enableArp() {
        arp.isEnabled().set(true);
        arp.mode().set("all"); // that's the note repeat way
        arp.octaves().set(0);
        arp.humanize().set(0);
        arp.isFreeRunning().set(false);
        arp.rate().set(ARP_RATES[selectedArpIndex.getValue()]);
        arp.usePressureToVelocity().set(false);
    }
    
    private void disableNotePlay() {
        Arrays.fill(notesToDrumTable, -1);
        noteInput.setKeyTranslationTable(notesToDrumTable);
    }
    
    private void enableNotePlay() {
        for (int i = 0; i < 8; i++) {
            final int noteToPadIndex = noteOffset + i;
            if (noteToPadIndex < 128) {
                notesToDrumTable[PAD_NOTES[i]] = noteToPadIndex;
            }
        }
        noteInput.setKeyTranslationTable(notesToDrumTable);
    }
    
    private void handleCopyPressed(final Boolean pressed) {
        if (!pressed) {
            return;
        }
        this.copyHeld = !this.copyHeld;
        if (!this.copyHeld) {
            copyBufferIndex = -1;
            clearCopyNote();
        }
    }
    
    private void handlePadSelection(final int index, final DrumPad drumPad) {
        if (muteState) {
            final int previous = selectedPadIndex + noteOffset;
            clip.scrollToKey(noteOffset + index);
            midiProcessor.delayAction(
                () -> {
                    operatorNoteState.applyMute();
                    if (previous != noteOffset + index) {
                        clip.scrollToKey(previous);
                    }
                }, 20);
            
        } else {
            drumPad.selectInEditor();
        }
    }
    
    private YaeltexButtonLedState getPadColorState(final int index) {
        final YaeltexButtonLedState color = getPadColor(index);
        if (index == copyBufferIndex) {
            return midiProcessor.blinkFast(color);
        }
        return color;
    }
    
    
    private YaeltexButtonLedState getPadColor(final int index) {
        if (notesPlaying[index]) {
            return playColor;
        }
        if (index == selectedPadIndex) {
            return YaeltexButtonLedState.WHITE;
        }
        return padColors[index];
    }
    
    private void handlePadSelection(final int index, final boolean selected) {
        if (selected) {
            selectedPadIndex = index;
            if (selectedPadIndex >= 0 && selectedPadIndex < 8) {
                clip.scrollToKey(selectedPadIndex + noteOffset);
            }
        }
    }
    
    private InternalHardwareLightState getSlotColor(final int index, final ClipLauncherSlot slot) {
        if (slot.hasContent().get()) {
            if (slot.isPlaybackQueued().get()) {
                return midiProcessor.blinkMid(slotColors[index], slotColors[index].intensity(50));
            } else if (slot.isStopQueued().get()) {
                return midiProcessor.blinkFast(slotColors[index]);
            } else if (slot.isPlaying().get()) {
                return midiProcessor.blinkSlow(YaeltexButtonLedState.GREEN, YaeltexButtonLedState.GREEN_DIM);
            }
            return slotColors[index];
        }
        return YaeltexButtonLedState.OFF;
    }
    
    private void handleSlot(final boolean pressed, final int index, final ClipLauncherSlot slot) {
        if (copyHeld) {
            if (copySlot != null) {
                slot.replaceInsertionPoint().copySlotsOrScenes(copySlot);
            } else if (slot.hasContent().get()) {
                copySlot = slot;
            }
        } else {
            if (pressed && slot.hasContent().get()) {
                slot.select();
            }
            if (pressed) {
                slot.launch();
            } else {
                slot.launchRelease();
            }
        }
    }
    
    private YaeltexButtonLedState getStepColor(final int index) {
        if (index >= 0 && index < 16) {
            if (index == playingStep) {
                return YaeltexButtonLedState.WHITE;
            }
            final NoteStep.State state = assignments[index] == null ? NoteStep.State.Empty : assignments[index].state();
            if (state == NoteStep.State.Empty) {
                return YaeltexButtonLedState.OFF;
            } else if (state == NoteStep.State.NoteOn) {
                if (assignments[index].isMuted()) {
                    return muteColor;
                }
                if (assignments[index].chance() <= STD_CHANCE) {
                    return selectedPadIndex >= 0 ? padColorsAlt[selectedPadIndex] : YaeltexButtonLedState.GREEN_DIM;
                }
                return selectedPadIndex >= 0 ? padColors[selectedPadIndex] : YaeltexButtonLedState.GREEN;
            }
        }
        return YaeltexButtonLedState.OFF;
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
    
    protected void handleNoteCopyAction(final int index, final NoteStep note) {
        if (copyBufferIndex != -1) {
            for (int i = 0; i < this.copyNotes.length; i++) {
                final NoteStep copyNote = copyNotes[i];
                if (copyNote == null || copyNote.state() == NoteStep.State.Empty) {
                    clip.clearStep(i, 0);
                } else {
                    final int vel = (int) Math.round(copyNote.velocity() * 127);
                    final double duration = copyNote.duration();
                    operatorNoteState.registerChanges(i, copyNote);
                    clip.setStep(i, 0, vel, duration);
                }
            }
        } else {
            copyBufferIndex = selectedPadIndex;
            
            for (int i = 0; i < this.copyNotes.length; i++) {
                final NoteStep orig = this.assignments[i];
                this.copyNotes[i] = orig != null ? new NoteStepStore(orig) : null;
            }
        }
    }
    
    
    @Override
    protected int getVelocity() {
        return 100;
    }
    
    @Override
    protected void onActivate() {
        this.seqButtonLayer.setIsActive(true);
        seqLengthLayer.setIsActive(false);
        updateMuteButtonRow();
        disableNotePlay();
    }
    
    private void updateMuteButtonRow() {
        muteButtonLayer.setIsActive(selectMode == SelectMode.MUTE);
        selectButtonLayer.setIsActive(selectMode == SelectMode.TYPE_SELECT);
    }
    
    @Override
    protected void onDeactivate() {
        seqButtonLayer.setIsActive(false);
        seqLengthLayer.setIsActive(false);
        disableNotePlay();
    }
    
}