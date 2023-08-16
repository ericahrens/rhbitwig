package com.novation.launchpadProMk3;

import com.bitwig.extension.controller.api.*;
import com.bitwig.extension.controller.api.NoteStep.State;
import com.bitwig.extensions.common.DrumSequencerSource;
import com.bitwig.extensions.context.GlobalContext;
import com.bitwig.extensions.framework.Layer;
import com.bitwig.extensions.framework.Layers;
import com.bitwig.extensions.rh.BooleanValueObject;
import com.bitwig.extensions.rh.StepViewPosition;

import java.util.*;
import java.util.function.BooleanSupplier;

public class DrumSequenceMode extends Layer implements DrumSequencerSource {
    private final NoteInput noteInput;
    private final Integer[] notesToDrumTable = new Integer[128];
    private final Integer[] velocityTable = new Integer[128];
    private final int[] notesToPadsTable = new int[128];
    private final BooleanValueObject[] playing = new BooleanValueObject[16];
    private final boolean[] drumTracker = new boolean[16];
    private final ControllerHost host;
    private int drumScrollOffset = 0;
    private final List<PadContainer> pads = new ArrayList<>();
    double gatePercent = 0.98;
    private final Set<Integer> holdNotes = new HashSet<>();

    private static final int[] PAD_NOTES = new int[]{11, 12, 13, 14, 21, 22, 23, 24, 31, 32, 33, 34, 41, 42, 43, 44};
    private static final double[] ARP_RATES = new double[]{0.125, 0.25, 0.5, 1.0, 1.0 / 12, 1.0 / 6, 1.0 / 3, 2.0 / 3};
    private static final double[] GRID_RATES = new double[]{0.125, 0.25, 0.5, 1.0, //
            1.0 / 12, 1.0 / 6, 1.0 / 3, 2.0 / 3};
    private static final LpColor[] ARP_BUTTON_COLORS = new LpColor[]{LpColor.BLUE, LpColor.BLUE, LpColor.BLUE, LpColor.BLUE, LpColor.PURPLE, LpColor.PURPLE, LpColor.PURPLE, LpColor.PURPLE};
    private static final LpColor[] GRID_BUTTON_COLORS = new LpColor[]{LpColor.PINK, LpColor.PINK, LpColor.PINK, LpColor.PINK, LpColor.PINK, LpColor.RED, LpColor.RED, LpColor.RED};

    static final RndConfig[] RND_VALUES = new RndConfig[]{RndConfig.P25, RndConfig.P50, RndConfig.P75};

    private int selectedRefVel = 0;
    private int selectedRndIndex = 2;
    private final int[] velTable = new int[]{127, 120, 110, 105, 100, 90, 80, 75, 70, 60, 50, 40, 30, 20, 10, 5};

    private int selectedArpIndex = 1;
    private int selectedGridIndex = 1;
    private double currentArpRate = ARP_RATES[1];

    private final PinnableCursorClip cursorClip;
    private final StepViewPosition positionHandler;
    private final BooleanValueObject sendHeld = new BooleanValueObject();
    private final BooleanValueObject altLaunchHeld = new BooleanValueObject();

    private final Arpeggiator arp;
    private final NoteStep[] assignments = new NoteStep[32];
    private final LpColor[] slotColors = new LpColor[8];
    private final Double[] probValues = new Double[32];

    private int playingStep;
    private int selectedSlotIndex = -1;
    private int currentSendsSlot = 0;
    private int selectedPadIndex = -1;

    private LpColor currentPadColor = LpColor.GREEN;
    private PadContainer selectedPad;
    private final BooleanValueObject fixedLengthHeld = new BooleanValueObject();
    private final BooleanValueObject notePlayingActive = new BooleanValueObject();
    private final BooleanValueObject stopButtonHeld = new BooleanValueObject();
    private final BooleanValueObject clipLaunchModeQuantize = new BooleanValueObject();
    private final BooleanValueObject randomModeActive = new BooleanValueObject();

    private final LpStateValues states;

    private final Layer mainLayer;
    private final Layer muteLayer;
    private final Layer soloLayer;
    private final Layer velLayer;
    private final Layer clipAreaNavLayer;
    private final Layer sendsLayer;
    private final Layer shiftLayer;
    private final Layer randomLayer;
    private final DrumSeqMultilineLayer multilineSeqLayer;

    private Layer currentLayer;

    private double originalClipLength;
    private boolean markIgnoreOrigLen = false;
    private final ClipLauncherSlotBank slotBank;
    private final CursorTrack cursorTrack;

    private long downTimeChordButton = 0;
    private long chordTapCount = 0;

    private Map<String, DrumSequencerSource.ChangeListener> changeListeners = new HashMap<>();

    enum RndConfig {
        P10(0.10, LpColor.PINK),
        P25(0.25, LpColor.MAGENTA),
        P50(0.50, LpColor.AMBER),
        P75(0.75, LpColor.OCEAN),
        P90(0.90, LpColor.BLUE);

        private final double prob;
        private final LpColor refColor;

        RndConfig(final double prob, final LpColor refColor) {
            this.prob = prob;
            this.refColor = refColor;
        }

        public double getProb() {
            return prob;
        }

        public LpColor getRefColor() {
            return refColor;
        }
    }

    private class PadContainer {
        private LpColor padColor = LpColor.WHITE;
        private final BooleanValueObject playing;
        private boolean selected;
        private final int index;
        private final DrumPad pad;
        private boolean exists;

        public PadContainer(final int index, final DrumPad pad, final BooleanValueObject playing) {
            super();
            this.index = index;
            this.pad = pad;
            this.playing = playing;
            this.playing.markInterested();

            for (int i = 0; i < 8; i++) {
                pad.sendBank().getItemAt(i).exists().markInterested();
                pad.sendBank().getItemAt(i).value().markInterested();
            }
            pad.mute().markInterested();
            pad.solo().markInterested();
            pad.name().markInterested();
            pad.addIsSelectedInEditorObserver(selected -> {
                this.selected = selected;
                if (this.selected) {
                    currentPadColor = padColor;
                    selectedPad = this;
                    focusOnSelectedPad();
                    selectedPadIndex = index;
                    notifyNoteOffset();
                    multilineSeqLayer.setSelectPadIndex(index);
                }
            });
            pad.exists().addValueObserver(exists -> this.exists = exists);
            pad.color().addValueObserver((r, g, b) -> padColor = ColorLookup.getColor(r, g, b, LpColor.BLACK));
        }

        public RgbState mutingColors() {
            final boolean muted = pad.mute().get();
            if (muted) {
                return playing.returnTrueFalse(RgbState.of(5), //
                        RgbState.of(60, LightState.PULSING));
            }
            return playing.returnTrueFalse(RgbState.of(2), RgbState.of(1));
        }

        public RgbState soloingColors() {
            final boolean soloed = pad.solo().get();
            if (soloed) {
                return playing.returnTrueFalse(RgbState.of(14), //
                        RgbState.of(13, LightState.PULSING));
            }
            return playing.returnTrueFalse(RgbState.of(3), RgbState.of(1));
        }

        public RgbState getColor() {
            if (!exists) {
                return RgbState.of(0);
            }
            if (selected) {
                return playing.returnTrueFalse(RgbState.of(LpColor.WHITE), //
                        RgbState.of(padColor.getHiIndex() - 1));
            }
            return playing.returnTrueFalse(RgbState.of(padColor.getHiIndex()), //
                    RgbState.of(padColor.getLowIndex()));
        }

        public RgbState sendStatusColor() {
            if (!exists) {
                return RgbState.of(0);
            }
            final Send send = pad.sendBank().getItemAt(currentSendsSlot);
            final double value = send.get();
            if (value == 0) {
                return RgbState.of(LpColor.BLUE_LO);
            } else if (value == 1.0) {
                return RgbState.of(LpColor.BLUE_HI);
            }
            return RgbState.of(LpColor.BLUE);
        }

        public void toggleSendValue() {
            final Send send = pad.sendBank().getItemAt(currentSendsSlot);
            final double value = send.value().get();
            if (value > 0) {
                send.value().setImmediately(0.0);
            } else {
                send.value().setImmediately(1.0);
            }

        }

        public void select() {
            pad.selectInEditor();
        }

        public boolean hasSend(final int index) {
            return pad.sendBank().getItemAt(index).exists().get();
        }

        public void resetSend() {
            for (int i = 0; i < 8; i++) {
                pad.sendBank().getItemAt(i).value().setImmediately(0);
            }
        }

    }

    public DrumSequenceMode(final Layers layers, final LaunchpadProMk3ControllerExtension driver) {
        super(layers, "DRUM_SEQUENCE_LAYER");
        HardwareElements hwElements = driver.getHwElements();
        mainLayer = new Layer(getLayers(), getName() + "_MAIN");
        shiftLayer = new Layer(getLayers(), getName() + "_SHIFT");
        muteLayer = new Layer(getLayers(), getName() + "_MUTE");
        soloLayer = new Layer(getLayers(), getName() + "_SOLO");
        velLayer = new Layer(getLayers(), getName() + "_VEL");
        clipAreaNavLayer = new Layer(getLayers(), getName() + "_NAV");
        sendsLayer = new Layer(getLayers(), getName() + "_VEL");
        randomLayer = new Layer(getLayers(), getName() + "_RANDOM");
        multilineSeqLayer = new DrumSeqMultilineLayer(layers, driver, this);
        currentLayer = mainLayer;
        this.host = driver.getHost();
        noteInput = driver.getNoteInput();

        final ViewCursorControl control = driver.getViewControl();
        setupPlaying(control);

        cursorTrack = driver.getViewControl().getCursorTrack();

        cursorClip = cursorTrack.createLauncherCursorClip("SQClip", "SQClip", 32, 1);

        slotBank = cursorTrack.clipLauncherSlotBank();

        cursorTrack.position().addValueObserver(v -> cursorClip.clipLauncherSlot().showInEditor());

        randomModeActive.addValueObserver(rndActive -> {
            if (rndActive) {
                randomLayer.activate();
            } else {
                randomLayer.deactivate();
            }
        });
        
        cursorClip.addNoteStepObserver(this::handleNoteStep);
        cursorClip.playingStep().addValueObserver(this::handlePlayingStep);
        cursorClip.getLoopLength().addValueObserver(clipLength -> {
            if (markIgnoreOrigLen) {
                markIgnoreOrigLen = false;
            } else {
                originalClipLength = clipLength;
            }
        });
        positionHandler = new StepViewPosition(cursorClip, 32, "REGULAR");

        initDrumPadButtons(driver);
        initExtendSection(driver);
        initSequenceSection(driver);
        initSpecialButtons(driver);
        final Application application = driver.getApplication();
        states = driver.getStates();

        states.getDuplicateButtonPressed().addValueObserver(pressed -> handleDuplication(application, pressed));
        states.getClearButtonPressed().addValueObserver(this::handleClear);
        states.getNoteRepeatActive().addValueObserver(this::handleNoteRepeatChanged);
        states.getMuteButtonPressed().addValueObserver(this::handleMuteMode);
        states.getSoloButtonPressed().addValueObserver(this::handleSoloMode);
        states.getVolumeButtonPressed().addValueObserver(this::handleVelMode);
        states.getShiftModeActive().addValueObserver(active -> {
            if (active) {
                shiftLayer.activate();
            } else {
                shiftLayer.deactivate();
            }
        });
        arp = noteInput.arpeggiator();
        arp.usePressureToVelocity().set(false);
        // arp.shuffle().set(true);
        arp.mode().set("all"); // that's the note repeat way
        arp.octaves().set(0);
        arp.humanize().set(0);
        arp.isFreeRunning().set(false);

        notePlayingActive.addValueObserver(active -> {
            if (active) {
                if (multilineSeqLayer.isActive()) {
                    multilineSeqLayer.setIsActive(false);
                }
                applyScale();
            } else if (!notePlayingEnabled()) {
                disableNotePlaying();
            }
        });
        assignNoteRepeat(hwElements.getTrackSelectButtons());
        assignGridResolution(hwElements.getSceneLaunchButtons());
        GlobalContext.getContext().provideListener(DrumSequencerSource.class, this);
    }

    private void initSpecialButtons(LaunchpadProMk3ControllerExtension driver) {
        final HardwareElements hwElements = driver.getHwElements();
        hwElements.getButton(LabelCcAssignments.FIXED_LENGTH).bindPressed(mainLayer, fixedLengthHeld, LpColor.PURPLE);
        LabeledButton quantizeButton = hwElements.getButton(LabelCcAssignments.QUANTIZE);
        quantizeButton.bind(mainLayer, this::macroLaunchNote, LpColor.CYAN);
        driver.bindRecQuantize(shiftLayer, quantizeButton);

        hwElements.getButton(LabelCcAssignments.NOTE)
                .bindToggle(mainLayer, notePlayingActive, LpColor.LIME_LO, LpColor.BLACK);
        hwElements.getButton(LabelCcAssignments.CUSTOM)
                .bindToggle(mainLayer, randomModeActive, LpColor.GREEN_SPRING, LpColor.BLACK);
        hwElements.getButton(LabelCcAssignments.STOP_CLIP_SWING).bindPressed(mainLayer, stopButtonHeld, LpColor.RED);
        hwElements.getButton(LabelCcAssignments.CHORD).bindPressed(mainLayer, this::handleMultilanePressed);
        hwElements.getButton(LabelCcAssignments.CHORD).bindReleased(mainLayer, this::handleMultilaneReleased);
        hwElements.getButton(LabelCcAssignments.CHORD)
                .bindLight(mainLayer,
                        () -> multilineSeqLayer.isActive() ? (multilineSeqLayer.isEightLaneActive() ? RgbState.of(60,
                                LightState.PULSING) : RgbState.of(LpColor.ORANGE)) : RgbState.of(LpColor.BLACK));

        final LabeledButton deviceButton = hwElements.getButton(LabelCcAssignments.DEVICE_TEMPO);
        final ViewCursorControl control = driver.getViewControl();
        final PinnableCursorDevice device = control.getPrimaryDevice();

        device.isPinned().markInterested();
        device.deviceType().markInterested();
        cursorTrack.isPinned().markInterested();
        cursorClip.isPinned().markInterested();
        //cursorClip.launchMode().addValueObserver(mode -> DebugConsole.println(" MODE = %s", mode));
        deviceButton.bind(mainLayer, () -> {
            if (!device.hasDrumPads().get()) {
                return;
            }
            if (device.isPinned().get()) {
                device.isPinned().set(false);
                cursorTrack.isPinned().set(false);
                cursorClip.isPinned().set(false);
            } else {
                device.isPinned().set(true);
                cursorTrack.isPinned().set(true);
                cursorClip.isPinned().set(true);
            }
        }, () -> {
            if (device.isPinned().get()) {
                return RgbState.of(LpColor.MAGENTA_HI);
            } else {
                return RgbState.of(LpColor.MAGENTA_LO);
            }
        });
        cursorTrack.hasNext().markInterested();
        cursorTrack.hasPrevious().markInterested();

        hwElements.getButton(LabelCcAssignments.DOWN)
                .bind(mainLayer, positionHandler::scrollRight, positionHandler.canScrollRight(), LpColor.WHITE);
        hwElements.getButton(LabelCcAssignments.UP)
                .bind(mainLayer, positionHandler::scrollLeft, positionHandler.canScrollLeft(), LpColor.WHITE);

        hwElements.getButton(LabelCcAssignments.SENDS_TAP).bindPressed(mainLayer, this::handleSendButton, LpColor.BLUE);

        hwElements.getButton(LabelCcAssignments.PAN)
                .bindToggle(mainLayer, clipLaunchModeQuantize, LpColor.AMBER_HI, LpColor.AMBER_LO);
    }

    private void handleSendButton(final Boolean pressed) {
        sendHeld.set(pressed);
        if (states.getClearButtonPressed().get()) {
            if (pressed) {
                for (final PadContainer pad : pads) {
                    pad.resetSend();
                }
            } else {
                handleSendsMode(false);
            }
        } else {
            handleSendsMode(pressed);
        }
    }

    private void handleMultilanePressed() {
        downTimeChordButton = System.currentTimeMillis();
    }

    private void handleMultilaneReleased() {
        int detectedTaps = getChordTapCount();
        if (detectedTaps == 2) {
            handleDoubleTap();
        } else if (detectedTaps == 1) {
            handleSingleTap();
        }
    }

    private void handleSingleTap() {
        multilineSeqLayer.toggleIsActive();
        if (multilineSeqLayer.isActive() && notePlayingEnabled()) {
            notePlayingActive.set(false);
            states.getNoteRepeatActive().set(false);
        }
    }

    private void handleDoubleTap() {
        multilineSeqLayer.toggleLaneMode();
        if (multilineSeqLayer.isActive() && notePlayingEnabled()) {
            notePlayingActive.set(false);
            states.getNoteRepeatActive().set(false);
        }
    }

    private int getChordTapCount() {
        long time = System.currentTimeMillis() - downTimeChordButton;
        if (time < 150) {
            if (chordTapCount > 0) {
                chordTapCount = 0;
                return 2;
            }
            chordTapCount++;
            host.scheduleTask(() -> {
                if (chordTapCount == 1) {
                    chordTapCount = 0;
                    handleSingleTap();
                }
            }, 150);
            return 0;
        }
        chordTapCount = 0;
        downTimeChordButton = 0;
        return 1;
    }

    private void initDrumPadButtons(final LaunchpadProMk3ControllerExtension driver) {
        HardwareElements hwElements = driver.getHwElements();
        final ViewCursorControl control = driver.getViewControl();
        for (int row = 4; row < 8; row++) {
            for (int col = 0; col < 4; col++) {
                final GridButton button = hwElements.getGridButton(row, col);
                final int index = (7 - row) * 4 + col;
                final PadContainer pad = new PadContainer(index, control.getDrumPadBank().getItemAt(index),
                        playing[index]);
                pads.add(pad);
                button.bindPressed(mainLayer, () -> handlePadSelection(pad));
                button.bindLight(mainLayer, pad::getColor);
                button.bindToggle(muteLayer, pad.pad.mute());
                button.bindLight(muteLayer, pad::mutingColors);
                button.bindToggle(soloLayer, pad.pad.solo());
                button.bindLight(soloLayer, pad::soloingColors);
                button.bind(sendsLayer, pad::toggleSendValue);
                button.bindLight(sendsLayer, pad::sendStatusColor);
            }
        }
        pads.sort(Comparator.comparingInt(pc -> pc.index));
    }

    private void initSequenceSection(final LaunchpadProMk3ControllerExtension driver) {
        HardwareElements hwElements = driver.getHwElements();
        for (int row = 0; row < 4; row++) {
            for (int col = 0; col < 8; col++) {
                final GridButton button = hwElements.getGridButton(row, col);
                final int index = row * 8 + col;
                button.bindPressed(mainLayer, p -> handleSeqSelection(index, p), () -> stepState(index));
            }
        }
    }

    private void initExtendSection(final LaunchpadProMk3ControllerExtension driver) {
        HardwareElements hwElements = driver.getHwElements();
        for (int row = 4; row < 6; row++) {
            for (int col = 4; col < 8; col++) {
                if (col == 7 && row == 5) {
                    continue;
                }
                final GridButton button = hwElements.getGridButton(row, col);
                final int index = (row - 4) * 4 + col - 4;

                final ClipLauncherSlot cs = slotBank.getItemAt(index);
                prepareClipSlot(index, cs);
                button.bindPressed(mainLayer, p -> handleClip(index, cs, p), () -> getClipSate(index, cs));
                button.bindPressed(clipAreaNavLayer, p -> handlePositionSelection(index, p), () -> lengthState(index));
            }
        }
        GridButton altControlButton = hwElements.getGridButton(5, 7);
        altControlButton.bind(mainLayer, altLaunchHeld);
        altControlButton.bindLight(mainLayer,
                () -> altLaunchHeld.get() ? RgbState.of(2, LightState.PULSING) : RgbState.of(2));

        for (int row = 4; row < 8; row++) {
            for (int col = 4; col < 8; col++) {
                final GridButton button = hwElements.getGridButton(row, col);
                final int index = (row - 4) * 4 + col - 4;
                button.bindPressed(velLayer, p -> handleVelocitySelection(index, p), () -> velocityValues(index));
                button.bindPressed(randomLayer, p -> handleRndSelection(index, p), () -> rndValues(index));
            }
        }
        final PadContainer refPad = pads.get(0);
        for (int row = 6; row < 8; row++) {
            for (int col = 4; col < 8; col++) {
                final GridButton button = hwElements.getGridButton(row, col);
                final int index = (row - 6) * 4 + col - 4;
                button.bind(sendsLayer, () -> selectSendSlot(index, refPad));
                button.bindLight(sendsLayer, () -> sendSlotColor(index, refPad));
            }
        }

        final GridButton retriggerButton = hwElements.getGridButton(6, 4);
        retriggerButton.bind(mainLayer, cursorClip::launch, LpColor.GREEN);
        final GridButton originalLengthButton = hwElements.getGridButton(6, 5);
        originalLengthButton.bind(mainLayer, this::setBackToOriginalLength, LpColor.PURPLE);
        final GridButton len8Button = hwElements.getGridButton(6, 6);
        len8Button.bind(mainLayer, () -> setLengthByNotes(8), LpColor.YELLOW);
        final GridButton len5Button = hwElements.getGridButton(6, 7);
        len5Button.bind(mainLayer, () -> setLengthByNotes(5), LpColor.YELLOW);

        final GridButton len4Button = hwElements.getGridButton(7, 4);
        len4Button.bind(mainLayer, () -> setLengthByNotes(4), LpColor.YELLOW);
        final GridButton len3Button = hwElements.getGridButton(7, 5);
        len3Button.bind(mainLayer, () -> setLengthByNotes(3), LpColor.YELLOW);
        final GridButton len2Button = hwElements.getGridButton(7, 6);
        len2Button.bind(mainLayer, () -> setLengthByNotes(2), LpColor.YELLOW);
        final GridButton len1Button = hwElements.getGridButton(7, 7);
        len1Button.bind(mainLayer, () -> setLengthByNotes(1), LpColor.YELLOW);
    }

    private void prepareClipSlot(int index, ClipLauncherSlot cs) {
        cs.color().addValueObserver((r, g, b) -> slotColors[index] = ColorLookup.getColor(r, g, b, LpColor.BLACK));
        cs.isSelected().addValueObserver(selected -> {
            if (selected) {
                selectedSlotIndex = index;
            }
        });
        slotColors[index] = ColorLookup.getColor(cs.color().get(), LpColor.BLACK);
        cs.exists().markInterested();
        cs.hasContent().markInterested();
        cs.isPlaybackQueued().markInterested();
        cs.isPlaying().markInterested();
        cs.isRecording().markInterested();
        cs.isRecordingQueued().markInterested();
        cs.isSelected().markInterested();
        cs.isStopQueued().markInterested();
    }

    public void macroLaunchNote() {
        setLengthByNotes(16);
        cursorClip.launch();
    }

    // continue_immediately
    // continue_with_quantization
    public void setLengthByNotes(final int notes) {
        markIgnoreOrigLen = true;
        adjustMode(notes);
        cursorClip.getLoopLength().set(notes * 0.25);
    }

    private void adjustMode(final double clipLength) {
        final int notes = (int) (clipLength / 0.25);
        adjustMode(notes);
    }

    private void adjustMode(final int notes) {
        if (notes % 8 == 0) {
            cursorClip.launchMode().set("default");
        } else if (clipLaunchModeQuantize.get()) {
            cursorClip.launchMode().set("synced");
        } else {
            cursorClip.launchMode().set("from_start");
        }
    }

    @Override
    public double getGatePercent() {
        return gatePercent;
    }

    @Override
    public int getRefVelocity() {
        return velTable[selectedRefVel];
    }

    @Override
    public double getGridResolution() {
        return positionHandler.getGridResolution();
    }

    @Override
    public int getNoteOffset() {
        return drumScrollOffset + selectedPadIndex;
    }

    @Override
    public void registerListener(String id, ChangeListener listener) {
        if (!changeListeners.containsKey(id)) {
            changeListeners.put(id, listener);
        }
    }

    @Override
    public void removeListener(String id) {
        changeListeners.remove(id);
    }

    private void notifyNoteOffset() {
        final int padIndex = selectedPadIndex == -1 ? 0 : selectedPadIndex;
        changeListeners.entrySet().forEach(entry -> entry.getValue().changePadsOffset(drumScrollOffset, padIndex, 16));
    }

    private void notifyGridResolution(double resolution) {
        changeListeners.entrySet().forEach(entry -> entry.getValue().changeGridResolution(resolution));
    }

    public void setBackToOriginalLength() {
        adjustMode(originalClipLength);
        cursorClip.getLoopLength().set(originalClipLength);
    }

    public void selectSendSlot(final int index, final PadContainer refPad) {
        if (refPad.hasSend(index)) {
            currentSendsSlot = index;
        }
    }

    public RgbState sendSlotColor(final int index, final PadContainer refPad) {
        if (!refPad.hasSend(index)) {
            return RgbState.of(0);
        }
        if (index == currentSendsSlot) {
            return RgbState.of(LpColor.CYAN_HI);
        }
        return RgbState.of(LpColor.CYAN_LO);
    }

    private void assignNoteRepeat(final List<LabeledButton> noteRepeateValueButtons) {
        for (int i = 0; i < noteRepeateValueButtons.size(); i++) {
            final int index = i;
            final LabeledButton button = noteRepeateValueButtons.get(i);
            button.bind(this, () -> triggerNrRateButton(index), () -> getTrackState(index));
        }
    }

    private void triggerNrRateButton(final int index) {
        selectedArpIndex = index;
        currentArpRate = ARP_RATES[index];
        arp.rate().set(currentArpRate);
    }

    private void assignGridResolution(final List<LabeledButton> sceneButtons) {
        for (int i = 0; i < 8; i++) {
            final int index = i;
            final LabeledButton button = sceneButtons.get(i);
            button.bind(this, () -> {
                selectedGridIndex = index;
                positionHandler.setGridResolution(GRID_RATES[index]);
                multilineSeqLayer.setGridResolution(GRID_RATES[index]);
                notifyGridResolution(GRID_RATES[index]);
            }, () -> getGridState(index));
        }
    }

    private void handleVelMode(final boolean pressed) {
        if (pressed) {
            velLayer.activate();
        } else {
            velLayer.deactivate();
        }
    }

    private void handleSoloMode(final boolean pressed) {
        if (pressed) {
            soloLayer.activate();
            disableNotePlaying();
        } else {
            soloLayer.deactivate();
            if (notePlayingEnabled()) {
                applyScale();
            }
        }
    }

    private void handleSendsMode(final boolean pressed) {
        if (pressed) {
            sendsLayer.activate();
            disableNotePlaying();
        } else {
            sendsLayer.deactivate();
            if (notePlayingEnabled()) {
                applyScale();
            }
        }
    }

    private void handleMuteMode(final boolean pressed) {
        if (pressed) {
            muteLayer.activate();
            disableNotePlaying();
        } else {
            muteLayer.deactivate();
            if (notePlayingEnabled()) {
                applyScale();
            }
        }
    }

    private void handleClip(final int index, final ClipLauncherSlot slot, final boolean pressed) {
        if (!pressed) {
            if (altLaunchHeld.get()) {
                slot.launchReleaseAlt();
            } else if (states.isOnlyShiftActive()) {
                slot.select();
            } else if (states.isNoModifiersActive()) {
                slot.launchRelease();
            }
            return;
        }

        final boolean hasContent = slot.hasContent().get();
        if (hasContent) {
            if (altLaunchHeld.get()) {
                slot.launchAlt();
            } else if (states.isOnlyShiftActive()) {
                slot.select();
                states.notifyShiftFunctionInvoked();
            } else if (states.getClearButtonPressed().get()) {
                final int previous = selectedSlotIndex;
                slot.select();
                cursorClip.clearSteps();
                if (previous != -1) {
                    slotBank.getItemAt(previous).select();
                }
            } else if (states.getDuplicateButtonPressed().get()) {
                if (selectedSlotIndex != -1 && selectedSlotIndex != index) {
                    slot.replaceInsertionPoint().copySlotsOrScenes(slotBank.getItemAt(selectedSlotIndex));
                }
            } else if (stopButtonHeld.get()) {
                if (slot.isPlaying().get()) {
                    cursorTrack.stop();
                }
            } else {
                slot.select();
                slot.launch();
            }
        } else {
            if (states.getDuplicateButtonPressed().get()) {
                if (selectedSlotIndex != -1 && selectedSlotIndex != index) {
                    slot.replaceInsertionPoint().copySlotsOrScenes(slotBank.getItemAt(selectedSlotIndex));
                }
            } else {
                slot.createEmptyClip(4);
            }
        }

    }

    private RgbState getClipSate(final int index, final ClipLauncherSlot slot) {
        if (slot.hasContent().get()) {
            if (slot.isSelected().get()) {
                final int hiIndex = slotColors[index].getHiIndex();

                if (slot.isPlaying().get()) {
                    return RgbState.of(hiIndex, LightState.PULSING);
                }
                if (slot.isPlaybackQueued().get()) {
                    return RgbState.of(hiIndex, LightState.FLASHING);
                }
                return RgbState.of(hiIndex);
            } else {
                if (slot.isPlaying().get()) {
                    return RgbState.of(slotColors[index].getIndex(), LightState.PULSING);
                }
                if (slot.isPlaybackQueued().get()) {
                    return RgbState.of(slotColors[index].getIndex(), LightState.FLASHING);
                }
                return RgbState.of(slotColors[index].getLowIndex());
            }
        }
        return RgbState.of(0);
    }

    private RgbState getTrackState(final int index) {
        if (selectedArpIndex == index) {
            final LpColor color = ARP_BUTTON_COLORS[index];
            if (arp.isEnabled().get()) {
                return RgbState.of(color.getHiIndex(), LightState.FLASHING);
            }
            return RgbState.of(color.getLowIndex());
        }
        return RgbState.of(0);
    }

    private RgbState getGridState(final int index) {
        if (selectedGridIndex == index) {
            final LpColor color = GRID_BUTTON_COLORS[index];
            return RgbState.of(color.getHiIndex());
        }
        return RgbState.of(0);
    }

    private void handleNoteStep(final NoteStep noteStep) {
        assignments[noteStep.x()] = noteStep;
        if (probValues[noteStep.x()] != null && noteStep.state() == State.NoteOn) {
            noteStep.setChance(probValues[noteStep.x()]);
            probValues[noteStep.x()] = null;
        }
    }

    private void handlePlayingStep(final int playingStep) {
        if (playingStep == -1) {
            this.playingStep = -1;
        }
        this.playingStep = playingStep - positionHandler.getStepOffset();
    }

    public boolean isFixedLengthHeld() {
        return fixedLengthHeld.get();
    }

    public boolean isRandomModeActive() {
        return randomModeActive.get();
    }

    private void handleSeqSelection(final int index, final boolean pressed) {
        if (!pressed) {
            return;
        }
        final NoteStep note = assignments[index];
        if (fixedLengthHeld.get()) {
            stepActionFixedLength(index);
        } else if (randomModeActive.get()) {
            stepActionRandomMode(index, note);
        } else {
            if (note == null || note.state() == State.Empty) {
                cursorClip.setStep(index, 0, velTable[selectedRefVel],
                        positionHandler.getGridResolution() * gatePercent);
            } else {
                cursorClip.toggleStep(index, 0, velTable[selectedRefVel]);
            }
        }
    }

    public RndConfig getCurrentRndValue() {
        return RND_VALUES[selectedRndIndex];
    }

    private void stepActionRandomMode(final int stepIndex, final NoteStep note) {
        final double setProb = RND_VALUES[selectedRndIndex].prob;
        if (note != null && note.state() == State.NoteOn) {
            final double prob = note.chance();
            if (prob == setProb) {
                note.setChance(1);
            } else {
                note.setChance(RND_VALUES[selectedRndIndex].prob);
            }
        } else if (note == null || note.state() == State.Empty) {
            cursorClip.setStep(stepIndex, 0, velTable[selectedRefVel],
                    positionHandler.getGridResolution() * gatePercent);
            probValues[stepIndex] = RND_VALUES[selectedRndIndex].prob;
        }
    }

    void stepActionFixedLength(final int index) {
        final double newLen = positionHandler.lengthWithLastStep(index);
        adjustMode(newLen);
        cursorClip.getLoopLength().set(newLen);
    }

    private void handlePadSelection(final PadContainer pad) {
        if (states.getClearButtonPressed().get()) {
            cursorClip.scrollToKey(drumScrollOffset + pad.index);
            if (randomModeActive.get()) {
                resetRandomization();
            } else {
                cursorClip.clearStepsAtY(0, 0);
                pad.pad.selectInEditor();
            }
        } else if (states.getDuplicateButtonPressed().get()) {
            if (pad.index != selectedPadIndex) {
                final NoteStep[] copy = new NoteStep[32];
                System.arraycopy(assignments, 0, copy, 0, copy.length);
                cursorClip.scrollToKey(drumScrollOffset + pad.index);
                cursorClip.clearStepsAtY(0, 0);
                for (int i = 0; i < copy.length; i++) {
                    final NoteStep note = copy[i];
                    if (note != null && note.state() == NoteStep.State.NoteOn) {
                        cursorClip.setStep(i, 0, (int) (note.velocity() * 127), note.duration());
                    }
                }
                cursorClip.scrollToKey(drumScrollOffset + selectedPadIndex);
            }
        } else {
            pad.pad.selectInEditor();
            multilineSeqLayer.setSelectPadIndex(pad.index);
        }
    }

    private void resetRandomization() {
        for (final NoteStep step : assignments) {
            if (step != null && step.state() == State.NoteOn) {
                step.setChance(1.0);
            }
        }
    }

    private RgbState stepState(final int index) {
        final int steps = positionHandler.getAvailableSteps();
        if (index < steps) {
            if (assignments[index] == null || assignments[index].state() != State.NoteOn) {
                if (index == playingStep) {
                    return RgbState.of(2);
                }
                return RgbState.of(1);
            }
            if (randomModeActive.get()) {
                final double chance = assignments[index].chance();
                if (chance == 1) {
                    if (index == playingStep) {
                        return RgbState.of(currentPadColor.getHiIndex());
                    }
                    return RgbState.of(currentPadColor.getIndex());
                } else {
                    final LpColor chanceColor = toColor(chance);
                    if (index == playingStep) {
                        return RgbState.of(chanceColor.getHiIndex());
                    }
                    return RgbState.of(chanceColor.getIndex());
                }
            } else {
                if (index == playingStep) {
                    return RgbState.of(currentPadColor.getHiIndex());
                }
                return RgbState.of(currentPadColor.getIndex());
            }
        }
        return RgbState.of(0);
    }

    LpColor toColor(final double chance) {
        if (chance == 0) {
            return LpColor.GREY_MD;
        }
        for (final RndConfig rndValue : RND_VALUES) {
            if (chance <= rndValue.prob) {
                return rndValue.refColor;
            }
        }
        return LpColor.RED;
    }

    private void handleVelocitySelection(final int index, final boolean pressed) {
        if (!pressed) {
            return;
        }
        selectedRefVel = index;
        applyRefVelocity(velTable[selectedRefVel]);
    }

    private void handleRndSelection(final int index, final boolean pressed) {
        if (!pressed) {
            return;
        }
        selectedRndIndex = index;
    }

    private RgbState velocityValues(final int index) {
        if (index == selectedRefVel) {
            return RgbState.of(37);
        }
        return RgbState.of(39);
    }

    private RgbState rndValues(final int index) {
        if (index < RND_VALUES.length) {
            if (index == selectedRndIndex) {
                return RgbState.of(RND_VALUES[index].refColor.getHiIndex());
            }
            return RgbState.of(RND_VALUES[index].refColor.getLowIndex());
        }
        return RgbState.of(0);
    }

    private void handlePositionSelection(final int index, final boolean pressed) {
        final int pages = positionHandler.getPages();
        final int page = positionHandler.getCurrentPage();
        if (index != page && index < pages) {
            positionHandler.setPage(index);
        }
    }

    private RgbState lengthState(final int index) {
        final int pages = positionHandler.getPages();
        final int page = positionHandler.getCurrentPage();
        if (index < pages) {
            if (index == page) {
                return RgbState.of(LpColor.WHITE);
            }
            return RgbState.of(LpColor.GREY_LO);
        }
        return RgbState.of(0);
    }

    private void handleDuplication(final Application application, final boolean duplPressed) {
        if (!isActive() || !duplPressed) {
            return;
        }
        if (states.getShiftModeActive().get() && cursorClip.getLoopLength().get() * 2 <= 8.0) {
            cursorClip.duplicateContent();
            states.notifyShiftFunctionInvoked();
        }
    }

    private void handleClear(final boolean pressed) {
        if (!isActive()) {
            return;
        }
        if (pressed) {
            if (notePlayingEnabled()) {
                disableNotePlaying();
            }
            if (states.getShiftModeActive().get()) {
                cursorClip.clearSteps();
                states.notifyShiftFunctionInvoked();
            } else if (sendHeld.get()) {
                for (final PadContainer pad : pads) {
                    pad.resetSend();
                }
            }
        } else {
            if (notePlayingEnabled()) {
                applyScale();
            }
        }
    }

    private void setupPlaying(final ViewCursorControl control) {
        final DrumPadBank drumPadBank = control.getDrumPadBank();
        final CursorTrack cursorTrack = control.getCursorTrack();
        for (int i = 0; i < notesToDrumTable.length; i++) {
            notesToDrumTable[i] = -1;
            notesToPadsTable[i] = -1;
        }
        applyRefVelocity(velTable[selectedRefVel]);
        noteInput.setKeyTranslationTable(notesToDrumTable);
        noteInput.setVelocityTranslationTable(velocityTable);
        drumPadBank.scrollPosition().addValueObserver(offset -> {
            drumScrollOffset = offset;
            focusOnSelectedPad();
            applyScale();
            notifyNoteOffset();
        });
        for (int i = 0; i < 16; i++) {
            playing[i] = new BooleanValueObject();
            playing[i].set(false);
        }
        cursorTrack.playingNotes().addValueObserver(this::handleNotes);
    }

    private void applyRefVelocity(final int value) {
        Arrays.fill(velocityTable, value);
        if (noteInput != null) {
            noteInput.setVelocityTranslationTable(velocityTable);
        }
    }

    public void retrigger() {
        cursorClip.launch();
    }

    public void focusOnSelectedPad() {
        final int padIndex = selectedPad != null ? selectedPad.index : 0;
        cursorClip.scrollToKey(drumScrollOffset + padIndex);
    }

    public RgbState getState(final int index, final BooleanSupplier playing, final PadContainer pad) {
        if (playing.getAsBoolean()) {
            return RgbState.of(LpColor.BLUE_HI);
        }
        return RgbState.of(LpColor.BLUE_LO);
    }

    @Override
    protected void onActivate() {
        currentLayer = mainLayer;
        mainLayer.activate();
        applyScale();
    }

    @Override
    protected void onDeactivate() {
        currentLayer.deactivate();
        shiftLayer.deactivate();
        disableNotePlaying();
    }

    public void notifyMidiEvent(final int note, final int value) {
        if (!isActive()) {
            return;
        }
        final int row = note / 10;
        final int col = note % 10;
        if (value > 0) {
            holdNotes.add(notesToDrumTable[note]);
        } else {
            holdNotes.remove(notesToDrumTable[note]);
        }

        if (row > 0 && row < 5 && col > 0 && col < 5) {
            final int index = (row - 1) * 4 + col - 1;
            pads.get(index).select();
        }
    }

    private void handleNotes(final PlayingNote[] notes) {
        if (!isActive()) {
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

    private boolean notePlayingEnabled() {
        return states.getNoteRepeatActive().get() || notePlayingActive.get();
    }

    private void applyScale() {
        if (!isActive()) {
            return;
        }
        for (int i = 0; i < 128; i++) {
            notesToPadsTable[i] = -1;
        }
        for (int i = 0; i < 16; i++) {
            final int padnote = PAD_NOTES[i];
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

    private void disableNotePlaying() {
        if (!isActive()) {
            return;
        }
        holdNotes.clear();
        for (int i = 0; i < 128; i++) {
            notesToDrumTable[i] = -1;
        }
        noteInput.setKeyTranslationTable(notesToDrumTable);
    }

    private void handleNoteRepeatChanged(final boolean nrActive) {
        if (nrActive) {
            if (multilineSeqLayer.isActive()) {
                multilineSeqLayer.setIsActive(false);
            }
            arp.isEnabled().set(true);
            arp.mode().set("all"); // that's the note repeat way
            arp.octaves().set(0);
            arp.humanize().set(0);
            arp.isFreeRunning().set(false);
            arp.rate().set(currentArpRate);
            arp.usePressureToVelocity().set(false);
            applyScale();
        } else {
            arp.isEnabled().set(false);
            if (!notePlayingEnabled()) {
                disableNotePlaying();
            }
        }
    }

}
