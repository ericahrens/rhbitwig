package com.novation.launchpadProMk3;

import com.bitwig.extension.controller.api.*;
import com.bitwig.extensions.framework.Layer;
import com.bitwig.extensions.framework.Layers;
import com.bitwig.extensions.framework.values.StepViewPosition;

import java.util.Arrays;

public class DrumSeqMultilineLayer extends Layer {

    private final PinnableCursorClip cursorClip;
    private final StepViewPosition positionHandler;
    private final DrumSequenceMode parent;
    private int drumScrollOffset;
    private int playingStep;
    boolean markIgnoreOrigLen;
    private LpColor trackColor;
    private final LpColor[] padColors = new LpColor[16];
    private final NoteStep[][] assignments = new NoteStep[8][32];
    private int currentPadIndex;
    private final Layer eightLane;

    public DrumSeqMultilineLayer(final Layers layers, final LaunchpadProMk3ControllerExtension driver,
                                 DrumSequenceMode parent) {
        super(layers, "FOUR_X_SEQUENCER");
        this.parent = parent;
        this.eightLane = new Layer(layers, "EIGHT_LANE");
        ViewCursorControl control = driver.getViewControl();
        CursorTrack cursorTrack = control.getCursorTrack();
        cursorTrack.color().addValueObserver((r, g, b) -> trackColor = ColorLookup.getColor(r, g, b));
        cursorClip = cursorTrack.createLauncherCursorClip("4xCLIP", "4RowClip", 16, 8);
        positionHandler = new StepViewPosition(cursorClip, 16, "8LANE");
        final DrumPadBank drumPadBank = control.getDrumPadBank();
        Arrays.fill(padColors, LpColor.BLACK);
        drumPadBank.scrollPosition().addValueObserver(offset -> {
            drumScrollOffset = offset;
            cursorClip.scrollToKey(drumScrollOffset + currentPadIndex);
        });
        for (int i = 0; i < drumPadBank.getSizeOfBank(); i++) {
            final int index = i;
            DrumPad pad = drumPadBank.getItemAt(i);
            pad.color().addValueObserver((r, g, b) -> padColors[index] = ColorLookup.getColor(r, g, b, trackColor));
        }

        initDrumPadButtons(driver.getHwElements());

        cursorClip.addNoteStepObserver(this::handleNoteStep);
        cursorClip.playingStep().addValueObserver(this::handlePlayingStep);
        cursorClip.getLoopLength().addValueObserver(clipLength -> {
            if (markIgnoreOrigLen) {
                markIgnoreOrigLen = false;
            }
        });

    }

    public void toggleLaneMode() {
        if (!isActive()) {
            setIsActive(true);
            eightLane.setIsActive(true);
            ensureIndexOffset();
        } else {
            eightLane.toggleIsActive();
            ensureIndexOffset();
        }
    }

    public boolean isEightLaneActive() {
        return eightLane.isActive();
    }

    public void setSelectPadIndex(int padIndex) {
        this.currentPadIndex = padIndex;
        ensureIndexOffset();
    }

    private void initDrumPadButtons(HardwareElements hwElements) {
        hwElements.getButton(LabelCcAssignments.DOWN)
                .bind(this, positionHandler::scrollRight, positionHandler.canScrollRight(), LpColor.WHITE);
        hwElements.getButton(LabelCcAssignments.UP)
                .bind(this, positionHandler::scrollLeft, positionHandler.canScrollLeft(), LpColor.WHITE);
        for (int row = 0; row < 8; row++) {
            for (int col = 0; col < 8; col++) {
                final GridButton button = hwElements.getGridButton(row, col);
                int noteIndex = row / 2;
                int stepIndex = (row % 2) * 8 + col;
                button.bindPressed(this, () -> handleSeqSelection(noteIndex, stepIndex));
                button.bindLight(this, () -> getColor(noteIndex, stepIndex));

                int noteIndex8 = row;
                int stepIndex8 = col;
                button.bindPressed(eightLane, () -> handleSeqSelection(noteIndex8, stepIndex8));
                button.bindLight(eightLane, () -> getColor(noteIndex8, stepIndex8));
            }
        }
    }

    private RgbState getColor(int noteIndex, int stepIndex) {
        final int steps = positionHandler.getAvailableSteps();
        NoteStep assignment = assignments[noteIndex][stepIndex];
        LpColor color = padColors[(noteIndex + currentPadIndex) % 16];
        color = color == LpColor.BLACK ? trackColor : color;
        if (stepIndex < steps) {
            if (assignment == null || assignment.state() != NoteStep.State.NoteOn) {
                if (stepIndex == playingStep) {
                    return RgbState.of(2);
                }
                return RgbState.of(1);
            }
            if (parent.isRandomModeActive()) {
                final double chance = assignment.chance();
                if (chance == 1) {
                    if (stepIndex == playingStep) {
                        return RgbState.of(color.getHiIndex());
                    }
                    return RgbState.of(color.getIndex());
                } else {
                    final LpColor chanceColor = parent.toColor(chance);
                    if (stepIndex == playingStep) {
                        return RgbState.of(chanceColor.getHiIndex());
                    }
                    return RgbState.of(chanceColor.getIndex());
                }
            } else {
                if (stepIndex == playingStep) {
                    return RgbState.of(color.getHiIndex());
                }
                return RgbState.of(color.getIndex());
            }
        }
        return RgbState.of(0);
    }

    private void handleSeqSelection(final int noteIndex, final int stepIndex) {
        final NoteStep note = assignments[noteIndex][stepIndex];
        if (parent.isFixedLengthHeld()) {
            parent.stepActionFixedLength(stepIndex);
        } else if (parent.isRandomModeActive()) {
            stepActionRandomMode(noteIndex, stepIndex, note);
        } else {
            if (note == null || note.state() == NoteStep.State.Empty) {
                cursorClip.setStep(stepIndex, noteIndex, parent.getRefVelocity(),
                        positionHandler.getGridResolution() * parent.getGatePercent());
            } else {
                cursorClip.toggleStep(stepIndex, noteIndex, parent.getRefVelocity());
            }
        }
    }

    void stepActionRandomMode(final int noteIndex, final int stepIndex, final NoteStep note) {
        DrumSequenceMode.RndConfig rndConfig = parent.getCurrentRndValue();
        final double setProb = rndConfig.getProb();
        if (note != null && note.state() == NoteStep.State.NoteOn) {
            final double prob = note.chance();
            if (prob == setProb) {
                note.setChance(1);
            } else {
                note.setChance(setProb);
            }
        } else if (note == null || note.state() == NoteStep.State.Empty) {
            cursorClip.setStep(stepIndex, noteIndex, parent.getRefVelocity(),
                    positionHandler.getGridResolution() * parent.getGatePercent());
            //probValues[index] = RND_VALUES[selectedRndIndex].prob;
        }
    }

    private void handleNoteStep(final NoteStep noteStep) {
        assignments[noteStep.y()][noteStep.x()] = noteStep;
//        if (probValues[noteStep.x()] != null && noteStep.state() == NoteStep.State.NoteOn) {
//            noteStep.setChance(probValues[noteStep.x()]);
//            probValues[noteStep.x()] = null;
//        }
    }

    private void handlePlayingStep(final int playingStep) {
        if (playingStep == -1) {
            this.playingStep = -1;
        }
        this.playingStep = playingStep - positionHandler.getStepOffset();
    }

    public void setGridResolution(double gridRate) {
        positionHandler.setGridResolution(gridRate);
    }

    @Override
    protected void onActivate() {
        super.onActivate();
        ensureIndexOffset();
    }

    private void ensureIndexOffset() {
        if (eightLane.isActive()) {
            this.currentPadIndex = Math.min(this.currentPadIndex, 8);
            cursorClip.scrollToKey(drumScrollOffset + currentPadIndex);
            positionHandler.setStepsPerPage(8);
        } else {
            this.currentPadIndex = Math.min(this.currentPadIndex, 12);
            cursorClip.scrollToKey(drumScrollOffset + currentPadIndex);
            positionHandler.setStepsPerPage(16);
        }
    }

    @Override
    protected void onDeactivate() {
        super.onDeactivate();
        eightLane.setIsActive(false);
    }
}
