package com.novation.launchpadProMk3;

import com.bitwig.extension.api.util.midi.ShortMidiMessage;
import com.bitwig.extension.callback.ShortMidiMessageReceivedCallback;
import com.bitwig.extension.controller.ControllerExtension;
import com.bitwig.extension.controller.ControllerExtensionDefinition;
import com.bitwig.extension.controller.api.*;
import com.bitwig.extensions.framework.Layer;
import com.bitwig.extensions.framework.Layers;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;

public class LaunchpadProMk3ControllerExtension extends ControllerExtension {

    private static final String HEADER = "F0 00 20 29 02 0E ";
    // private static final String INQ = "F0 7E 7F 06 01 F7";
    private static final String DAW_MODE = "0E 01";
    private static final String STAND_ALONE_MODE = "0E 00";

    private HardwareSurface surface;
    private MidiIn midiIn;
    private MidiOut midiOut;
    private Layers layers;
    private LpLayer mainLayer;
    private LpLayer shiftLayer;
    // Main Grid Buttons counting from top to bottom
    private final GridButton[][] gridButtons = new GridButton[8][8];
    private final List<LabeledButton> sceneLaunchButtons = new ArrayList<>();
    private final List<LabeledButton> trackSelectButtons = new ArrayList<>();
    private LabeledButton fixedLengthButton;
    private LabeledButton deviceButton;
    private LabeledButton quantizeButton;
    private LabeledButton upButton;
    private LabeledButton downButton;
    private LabeledButton leftButton;
    private LabeledButton rightButton;
    private LabeledButton noteButton;

    private ViewCursorControl viewControl;
    private NoteInput noteInput;
    private DrumSequenceMode drumseqenceMode;
    private Transport transport;
    private Application application;

    private final LpStateValues states = new LpStateValues();
    private LabeledButton sendsButton;
    private LabeledButton stopButton;
    private LabeledButton panButton;
    private LabeledButton customButton;

    protected LaunchpadProMk3ControllerExtension(final ControllerExtensionDefinition definition,
                                                 final ControllerHost host) {
        super(definition, host);
    }

    @Override
    public void init() {
        final ControllerHost host = getHost();
        layers = new Layers(this);
        surface = host.createHardwareSurface();
        transport = host.createTransport();
        application = host.createApplication();
        midiIn = host.getMidiInPort(0);
        midiIn.setMidiCallback((ShortMidiMessageReceivedCallback) this::onMidi0);
        midiOut = host.getMidiOutPort(0);
        
        noteInput = midiIn.createNoteInput("MIDI", "80????", "90????", "A0????", "D0????");
        noteInput.setShouldConsumeEvents(false);

        viewControl = new ViewCursorControl(host);
        mainLayer = new LpLayer(layers, "MainLayer");
        shiftLayer = new LpLayer(layers, "GlobalShiftLayer");
        setUpMidiSysExCommands();
        host.showPopupNotification(" Intialize Launchpad");
        initGridButtons();
        initModifierButtons();
        initModeButtton();
        initTransportSection();
        initDrumSequenceLayer();
        mainLayer.activate();
        drumseqenceMode.activate();
        sendSysExCommand(DAW_MODE);
    }

    private void initDrumSequenceLayer() {
        drumseqenceMode = new DrumSequenceMode(layers, this);
        // implement a only pressed binding to avoid stupid if stuff..
    }

    private void initTransportSection() {
//		final LabeledButton overdubButton = new LabeledButton(surface, midiIn, midiOut, LabelCcAssignments.REC);
//		overdubButton.bindToggle(mainLayer, transport.isClipLauncherOverdubEnabled(), LpColor.RED, LpColor.BLACK);
        transport.isPlaying().markInterested();
        transport.tempo().markInterested();
        transport.playPosition().markInterested();
        final LabeledButton playButton = new LabeledButton(surface, midiIn, midiOut, LabelCcAssignments.PLAY);
        playButton.bind(mainLayer, this::togglePlay,
                () -> transport.isPlaying().get() ? RgbState.of(LpColor.GREEN_HI) : RgbState.of(LpColor.GREEN_LO));

        final LabeledButton clipAutoButton = new LabeledButton(surface, midiIn, midiOut, LabelCcAssignments.SESSION);
        clipAutoButton.bindToggle(mainLayer, transport.isClipLauncherAutomationWriteEnabled(), LpColor.RED_HI,
                LpColor.RED_LO);
    }

    private void togglePlay() {
        if (transport.isPlaying().get()) {
            transport.isPlaying().set(false);
        } else {
            drumseqenceMode.retrigger();
            // transport.isPlaying().set(true);
            transport.restart();
        }
    }

    private void initModeButtton() {
        final LabeledButton sequenceModeButton = new LabeledButton(surface, midiIn, midiOut,
                LabelCcAssignments.SEQUENCER);
        sequenceModeButton.bind(mainLayer, () -> {
        }, () -> RgbState.of(LpColor.BLUE_HI));
    }

    private void initModifierButtons() {
        leftButton = new LabeledButton(surface, midiIn, midiOut, LabelCcAssignments.LEFT);
        rightButton = new LabeledButton(surface, midiIn, midiOut, LabelCcAssignments.RIGHT);
        upButton = new LabeledButton(surface, midiIn, midiOut, LabelCcAssignments.UP);
        downButton = new LabeledButton(surface, midiIn, midiOut, LabelCcAssignments.DOWN);
        noteButton = new LabeledButton(surface, midiIn, midiOut, LabelCcAssignments.NOTE);
        sendsButton = new LabeledButton(surface, midiIn, midiOut, LabelCcAssignments.SENDS_TAP);
        stopButton = new LabeledButton(surface, midiIn, midiOut, LabelCcAssignments.STOP_CLIP_SWING);
        quantizeButton = new LabeledButton(surface, midiIn, midiOut, LabelCcAssignments.QUANTIZE);
        customButton = new LabeledButton(surface, midiIn, midiOut, LabelCcAssignments.CUSTOM);

        final LabeledButton shiftButton = new LabeledButton(surface, midiIn, midiOut, LabelCcAssignments.SHIFT);
        shiftButton.bindPressed(mainLayer, states::handleShiftPressed,
                () -> states.getShiftModeActive().get() ? RgbState.of(LpColor.OCEAN_HI) : RgbState.of(
                        LpColor.OCEAN_LO));

        states.getShiftModeActive().addValueObserver(shiftMode -> {
            if (shiftMode) {
                shiftLayer.activate();
            } else {
                shiftLayer.deactivate();
            }
        });

        final Arpeggiator arp = noteInput.arpeggiator();
        arp.isEnabled().markInterested();
        arp.usePressureToVelocity().markInterested();
        arp.octaves().markInterested();
        arp.rate().markInterested();
        final LabeledButton armButton = new LabeledButton(surface, midiIn, midiOut, LabelCcAssignments.REC);
        armButton.bindToggle(mainLayer, transport.isClipLauncherOverdubEnabled(), LpColor.RED, LpColor.BLACK);

        final LabeledButton recArmUndoButton = new LabeledButton(surface, midiIn, midiOut,
                LabelCcAssignments.RECORD_ARM_UNDO);
        recArmUndoButton.bindToggle(mainLayer, states.getNoteRepeatActive(),
                RgbState.of(LpColor.RED_HI.getIndex(), LightState.PULSING), RgbState.of(LpColor.BLACK));

        leftButton.bind(mainLayer, () -> application.undo(), LpColor.BLUE);

//		recArmUndoButton.bindPressed(shiftLayer, pressed -> {
//			if (pressed) {
//				application.undo();
//				states.notifyShiftFunctionInvoked();
//			}
//		}, LpColor.GREEN);

        final LabeledButton clearButton = new LabeledButton(surface, midiIn, midiOut, LabelCcAssignments.CLEAR);
        clearButton.bindPressed(mainLayer, states.getClearButtonPressed(), LpColor.WHITE);

        final LabeledButton duplicateButton = new LabeledButton(surface, midiIn, midiOut, LabelCcAssignments.DUPLICATE);
        duplicateButton.bindPressed(mainLayer, states.getDuplicateButtonPressed(), LpColor.WHITE);

        final LabeledButton muteButton = new LabeledButton(surface, midiIn, midiOut, LabelCcAssignments.MUTE_REDO);
        muteButton.bindPressed(mainLayer, states.getMuteButtonPressed(), LpColor.ORANGE);
//		muteButton.bindPressed(shiftLayer, pressed -> {
//			if (pressed) {
//				application.redo();
//				states.notifyShiftFunctionInvoked();
//			}
//		}, LpColor.LIME);

        rightButton.bind(mainLayer, () -> application.redo(), LpColor.BLUE);

        final LabeledButton soloButton = new LabeledButton(surface, midiIn, midiOut, LabelCcAssignments.SOLO_CLICK);
        soloButton.bindPressed(mainLayer, states.getSoloButtonPressed(), LpColor.YELLOW);
        // soloButton.bindToggle(shiftLayer, transport.isMetronomeEnabled(),
        // LpColor.SKY_HI, LpColor.SKY_LO);
        fixedLengthButton = new LabeledButton(surface, midiIn, midiOut, LabelCcAssignments.FIXED_LENGTH);
        deviceButton = new LabeledButton(surface, midiIn, midiOut, LabelCcAssignments.DEVICE_TEMPO);

        final LabeledButton volumeButton = new LabeledButton(surface, midiIn, midiOut, LabelCcAssignments.VOLUME);
        volumeButton.bindPressed(mainLayer, states.getVolumeButtonPressed(), LpColor.CYAN);
        panButton = new LabeledButton(surface, midiIn, midiOut, LabelCcAssignments.PAN);
    }

    void bindRecQuantize(final Layer layer, final LabeledButton button) {
        final SettableEnumValue recGrid = application.recordQuantizationGrid();
        recGrid.markInterested();
        button.bind(layer, () -> {
            if (recGrid.get().equals("OFF")) {
                recGrid.set("1/16");
            } else {
                recGrid.set("OFF");
            }
            states.notifyShiftFunctionInvoked();
        }, () -> {
            if (recGrid.get().equals("OFF")) {
                return RgbState.of(LpColor.BLUE_LO);
            }
            return RgbState.of(LpColor.BLUE_HI);
        });
    }

    private void initGridButtons() {
        for (int row = 0; row < 8; row++) {
            for (int col = 0; col < 8; col++) {
                final GridButton b = new GridButton(surface, midiIn, midiOut, row, col);
                gridButtons[row][col] = b;
                b.bindPressed(mainLayer, p -> {
                }, LpColor.BLUE_HI);
                b.bindPressed(shiftLayer, p -> {
                }, LpColor.ORANGE);
            }
        }
        for (int i = 0; i < 8; i++) {
            final LabeledButton sceneButton = new LabeledButton("SCENE_LAUNCH_" + (i + 1), surface, midiIn, midiOut,
                    LabelCcAssignments.R8_PRINT_TO_CLIP.getCcValue() + (7 - i) * 10);
            sceneLaunchButtons.add(sceneButton);

            final LabeledButton trackButton = new LabeledButton("TRACK_" + (i + 1), surface, midiIn, midiOut,
                    LabelCcAssignments.TRACK_SEL_1.getCcValue() + i);
            trackSelectButtons.add(trackButton);
        }
    }

    public int beatToMs(final double beats) {
        final double tempo = transport.tempo().getRaw();
        final double hz = tempo / 60.0;
        return (int) (beats / hz * 1000);
    }

    public double transportPos() {
        return transport.playPosition().get();
    }

    private void setUpMidiSysExCommands() {
        midiIn.setSysexCallback(data -> {
            // RemoteConsole.out.println(" MIDI SYS EX {}", data);
        });
    }

    private void onMidi0(final ShortMidiMessage msg) {
        if (msg.getChannel() == 0 && msg.getStatusByte() == 144) {
            drumseqenceMode.notifyMidiEvent(msg.getData1(), msg.getData2());
        }
    }

    public ViewCursorControl getViewControl() {
        return viewControl;
    }

    public NoteInput getNoteInput() {
        return noteInput;
    }

    private void sendSysExCommand(final String command) {
        midiOut.sendSysex(HEADER + command + " F7");
    }

    private void shutDownController(final CompletableFuture<Boolean> shutdown) {
        sendSysExCommand(STAND_ALONE_MODE);
        try {
            Thread.sleep(300);
        } catch (final InterruptedException e) {
            e.printStackTrace();
        }
        shutdown.complete(true);
    }

    @Override
    public void exit() {
        drumseqenceMode.deactivate();
        final CompletableFuture<Boolean> shutdown = new CompletableFuture<>();
        Executors.newSingleThreadExecutor().execute(() -> shutDownController(shutdown));
        try {
            shutdown.get();
        } catch (final InterruptedException | ExecutionException e) {
            e.printStackTrace();
        }
        getHost().showPopupNotification("Novation Launchpad Exited");
    }

    @Override
    public void flush() {
        surface.updateHardware();
    }

    public GridButton getGetGridButton(final int row, final int col) {
        return gridButtons[row][col];
    }

    public LpStateValues getStates() {
        return states;
    }

    public Application getApplication() {
        return application;
    }

    public LabeledButton getFixedLengthButton() {
        return fixedLengthButton;
    }

    public LabeledButton getDeviceButton() {
        return deviceButton;
    }

    public LabeledButton getQuantizeButton() {
        return quantizeButton;
    }

    public LabeledButton getUpButton() {
        return upButton;
    }

    public LabeledButton getDownButton() {
        return downButton;
    }

    public LabeledButton getLeftButton() {
        return leftButton;
    }

    public LabeledButton getRightButton() {
        return rightButton;
    }

    public LabeledButton getNoteButton() {
        return noteButton;
    }

    public List<LabeledButton> getSceneLaunchButtons() {
        return sceneLaunchButtons;
    }

    public List<LabeledButton> getTrackSelectButtons() {
        return trackSelectButtons;
    }

    public LabeledButton getSendsButton() {
        return sendsButton;
    }

    public LabeledButton getStopButton() {
        return stopButton;
    }

    public LabeledButton getPanButton() {
        return panButton;
    }

    public LabeledButton getCustomButton() {
        return customButton;
    }
}
