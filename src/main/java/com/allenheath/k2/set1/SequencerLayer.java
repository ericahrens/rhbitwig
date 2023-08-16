package com.allenheath.k2.set1;

import com.bitwig.extension.controller.api.*;
import com.bitwig.extensions.common.DrumSequencerSource;
import com.bitwig.extensions.context.GlobalContext;
import com.bitwig.extensions.framework.Layer;
import com.bitwig.extensions.framework.Layers;

import java.util.Arrays;

public class SequencerLayer extends Layer implements DrumSequencerSource.ChangeListener {
    public static final String LISTENER_ID = "k2.sequencer";
    private final PinnableCursorClip cursorClip;
    private final DrumPadBank drumPadBank;
    private final PadGrouping padGrouping;
    private int drumPadOffset = 36;
    private int selectedPadIndex = 0;
    private double clipLength;
    private NoteStep[] assignments = new NoteStep[16];
    private int playingStep;
    private final boolean[] playing = new boolean[8];

    private final DrumSequencerSource selfProvider = new DrumSequencerSource() {
        @Override
        public int getRefVelocity() {
            return 100;
        }

        @Override
        public double getGatePercent() {
            return 0.9;
        }

        @Override
        public double getGridResolution() {
            return 0.25;
        }

        @Override
        public int getNoteOffset() {
            return drumPadOffset + selectedPadIndex;
        }

        @Override
        public void registerListener(String id, ChangeListener listener) {
        }

        @Override
        public void removeListener(String id) {
        }

    };
    private DrumSequencerSource drumSequencerSource = selfProvider;

    public SequencerLayer(Layers layers, HwElements hwElements, ViewCursorControl viewCursorControl,
                          PadGrouping padGrouping) {
        super(layers, "SEQUENCER");

        this.padGrouping = padGrouping;
        cursorClip = viewCursorControl.getCursorTrack().createLauncherCursorClip(16, 1);
        drumPadBank = viewCursorControl.getDrumPadBank();
        final NoteStep[] assignments = new NoteStep[32];

        drumPadBank.scrollPosition().addValueObserver(drumPosition -> {
            adjustPosition();
        });

        viewCursorControl.getCursorTrack().playingNotes().addValueObserver(notes -> handleNotePlaying(notes));

        cursorClip.addNoteStepObserver(this::handleNoteStep);
        cursorClip.playingStep().addValueObserver(this::handlePlayingStep);
        cursorClip.getLoopLength().addValueObserver(clipLength -> {
            this.clipLength = clipLength;
        });

        for (int i = 0; i < 8; i++) {
            int index = i;
            MultiStateHardwareLight light = hwElements.getChannelLight(i);
            bindLightState(() -> this.playingState(index), light);
        }
        for (int i = 0; i < 2; i++) {
            for (int j = 0; j < 8; j++) {
                final int columnIndex = j;
                int stepIndex = i * 8 + j;
                StateButton button = hwElements.getStateButton(i, j);
                button.bindPressed(this, () -> {
                    this.handlePressedStep(stepIndex);
                });
                button.bindLight(this, () -> getStepLight(stepIndex));
            }
        }
        GlobalContext.getContext()
                .registerForListener(DrumSequencerSource.class, LISTENER_ID, this::connectToSequencer,
                        this::disconnectFromSequencerProvider);
        this.setIsActive(true);
    }

    public void exit() {
        GlobalContext.getContext().removeListener(LISTENER_ID);
    }

    private void connectToSequencer(DrumSequencerSource source) {
        AllenHeathK2ControllerExtension.println(" Connected to Drum Sequencer gr=%f  gp=%f", source.getGridResolution(),
                source.getGatePercent());
        this.drumSequencerSource = source;
        source.registerListener(LISTENER_ID, this);
    }

    private void disconnectFromSequencerProvider() {
        AllenHeathK2ControllerExtension.println(" !!! Disconnect from Drum Sequencer because sequencer gone");
        this.drumSequencerSource = selfProvider;
    }

    @Override
    public void changePadsOffset(int bankOffset, int padOffset, int bankSize) {
        this.selectedPadIndex = padOffset;
        adjustPosition();
    }

    @Override
    public void changeGridResolution(double resolution) {
        AllenHeathK2ControllerExtension.println(" GRID Resolution = %f", resolution);
        cursorClip.setStepSize(resolution);
    }

    private void handlePressedStep(int stepIndex) {
        final NoteStep note = assignments[stepIndex];
        if (note == null || note.state() == NoteStep.State.Empty) {
            cursorClip.setStep(stepIndex, 0, drumSequencerSource.getRefVelocity(),
                    drumSequencerSource.getGridResolution() * drumSequencerSource.getGatePercent());
        } else {
            cursorClip.toggleStep(stepIndex, 0, 0);
        }
    }

    private RedGreenButtonState getStepLight(int stepIndex) {
        if (stepIndex == playingStep) {
            return RedGreenButtonState.YELLOW;
        }
        if (assignments[stepIndex] != null && assignments[stepIndex].state() == NoteStep.State.NoteOn) {
            return RedGreenButtonState.GREEN;
        }
        return RedGreenButtonState.OFF;
    }

    private void handleNotePlaying(PlayingNote[] notes) {
        Arrays.fill(playing, false);
        for (final PlayingNote playingNote : notes) {
            final int baseIndex = playingNote.pitch() - 36;
            if (baseIndex >= 0 && baseIndex < 16) {
                int slotIndex = padGrouping.padIndexToSlot(baseIndex);
                if (slotIndex >= 0 && slotIndex < 8) {
                    playing[slotIndex] = true;
                }
            }
        }
    }

    private void handlePlayingStep(final int playingStep) {
        if (playingStep == -1) {
            this.playingStep = -1;
        }
        this.playingStep = playingStep;
    }

    private void handleNoteStep(NoteStep noteStep) {
        assignments[noteStep.x()] = noteStep;
    }

    private RedGreenButtonState playingState(int trackIndex) {
        return playing[trackIndex] ? RedGreenButtonState.GREEN : RedGreenButtonState.OFF;
    }

    private void adjustPosition() {
        int position = drumPadOffset + selectedPadIndex;
        if (position >= 0 && position < 128) {
            cursorClip.scrollToKey(drumPadOffset + selectedPadIndex);
        }
    }
}
