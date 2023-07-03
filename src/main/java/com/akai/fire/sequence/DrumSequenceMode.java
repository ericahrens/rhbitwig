package com.akai.fire.sequence;

import com.akai.fire.AkaiFireDrumSeqExtension;
import com.akai.fire.NoteAssign;
import com.akai.fire.control.BiColorButton;
import com.akai.fire.control.RgbButton;
import com.akai.fire.control.TouchEncoder;
import com.akai.fire.display.DisplayInfo;
import com.akai.fire.display.OledDisplay;
import com.akai.fire.display.OledDisplay.TextJustification;
import com.akai.fire.lights.BiColorLightState;
import com.akai.fire.lights.RgbLigthState;
import com.bitwig.extension.controller.api.CursorTrack;
import com.bitwig.extension.controller.api.MultiStateHardwareLight;
import com.bitwig.extension.controller.api.NoteStep;
import com.bitwig.extension.controller.api.NoteStep.State;
import com.bitwig.extension.controller.api.PinnableCursorClip;
import com.bitwig.extensions.framework.Layer;
import com.bitwig.extensions.rh.BooleanValueObject;
import com.bitwig.extensions.rh.StepViewPosition;

import java.util.*;
import java.util.stream.Collectors;

public class DrumSequenceMode extends Layer {

    private final IntSetValue heldSteps = new IntSetValue();
    private final Set<Integer> addedSteps = new HashSet<>();
    private final Set<Integer> modifiedSteps = new HashSet<>();
    private final HashMap<Integer, NoteStep> expectedNoteChanges = new HashMap<>();

    private final NoteStep[] assignments = new NoteStep[32];

    private final OledDisplay oled;

    private final Layer mainLayer;
    private final Layer shiftLayer;
    private Layer currentLayer;
    private final Layer muteLayer;
    private final Layer soloLayer;
    private final SequencEncoderHandler encoderLayer;

    private final CursorTrack cursorTrack;
    private final PinnableCursorClip cursorClip;

    private final StepViewPosition positionHandler;
    private final ResolutionHander resolutionHandler;
    private final SeqClipHandler clipHandler;
    private final RecurrenceEditor recurrenceEditor;
    private final PadHandler padHandler;

    private final BooleanValueObject muteMode = new BooleanValueObject();
    private final BooleanValueObject soloMode = new BooleanValueObject();
    private final BooleanValueObject selectHeld = new BooleanValueObject();
    private final BooleanValueObject copyHeld = new BooleanValueObject();
    private final BooleanValueObject deleteHeld = new BooleanValueObject();
    private final BooleanValueObject fixedLengthHeld = new BooleanValueObject();
    private final BooleanValueObject shiftActive = new BooleanValueObject();
    private final BooleanValueObject clipLaunchModeQuant = new BooleanValueObject();
    private final BooleanValueObject lengthDisplay = new BooleanValueObject();

    private final BooleanValueObject muteActionsTaken = new BooleanValueObject();
    private final BooleanValueObject soloActionsTaken = new BooleanValueObject();

    private int playingStep;
    private final double gatePercent = 0.98;
    private boolean markIgnoreOrigLen = false;
    private final AccentHandler accentHandler;
    private NoteAction pendingAction;
    private NoteStep copyNote = null;
    private int blinkState;

    public DrumSequenceMode(final AkaiFireDrumSeqExtension driver) {
        super(driver.getLayers(), "DRUM_SEQUENCE_LAYER");
        oled = driver.getOled();
        mainLayer = new Layer(getLayers(), getName() + "_MAIN");
        shiftLayer = new Layer(getLayers(), getName() + "_SHIFT");
        muteLayer = new Layer(getLayers(), getName() + "_MUTE");
        soloLayer = new Layer(getLayers(), getName() + "_SOLO");

        currentLayer = mainLayer;
        accentHandler = new AccentHandler(this);
        resolutionHandler = new ResolutionHander(this);

        cursorTrack = driver.getViewControl().getCursorTrack();
        cursorTrack.name().markInterested();
        cursorClip = cursorTrack.createLauncherCursorClip("SQClip", "SQClip", 32, 1);

        cursorClip.addNoteStepObserver(this::handleNoteStep);
        cursorClip.playingStep().addValueObserver(this::handlePlayingStep);
        cursorClip.getLoopLength().addValueObserver(clipLength -> {
            if (markIgnoreOrigLen) {
                markIgnoreOrigLen = false;
            }
        });
        cursorClip.isPinned().markInterested();
        positionHandler = new StepViewPosition(cursorClip, 32, "AKAI");

        padHandler = new PadHandler(driver, this, mainLayer, muteLayer, soloLayer);
        clipHandler = new SeqClipHandler(driver, this, mainLayer);
        recurrenceEditor = new RecurrenceEditor(driver, this);

        initSequenceSection(driver);
        initModeButtons(driver);
        initButtonBehaviour(driver);
        encoderLayer = new SequencEncoderHandler(this, driver, padHandler);

        muteMode.addValueObserver(active -> {
            if (active) {
                muteLayer.activate();
            } else {
                muteLayer.deactivate();
            }
        });

        soloMode.addValueObserver(active -> {
            if (active) {
                soloLayer.activate();
            } else {
                soloLayer.deactivate();
            }
        });
        copyHeld.addValueObserver(held -> {
            if (!held && copyNote != null) {
                copyNote = null;
            }
        });

        final TouchEncoder mainEncoder = driver.getMainEncoder();
        mainEncoder.setStepSize(0.4);
        mainEncoder.bindEncoder(mainLayer, this::handleMainEncoder);
        mainEncoder.bindTouched(mainLayer, this::handeMainEncoderPress);
    }

    private void initModeButtons(final AkaiFireDrumSeqExtension driver) {
        final MultiStateHardwareLight[] stateLights = driver.getStateLights();
        bindEditButton(driver.getButton(NoteAssign.MUTE_1), "Select", selectHeld, stateLights[0], muteMode,
                muteActionsTaken);
        bindEditButton(driver.getButton(NoteAssign.MUTE_2), "Last Step", fixedLengthHeld, stateLights[1], soloMode,
                soloActionsTaken);
        bindEditButton(driver.getButton(NoteAssign.MUTE_3), "Copy", copyHeld, stateLights[2], null, null);
        bindEditButton(driver.getButton(NoteAssign.MUTE_4), "Delete/Reset", deleteHeld, stateLights[3], null, null);
        final BiColorButton deleteButton = driver.getButton(NoteAssign.MUTE_4);
        deleteButton.bind(mainLayer, deleteHeld, BiColorLightState.GREEN_FULL, BiColorLightState.OFF);
    }

    private void initButtonBehaviour(final AkaiFireDrumSeqExtension driver) {

        final BiColorButton accentButton = driver.getButton(NoteAssign.STEP_SEQ); // TODO combine with encoder
        accentButton.bindPressed(mainLayer, accentHandler::handlePressed, accentHandler::getLightState);

        final BiColorButton shiftButton = driver.getButton(NoteAssign.SHIFT);
        shiftButton.bind(mainLayer, shiftActive, BiColorLightState.GREEN_HALF, BiColorLightState.OFF);

        final BiColorButton clipLaunchModeButton = driver.getButton(NoteAssign.NOTE);
        clipLaunchModeButton.bindToggle(mainLayer, clipLaunchModeQuant, BiColorLightState.AMBER_FULL,
                BiColorLightState.AMBER_HALF, oled,
                new DisplayInfo().addLine("Clip Legato", 2, 0, TextJustification.CENTER)//
                        .addLine(() -> clipLaunchModeQuant.get() ? "with quant" : "immediate", 2, 3,
                                TextJustification.CENTER)//
                        .create());

        final BiColorButton retrigButton = driver.getButton(NoteAssign.DRUM);
        retrigButton.bind(mainLayer, this::retrigger, BiColorLightState.AMBER_FULL, BiColorLightState.AMBER_HALF);

        final BiColorButton pinButton = driver.getButton(NoteAssign.ALT);
        pinButton.bindPressed(mainLayer, this::handleClipPinning, this::getPinnedState);

        final BiColorButton resolutionButton = driver.getButton(NoteAssign.PERFORM);
        resolutionButton.bindPressed(mainLayer, resolutionHandler::handlePressed, resolutionHandler::getLightState);

        final BiColorButton shiftLeftButton = driver.getButton(NoteAssign.BANK_L);
        shiftLeftButton.bindPressed(mainLayer, p -> movePattern(p, -1), BiColorLightState.HALF, BiColorLightState.OFF);
        final BiColorButton shiftRightButton = driver.getButton(NoteAssign.BANK_R);
        shiftRightButton.bindPressed(mainLayer, p -> movePattern(p, 1), BiColorLightState.HALF, BiColorLightState.OFF);
    }

    private void initSequenceSection(final AkaiFireDrumSeqExtension driver) {
        final RgbButton[] rgbButtons = driver.getRgbButtons();
        for (int i = 0; i < 32; i++) {
            final RgbButton button = rgbButtons[i + 32];
            final int index = i;
            button.bindPressed(mainLayer, p -> handleSeqSelection(index, p), () -> stepState(index));
        }
    }

    private void handleSeqSelection(final int index, final boolean pressed) {
        final NoteStep note = assignments[index];
        if (!pressed) {
            heldSteps.remove(index);
            if (copyHeld.get() || fixedLengthHeld.get()) {
                // do nothing
            } else if (note != null && note.state() == State.NoteOn && !addedSteps.contains(index)) {
                if (!modifiedSteps.contains(index)) {
                    cursorClip.toggleStep(index, 0, accentHandler.getCurrenVel());
                } else {
                    modifiedSteps.remove(index);
                }
            }
            addedSteps.remove(index);
        } else {
            heldSteps.add(index);
            if (fixedLengthHeld.get()) {
                stepActionFixedLength(index);
            } else if (copyHeld.get()) {
                handleNoteCopyAction(index, note);
            } else {
                if (note == null || note.state() == State.Empty || note.state() == State.NoteSustain) {
                    cursorClip.setStep(index, 0, accentHandler.getCurrenVel(),
                            positionHandler.getGridResolution() * gatePercent);
                    addedSteps.add(index);
                }
            }
        }
    }

    private void handleNoteCopyAction(final int index, final NoteStep note) {
        if (copyNote != null) {
            if (index == copyNote.x()) {
                return;
            }
            final int vel = (int) Math.round(copyNote.velocity() * 127);
            final double duration = copyNote.duration();
            expectedNoteChanges.put(index, copyNote);
            cursorClip.setStep(index, 0, vel, duration);
        } else if (note != null && note.state() == State.NoteOn) {
            copyNote = note;
        }
    }

    private RgbLigthState stepState(final int index) {
        final int steps = positionHandler.getAvailableSteps();
        if (index < steps) {
            final State state = assignments[index] == null ? State.Empty : assignments[index].state();

            if (state == State.Empty) {
                return emptyNoteState(index);
            } else if (state == State.NoteSustain) {
                if (lengthDisplay.get()) {
                    if (index == playingStep) {
                        return padHandler.getCurrentPadColor().getBrightend();
                    }
                    return padHandler.getCurrentPadColor().getVeryDimmed();
                }
                return emptyNoteState(index);
            }

            if (copyNote != null && copyNote.x() == index) {
                if (blinkState % 4 < 2) {
                    return RgbLigthState.GRAY_1;
                }
                return padHandler.getCurrentPadColor();
            }
            if (index == playingStep) {
                return padHandler.getCurrentPadColor().getBrightend();
            }
            return padHandler.getCurrentPadColor();

        }
        return RgbLigthState.OFF;
    }

    private RgbLigthState emptyNoteState(final int index) {
        if (index == playingStep) {
            return RgbLigthState.WHITE;
        }
        if (index / 4 % 2 == 0) {
            return RgbLigthState.GRAY_1;
        } else {
            return RgbLigthState.GRAY_2;
        }
    }

    private void movePattern(final boolean pressed, final int dir) {
        if (pressed) {
            return;
        }
        final List<NoteStep> notes = getOnNotes();
        final int availableSteps = positionHandler.getAvailableSteps();
        cursorClip.clearStepsAtY(0, 0);

        for (final NoteStep noteStep : notes) {
            int pos = noteStep.x() + dir;
            if (pos < 0) {
                pos = availableSteps - 1;
            } else if (pos >= availableSteps) {
                pos = 0;
            }
            if (!shiftActive.get()) {
                expectedNoteChanges.put(pos, noteStep);
            }
            cursorClip.setStep(pos, 0, (int) Math.round(noteStep.velocity() * 127), noteStep.duration());
        }
    }

    private BiColorLightState getPinnedState() {
        return cursorClip.isPinned().get() ? BiColorLightState.HALF : BiColorLightState.OFF;
    }

    private void handleClipPinning(final boolean pressed) {
        if (pressed) {
            cursorClip.isPinned().toggle();
            oled.paramInfo((cursorClip.isPinned().get() ? "UNPIN" : "PIN") + " Clip", "TR:" + cursorTrack.name().get());
        } else {
            oled.clearScreenDelayed();
        }
    }

    private void handleMainEncoder(final int inc) {
        if (accentHandler.isHolding()) {
            accentHandler.handleMainEncoder(inc);
        } else if (resolutionHandler.isHolding()) {
            resolutionHandler.handleMainEncoder(inc);
        } else {
            padHandler.handleMainEncoder(inc);
        }
    }

    private void handeMainEncoderPress(final boolean press) {
        if (accentHandler.isHolding()) {
            accentHandler.handeMainEncoderPress(press);
        } else if (resolutionHandler.isHolding()) {
            resolutionHandler.handeMainEncoderPress(press);
        }
    }

    public BooleanValueObject getShiftActive() {
        return shiftActive;
    }

    public BooleanValueObject getDeleteHeld() {
        return deleteHeld;
    }

    public void notifyBlink(final int blinkTicks) {
        blinkState = blinkTicks;
        clipHandler.notifyBlink(blinkTicks);
    }

    public OledDisplay getOled() {
        return oled;
    }

    private void bindEditButton(final BiColorButton button, final String name, final BooleanValueObject value,
                                final MultiStateHardwareLight stateLight, final BooleanValueObject altValue,
                                final BooleanValueObject altActionHappenedFlag) {
        if (altValue == null) {
            final FunctionInfo info1 = FunctionInfo.INFO1.get(button.getNoteAssign());
            button.bind(mainLayer, value, BiColorLightState.GREEN_FULL, BiColorLightState.OFF);
            mainLayer.bindLightState(() -> BiColorLightState.AMBER_HALF, stateLight);
            value.addValueObserver(active -> handleEditValueChanged(button, active, info1));
            mainLayer.bindLightState(
                    () -> button.isPressed() ? BiColorLightState.AMBER_FULL : BiColorLightState.AMBER_HALF, stateLight);
        } else {
            final BooleanValueObject alternateFunctionActive = new BooleanValueObject();
            final FunctionInfo info1 = FunctionInfo.INFO1.get(button.getNoteAssign());
            value.addValueObserver(active -> handleEditValueChanged(button, active, info1));
            final FunctionInfo info2 = FunctionInfo.INFO2.get(button.getNoteAssign());
            altValue.addValueObserver(active -> handleEditValueChanged(button, active, info2));
            button.bindPressed(mainLayer,
                    pressed -> handleModeButtonWithAlternatePressed(value, altValue, alternateFunctionActive,
                            altActionHappenedFlag, info1, info2, pressed),  //
                    () -> button.isPressed() ? BiColorLightState.GREEN_FULL : BiColorLightState.OFF);
            mainLayer.bindLightState(() -> {
                final boolean active = button.isPressed() && !getShiftActive().get();
                if (alternateFunctionActive.get()) {
                    return active ? BiColorLightState.RED_FULL : BiColorLightState.RED_HALF;
                }
                return active ? BiColorLightState.AMBER_FULL : BiColorLightState.AMBER_HALF;
            }, stateLight);
        }
    }

    private void handleModeButtonWithAlternatePressed(final BooleanValueObject mainValue,
                                                      final BooleanValueObject altValue,
                                                      final BooleanValueObject alternateFunctionActive,
                                                      final BooleanValueObject actionTakenFlag,
                                                      final FunctionInfo info1, final FunctionInfo info2,
                                                      final Boolean pressed) {
        if (pressed) {
            if (alternateFunctionActive.get()) {
                altValue.set(true);
            } else {
                if (getShiftActive().get()) {
                    alternateFunctionActive.set(true);
                    actionTakenFlag.set(true);
                    oled.functionInfo(getPadInfo(), info2.getName(shiftActive.get()), info2.getDetail());
                } else {
                    oled.functionInfo(getPadInfo(), info1.getName(shiftActive.get()), info1.getDetail());
                    mainValue.set(true);
                }
            }
        }

        if (!pressed) {
            mainValue.set(false);
            altValue.set(false);
            if (!actionTakenFlag.get()) {
                alternateFunctionActive.set(false);
            }
            actionTakenFlag.set(false);
            oled.clearScreenDelayed();
        }
    }

    public String getPadInfo() {
        return padHandler.getPadInfo();
    }

    private void handleEditValueChanged(final BiColorButton button, final boolean active, final FunctionInfo info) {
        if (active) {
            if (padHandler.notePlayingEnabled()) {
                padHandler.disableNotePlaying();
            }
            oled.functionInfo(getPadInfo(), info.getName(shiftActive.get()), info.getDetail());
        } else {
            oled.clearScreenDelayed();
            if (padHandler.notePlayingEnabled()) {
                padHandler.applyScale();
            }
        }
    }

    double getGridResolution() {
        return positionHandler.getGridResolution();
    }

    String getDetails(final List<NoteStep> heldNotes) {
        return getPadInfo() + " <" + heldNotes.size() + ">";
    }

    public void registerModifiedSteps(final List<NoteStep> notes) {
        notes.forEach(s -> modifiedSteps.add(s.x()));
    }

    List<NoteStep> getHeldNotes() {
        return heldSteps.stream()//
                .map(idx -> assignments[idx])//
                .filter(ns -> ns != null && ns.state() == State.NoteOn) //
                .collect(Collectors.toList());
    }

    List<NoteStep> getOnNotes() {
        return Arrays.stream(assignments)
                .filter(ns -> ns != null && ns.state() == State.NoteOn)
                .collect(Collectors.toList());
    }

    public void registerPendingAction(final NoteAction action) {
        pendingAction = action;
    }

    public NoteAction getPendingAction() {
        return pendingAction;
    }

    public void clearPendingAction() {
        pendingAction = null;
    }

    private void stepActionFixedLength(final int index) {
        final double newLen = positionHandler.lengthWithLastStep(index);
        adjustMode(newLen);
        cursorClip.getLoopLength().set(newLen);
    }

    private void adjustMode(final double clipLength) {
        final int notes = (int) (clipLength / 0.25);
        adjustMode(notes);
    }

    private void adjustMode(final int notes) {
        if (notes % 8 == 0) {
            cursorClip.launchMode().set("play_with_quantization");
        } else if (clipLaunchModeQuant.get()) {
            cursorClip.launchMode().set("continue_with_quantization");
        } else {
            cursorClip.launchMode().set("continue_immediately");
        }
    }

    private void handleNoteStep(final NoteStep noteStep) {
        final int newStep = noteStep.x();

        assignments[newStep] = noteStep;
        if (expectedNoteChanges.containsKey(newStep)) {
            final NoteStep previousStep = expectedNoteChanges.get(newStep);
            expectedNoteChanges.remove(newStep);
            applyValues(noteStep, previousStep);
        }
    }

    private void applyValues(final NoteStep dest, final NoteStep src) {
        dest.setChance(src.chance());
        dest.setTimbre(src.timbre());
        dest.setPressure(src.pressure());
        dest.setRepeatCount(src.repeatCount());
        dest.setRepeatVelocityCurve(src.repeatVelocityCurve());
        dest.setPan(src.pan());
        dest.setRepeatVelocityEnd(src.repeatVelocityEnd());
        dest.setRecurrence(src.recurrenceLength(), src.recurrenceMask());
        dest.setOccurrence(src.occurrence());
    }

    private void handlePlayingStep(final int playingStep) {
        if (playingStep == -1) {
            this.playingStep = -1;
        }
        this.playingStep = playingStep - positionHandler.getStepOffset();
    }

    @Override
    protected void onActivate() {
        currentLayer = mainLayer;
        mainLayer.activate();
        encoderLayer.activate();
        padHandler.applyScale();
    }

    @Override
    protected void onDeactivate() {
        currentLayer.deactivate();
        shiftLayer.deactivate();
        encoderLayer.deactivate();
        padHandler.disableNotePlaying();
    }

    public void retrigger() {
        cursorClip.launch();
    }

    public StepViewPosition getPositionHandler() {
        return positionHandler;
    }

    PinnableCursorClip getCursorClip() {
        return cursorClip;
    }

    boolean isShiftHeld() {
        return shiftActive.get();
    }

    boolean isCopyHeld() {
        return copyHeld.get();
    }

    boolean isDeleteHeld() {
        return deleteHeld.get();
    }

    boolean isSelectHeld() {
        return selectHeld.get();
    }

    public void exitRecurrenceEdit() {
        recurrenceEditor.exitRecurrenceEdit();
    }

    public void enterRecurrenceEdit(final List<NoteStep> notes) {
        recurrenceEditor.enterRecurrenceEdit(notes);
    }

    public void updateRecurrencLength(final int length) {
        recurrenceEditor.updateLength(length);
    }

    public IntSetValue getHeldSteps() {
        return heldSteps;
    }

    public boolean isPadBeingHeld() {
        return padHandler.isPadBeingHeld();
    }

    public void registerExpectedNoteChange(final int x, final NoteStep noteStep) {
        expectedNoteChanges.put(noteStep.x(), noteStep);
    }

    public BooleanValueObject getLengthDisplay() {
        return lengthDisplay;
    }

    public void notifyMuteAction() {
        muteActionsTaken.set(true);
    }

    public void notifySoloAction() {
        soloActionsTaken.set(true);
    }


}
