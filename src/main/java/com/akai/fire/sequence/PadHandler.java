package com.akai.fire.sequence;

import com.akai.fire.AkaiFireDrumSeqExtension;
import com.akai.fire.NoteAssign;
import com.akai.fire.ViewCursorControl;
import com.akai.fire.control.BiColorButton;
import com.akai.fire.control.RgbButton;
import com.akai.fire.display.DisplayInfo;
import com.akai.fire.display.DisplayTarget;
import com.akai.fire.display.OledDisplay.TextJustification;
import com.akai.fire.lights.BiColorLightState;
import com.akai.fire.lights.RgbLigthState;
import com.akai.fire.sequence.ClipboardManager.ClippedNote;
import com.akai.fire.sequence.NoteAction.Type;
import com.bitwig.extension.controller.api.*;
import com.bitwig.extensions.framework.Layer;
import com.bitwig.extensions.rh.BooleanValueObject;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class PadHandler {

    final DrumSequenceMode parent;
    private final PinnableCursorClip cursorClip;
    private final NoteInput noteInput;
    private final DrumPadBank padBank;

    private final NoteRepeatHandler noteRepeatHandler;

    private final List<PadContainer> pads = new ArrayList<>();
    private final Set<Integer> padsHeld = new HashSet<>();

    RgbLigthState currentPadColor = RgbLigthState.PURPLE;

    PadContainer selectedPad;
    int selectedPadIndex = -1;
    private int drumScrollOffset;

    private final BooleanValueObject[] playing = new BooleanValueObject[16];
    private final BooleanValueObject notePlayingActive = new BooleanValueObject();

    private final boolean[] drumTracker = new boolean[16];
    private final Integer[] notesToDrumTable = new Integer[128];
    private final int[] notesToPadsTable = new int[128];
    private final int[] padNotes = new int[16];
    private final DisplayTarget displayTarget;
    private final DisplayInfo padDisplayInfo;
    
    private final ClipboardManager clipboardManager = new ClipboardManager();
    
    private boolean copyModeActive = false;
    private List<ClippedNote> clipboard = null;
    private int sourcePadIndex = -1;

    public PadHandler(final AkaiFireDrumSeqExtension driver, final DrumSequenceMode parent, final Layer mainLayer,
                      final Layer muteLayer, final Layer soloLayer) {
        this.parent = parent;
        cursorClip = parent.getCursorClip();
        noteInput = driver.getNoteInput();
        for (int i = 0; i < padNotes.length; i++) {
            padNotes[i] = 0x36 + i;
        }
        final ViewCursorControl control = driver.getViewControl();

        padBank = control.getDrumPadBank();
        padBank.canScrollBackwards().markInterested();
        padBank.canScrollForwards().markInterested();
        padBank.scrollPosition().markInterested();

        setupPlaying(driver.getViewControl());

        displayTarget = new DisplayTarget(parent.getOled());

        final RgbButton[] rgbButtons = driver.getRgbButtons();
        for (int i = 0; i < 16; i++) {
            final RgbButton button = rgbButtons[i];
            final PadContainer pad = new PadContainer(this, i, control.getDrumPadBank().getItemAt(i), playing[i]);

            bindMain(button, mainLayer, pad);
            button.bind(muteLayer, () -> {
                pad.pad.mute().toggle();
                parent.notifyMuteAction();
            });
            button.bindLight(muteLayer, pad::mutingColors);
            button.bind(soloLayer, () -> {
                pad.pad.solo().toggle();
                parent.notifySoloAction();
            });
            button.bindLight(soloLayer, pad::soloingColors);
        }

        noteRepeatHandler = new NoteRepeatHandler(driver, parent);
        noteRepeatHandler.getNoteRepeatActive().addValueObserver(this::handleNoteRepeatChanged);

        notePlayingActive.addValueObserver(active -> {
            if (active) {
                applyScale();
            } else if (!notePlayingEnabled()) {
                disableNotePlaying();
            }
        });
        initButtons(mainLayer, driver);
        padDisplayInfo = new DisplayInfo() //
                .addLine("Selected Pad", 1, 0, TextJustification.CENTER) //
                .addLine(() -> selectedPad != null ? selectedPad.getName() : "", 2, 3, TextJustification.CENTER) //
                .create();
    }

    private void bindMain(final RgbButton button, final Layer mainLayer, final PadContainer pad) {
        pads.add(pad);
        button.bindPressed(mainLayer, p -> handlePadSelection(pad, p), pad::getColor);
    }

    private void initButtons(final Layer mainLayer, final AkaiFireDrumSeqExtension driver) {
        final BiColorButton browerNrButton = driver.getButton(NoteAssign.BROWSER);
        browerNrButton.bindPressed(mainLayer, noteRepeatHandler::handlePressed, noteRepeatHandler::getLightState);
        final BiColorButton upNavButon = driver.getButton(NoteAssign.PATTERN_UP);
        upNavButon.markPressedInteressed();
        upNavButon.bindPressed(mainLayer, this::scrollForward, () -> canScrollUp(upNavButon));

        final BiColorButton downNavButon = driver.getButton(NoteAssign.PATTERN_DOWN);
        downNavButon.markPressedInteressed();
        downNavButon.bindPressed(mainLayer, this::scrollBackward, () -> canScrollDown(downNavButon));
        
        final BiColorButton copyButton = driver.getButton(NoteAssign.MUTE_3);
        copyButton.bindPressed(mainLayer, this::handleCopyButton, this::getCopyModeState);
    }
    
    private BiColorLightState getCopyModeState() {
        return copyModeActive ? BiColorLightState.GREEN_FULL : BiColorLightState.GREEN_HALF;
    }
    
    private void handleCopyButton(final boolean pressed) {
        if (pressed) {
            copyModeActive = true;
            if (selectedPad != null) {
                sourcePadIndex = selectedPad.getIndex();
                copySourceToClipboard();
            }
        } else {
            copyModeActive = false;
            sourcePadIndex = -1;
        }
    }

    private void copySourceToClipboard() {
        if (sourcePadIndex == -1) return;
        
        cursorClip.scrollToKey(drumScrollOffset + sourcePadIndex);
        List<NoteStep> notes = parent.getOnNotes();
        if (notes.isEmpty()) {
            clipboard = null;
            return;
        }
        clipboardManager.copyNotes(notes);
        clipboard = clipboardManager.getClipboard();
        
        if (selectedPad != null) {
            cursorClip.scrollToKey(drumScrollOffset + selectedPad.getIndex());
        }
    }

    private void selectSourcePad() {
        if (sourcePadIndex == -1) return;
        if (selectedPad != null && selectedPad.getIndex() == sourcePadIndex) return;
        
        for (PadContainer pad : pads) {
            if (pad.getIndex() == sourcePadIndex) {
                pad.pad.selectInEditor();
                selectedPad = pad;
                selectedPadIndex = pad.getIndex();
                break;
            }
        }
    }

    private void handlePadSelection(final PadContainer pad, final boolean pressed) {
        if (!pressed) {
            padsHeld.remove(pad.index);
            // КОГАТО ПУСНЕШ PAD-А - МАРКИРАЙ ИЗТОЧНИКА
            if (copyModeActive) {
                selectSourcePad();
            }
        } else {
            if (parent.isDeleteHeld()) {
                if (pad.index == selectedPadIndex) {
                    cursorClip.clearStepsAtY(0, 0);
                } else {
                    parent.registerPendingAction(new NoteAction(selectedPadIndex, pad.index, Type.CLEAR));
                    pad.pad.selectInEditor();
                }
                return;
            }
            
            pad.pad.selectInEditor();
            padsHeld.add(pad.index);
            
            if (copyModeActive && clipboard != null && !clipboard.isEmpty()) {
                if (pad.getIndex() != sourcePadIndex) {
                    // САМО ЕДНО ПЕЙСТВАНЕ - ВИНАГИ!
                    executePaste(clipboard, pad);
                    parent.getOled().paramInfo("Pasted!", clipboard.size() + " notes");
                    parent.getOled().clearScreenDelayed();
                    
                    // МАРКИРАЙ ИЗТОЧНИКА
                    selectSourcePad();
                }
            }
        }
    }

    private void executePaste(List<ClippedNote> clippedNotes, PadContainer destinationPad) {
        if (clippedNotes == null || clippedNotes.isEmpty()) return;
        
        cursorClip.scrollToKey(drumScrollOffset + destinationPad.getIndex());
        
        for (ClippedNote clipped : clippedNotes) {
            int pos = clipped.x;
            cursorClip.setStep(pos, 0, (int)(clipped.velocity * 127), clipped.duration);
            
            DrumSequenceMode.NoteData data = new DrumSequenceMode.NoteData();
            data.velocity = (int)(clipped.velocity * 127);
            data.transpose = clipped.transpose;
            data.duration = clipped.duration;
            data.chance = clipped.chance;
            data.timbre = clipped.timbre;
            data.pressure = clipped.pressure;
            data.velocitySpread = clipped.velocitySpread;
            data.repeatCount = clipped.repeatCount;
            data.repeatCurve = clipped.repeatCurve;
            data.repeatVelocityCurve = clipped.repeatVelocityCurve;
            data.repeatVelocityEnd = clipped.repeatVelocityEnd;
            data.recurrenceLength = clipped.recurrenceLength;
            data.recurrenceMask = clipped.recurrenceMask;
            data.occurrence = clipped.occurrence;
            data.pan = clipped.pan;
            
            parent.registerExpectedNoteData(pos, data);
        }
    }

    void executeCopy(final List<NoteStep> notes, final boolean copyParams) {
        cursorClip.clearStepsAtY(0, 0);
        for (final NoteStep noteStep : notes) {
            final double duration = Math.max(noteStep.duration(), 0.25);
            cursorClip.setStep(noteStep.x(), 0, (int) (noteStep.velocity() * 127), duration);
            if (copyParams) {
                parent.registerExpectedNoteChange(noteStep.x(), noteStep);
            }
        }
    }

    void executeClear(final int origIndex) {
        cursorClip.clearStepsAtY(0, 0);
        if (origIndex != -1) {
            pads.get(origIndex).pad.selectInEditor();
        }
    }

    public void executePadSelection(final PadContainer pad) {
        currentPadColor = pad.getBitwigPadColor();
        selectedPad = pad;
        focusOnSelectedPad();
        selectedPadIndex = pad.getIndex();

        displayTarget.setFocusIndex(selectedPadIndex);
        displayTarget.setName(selectedPad.getName());

        parent.getOled().showInfo(padDisplayInfo);

        selectedPad.updateDisplay(displayTarget.getTypeIndex());
        final NoteAction pendingAction = parent.getPendingAction();
        if (pendingAction != null) {
            if (pendingAction.getType() == Type.CLEAR && pendingAction.getDestPadIndex() == selectedPadIndex) {
                executeClear(pendingAction.getSrcPadIndex());
                parent.clearPendingAction();
            } else if (pendingAction.getType() == Type.COPY_PAD && pendingAction.getDestPadIndex() == selectedPadIndex) {
                executeCopy(pendingAction.getCopyNotes(), !parent.isShiftHeld());
                parent.clearPendingAction();
            }
        }
    }

    public void focusOnSelectedPad() {
        final int padIndex = selectedPad != null ? selectedPad.index : 0;
        cursorClip.scrollToKey(drumScrollOffset + padIndex);
    }

    public String getPadInfo() {
        if (selectedPad != null) {
            return selectedPad.getName();
        }
        return "";
    }

    public boolean isPadBeingHeld() {
        return !padsHeld.isEmpty();
    }

    public RgbLigthState getCurrentPadColor() {
        return currentPadColor;
    }

    private void setupPlaying(final ViewCursorControl control) {
        final DrumPadBank drumPadBank = control.getDrumPadBank();
        final CursorTrack cursorTrack = control.getCursorTrack();
        for (int i = 0; i < notesToDrumTable.length; i++) {
            notesToDrumTable[i] = -1;
            notesToPadsTable[i] = -1;
        }
        noteInput.setKeyTranslationTable(notesToDrumTable);
        drumPadBank.scrollPosition().addValueObserver(offset -> {
            drumScrollOffset = offset;
            focusOnSelectedPad();
            applyScale();
        });
        for (int i = 0; i < 16; i++) {
            playing[i] = new BooleanValueObject();
            playing[i].set(false);
        }
        cursorTrack.playingNotes().addValueObserver(this::handleNotes);
    }

    private void handleNotes(final PlayingNote[] notes) {
        if (!parent.isActive()) {
            return;
        }
        for (int i = 0; i < 16; i++) {
            drumTracker[i] = false;
        }
        for (final PlayingNote playingNote : notes) {
            final int padIndex = notesToPadsTable[playingNote.pitch()];
            if (padIndex != -1) {
                playing[padIndex].set(true);
                drumTracker[padIndex] = true;
            }
        }
        for (int i = 0; i < 16; i++) {
            if (!drumTracker[i]) {
                playing[i].set(false);
            }
        }
    }

    boolean notePlayingEnabled() {
        return notePlayingActive.get() || noteRepeatHandler.getNoteRepeatActive().get();
    }

    private void handleNoteRepeatChanged(final boolean nrActive) {
        if (nrActive) {
            noteRepeatHandler.activate();
            applyScale();
        } else {
            noteRepeatHandler.deactivate();
            if (!notePlayingEnabled()) {
                disableNotePlaying();
            }
        }
    }

    void disableNotePlaying() {
        if (!parent.isActive()) {
            return;
        }
        padsHeld.clear();
        for (int i = 0; i < 128; i++) {
            notesToDrumTable[i] = -1;
        }
        noteInput.setKeyTranslationTable(notesToDrumTable);
    }

    void applyScale() {
        if (!parent.isActive()) {
            return;
        }
        for (int i = 0; i < 128; i++) {
            notesToPadsTable[i] = -1;
        }
        for (int i = 0; i < 16; i++) {
            final int padnote = padNotes[i];
            final int noteToPadIndex = drumScrollOffset + i;
            if (noteToPadIndex < 128) {
                notesToDrumTable[padnote] = noteToPadIndex;
                notesToPadsTable[noteToPadIndex] = i;
            }
        }
        if (notePlayingEnabled()) {
            noteInput.setKeyTranslationTable(notesToDrumTable);
        }
    }

    public void handleMainEncoder(final int inc) {
        noteRepeatHandler.handleMainEncoder(inc);
    }

    private BiColorLightState canScrollUp(final BiColorButton button) {
        if (padBank.scrollPosition().get() + (parent.isShiftHeld() ? 16 : 4) < 128) {
            if (button.isPressed()) {
                return BiColorLightState.FULL;
            }
            return BiColorLightState.HALF;
        }
        return BiColorLightState.OFF;
    }

    private BiColorLightState canScrollDown(final BiColorButton button) {
        if (padBank.scrollPosition().get() - (parent.isShiftHeld() ? 16 : 4) >= 0) {
            if (button.isPressed()) {
                return BiColorLightState.FULL;
            }
            return BiColorLightState.HALF;
        }
        return BiColorLightState.OFF;
    }

    private void scrollForward(final boolean pressed) {
        if (!pressed) {
            return;
        }
        if (parent.isShiftHeld()) {
            padBank.scrollBy(4);
        } else {
            padBank.scrollBy(16);
        }
    }

    private void scrollBackward(final boolean pressed) {
        if (!pressed) {
            return;
        }
        if (parent.isShiftHeld()) {
            padBank.scrollBy(-4);
        } else {
            padBank.scrollBy(-16);
        }
    }

    public DisplayTarget getDiplayTarget() {
        return displayTarget;
    }

    public void activateView(final int typeIndex, final String paramName) {
        displayTarget.setTypeIndex(typeIndex, paramName);
        displayTarget.activate();
    }

    public void deactivateView() {
        displayTarget.deactivate();
    }

    public void modifyValue(final int typeIndex, final int inc) {
        if (selectedPad == null) {
            return;
        }
        selectedPad.modifyValue(typeIndex, inc, parent.isShiftHeld());
    }

    public void bindPadParameters(final Layer layer) {
        for (final PadContainer pad : pads) {
            pad.bindParameters(layer);
        }
    }

    public void updateDisplay(final int index) {
        if (selectedPad != null) {
            selectedPad.updateDisplay(index);
        }
    }
    
    public ClipboardManager getClipboardManager() {
        return clipboardManager;
    }

}