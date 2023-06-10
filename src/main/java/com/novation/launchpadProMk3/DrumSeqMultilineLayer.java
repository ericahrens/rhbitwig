package com.novation.launchpadProMk3;

import com.bitwig.extension.controller.api.*;
import com.bitwig.extensions.framework.Layer;
import com.bitwig.extensions.framework.Layers;
import com.bitwig.extensions.rh.StepViewPosition;

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
    private final NoteStep[][] assignments = new NoteStep[4][32];
    private int currentPadIndex;

    public DrumSeqMultilineLayer(final Layers layers, final LaunchpadProMk3ControllerExtension driver,
                                 DrumSequenceMode parent) {
        super(layers, "FOUR_X_SEQUENCER");
        this.parent = parent;
        ViewCursorControl control = driver.getViewControl();
        CursorTrack cursorTrack = control.getCursorTrack();
        cursorTrack.color().addValueObserver((r, g, b) -> trackColor = ColorLookup.getColor(r, g, b));
        cursorClip = cursorTrack.createLauncherCursorClip("4xCLIP", "4RowClip", 16, 4);
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

        positionHandler = new StepViewPosition(cursorClip);
    }

    public void setSelectPadIndex(int padIndex) {
        this.currentPadIndex = Math.min(padIndex, 12);
        cursorClip.scrollToKey(drumScrollOffset + currentPadIndex);
    }

    private void initDrumPadButtons(HardwareElements hwElements) {
        for (int row = 0; row < 8; row++) {
            for (int col = 0; col < 8; col++) {
                final GridButton button = hwElements.getGridButton(row, col);
                int noteIndex = row / 2;
                int stepIndex = (row % 2) * 8 + col;
                button.bindPressed(this, () -> handleSeqSelection(noteIndex, stepIndex));
                button.bindLight(this, () -> getColor(noteIndex, stepIndex));
            }
        }
    }

    private RgbState getColor(int noteIndex, int stepIndex) {
        final int steps = positionHandler.getAvailableSteps();
        NoteStep assignment = assignments[noteIndex][stepIndex];
        LpColor color = padColors[(noteIndex + currentPadIndex) % 16];
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
            stepActionRandomMode(stepIndex, note);
        } else {
            if (note == null || note.state() == NoteStep.State.Empty) {
                cursorClip.setStep(stepIndex, noteIndex, parent.getRefVelocity(),
                        positionHandler.getGridResolution() * parent.getGatePercent());
            } else {
                cursorClip.toggleStep(stepIndex, noteIndex, parent.getRefVelocity());
            }
        }
    }

    void stepActionRandomMode(final int index, final NoteStep note) {
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
            cursorClip.setStep(index, 0, parent.getRefVelocity(),
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
}
