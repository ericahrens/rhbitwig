package com.novation.launchpadProMk3;

import com.bitwig.extension.api.opensoundcontrol.OscConnection;
import com.bitwig.extension.api.util.midi.ShortMidiMessage;
import com.bitwig.extension.callback.ShortMidiMessageReceivedCallback;
import com.bitwig.extension.controller.ControllerExtension;
import com.bitwig.extension.controller.ControllerExtensionDefinition;
import com.bitwig.extension.controller.api.*;
import com.bitwig.extensions.framework.Layer;
import com.bitwig.extensions.framework.Layers;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;

public class LaunchpadProMk3ControllerExtension extends ControllerExtension {

    private static final String HEADER = "F0 00 20 29 02 0E ";

    private static final String DAW_MODE = "0E 01";
    private static final String STAND_ALONE_MODE = "0E 00";

    private HardwareSurface surface;
    private MidiIn midiIn;
    private MidiOut midiOut;
    private Layers layers;
    private LpLayer mainLayer;
    private LpLayer shiftLayer;
    private OscConnection gridOSCconnection;

    private ViewCursorControl viewControl;
    private NoteInput noteInput;
    private DrumSequenceMode drumseqenceMode;
    private Transport transport;
    private Application application;

    private final LpStateValues states = new LpStateValues();
    private HardwareElements hwElements;

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

        initOscConnection(host);

        noteInput = midiIn.createNoteInput("MIDI", "80????", "90????", "A0????", "D0????");
        noteInput.setShouldConsumeEvents(false);

        hwElements = new HardwareElements(surface, gridOSCconnection, midiIn, midiOut);
        viewControl = new ViewCursorControl(host);
        mainLayer = new LpLayer(layers, "MainLayer");
        shiftLayer = new LpLayer(layers, "GlobalShiftLayer");
        setUpMidiSysExCommands();
        host.showPopupNotification(" Intialize Launchpad");
        initModifierButtons();
        initTransportSection();
        initDrumSequenceLayer();
        mainLayer.activate();
        drumseqenceMode.activate();
        sendSysExCommand(DAW_MODE);
    }

    private void initOscConnection(ControllerHost host) {
        Preferences prefs = host.getPreferences();
        SettableRangedValue OscPortSetting = prefs.getNumberSetting("Port", "OSC", 8000, 100000, 1, "", 12345);
        SettableStringValue OscAddressSetting = prefs.getStringSetting("Address", "OSC", 15, "10.0.0.255");
        Signal SignalTest = prefs.getSignalSetting("Send Message", "OSC", "Test");
        SettableBooleanValue SendOSC = prefs.getBooleanSetting("Send OSC", "OSC", false);

        if (SendOSC.get() == true) {
            gridOSCconnection = host.getOscModule()
                    .connectToUdpServer(OscAddressSetting.get(), (int) OscPortSetting.getRaw(),
                            host.getOscModule().createAddressSpace());

            Object testArg = "sending!";
            // connection.startBundle();
            try {
                gridOSCconnection.sendMessage("/RHBitwig", testArg);
            } catch (IOException e) {
                // throw new RuntimeException(e);
                host.println("No Connection!!");
            }
        } else {
            gridOSCconnection = null;
            return;
        }

        SignalTest.addSignalObserver(() -> {
            Object o = "1";
            try {
                gridOSCconnection.sendMessage("Bitwig Test Message", o);
            } catch (IOException e) {
                // throw new RuntimeException(e);
                host.println("No Connection!!");
            }
        });
    }

    private void initDrumSequenceLayer() {
        drumseqenceMode = new DrumSequenceMode(layers, this);
        // implement a only pressed binding to avoid stupid if stuff..
    }

    private void initTransportSection() {
        transport.isPlaying().markInterested();
        transport.tempo().markInterested();
        transport.playPosition().markInterested();
        hwElements.getButton(LabelCcAssignments.PLAY)
                .bind(mainLayer, this::togglePlay,
                        () -> transport.isPlaying().get() ? RgbState.of(LpColor.GREEN_HI) : RgbState.of(
                                LpColor.GREEN_LO));

        hwElements.getButton(LabelCcAssignments.SESSION)
                .bindToggle(mainLayer, transport.isClipLauncherAutomationWriteEnabled(), LpColor.RED_HI,
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

    private void initModifierButtons() {
        hwElements.getButton(LabelCcAssignments.SEQUENCER).bind(mainLayer, () -> {
        }, () -> RgbState.of(LpColor.BLUE_HI));

        hwElements.getButton(LabelCcAssignments.SHIFT)
                .bindPressed(mainLayer, states::handleShiftPressed,
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
        hwElements.getButton(LabelCcAssignments.REC)
                .bindToggle(mainLayer, transport.isClipLauncherOverdubEnabled(), LpColor.RED, LpColor.BLACK);

        hwElements.getButton(LabelCcAssignments.RECORD_ARM_UNDO)
                .bindToggle(mainLayer, states.getNoteRepeatActive(),
                        RgbState.of(LpColor.RED_HI.getIndex(), LightState.PULSING), RgbState.of(LpColor.BLACK));

        hwElements.getButton(LabelCcAssignments.LEFT).bind(mainLayer, () -> application.undo(), LpColor.BLUE);

        hwElements.getButton(LabelCcAssignments.CLEAR)
                .bindPressed(mainLayer, states.getClearButtonPressed(), LpColor.WHITE);

        hwElements.getButton(LabelCcAssignments.DUPLICATE)
                .bindPressed(mainLayer, states.getDuplicateButtonPressed(), LpColor.WHITE);

        hwElements.getButton(LabelCcAssignments.MUTE_REDO)
                .bindPressed(mainLayer, states.getMuteButtonPressed(), LpColor.ORANGE);

        hwElements.getButton(LabelCcAssignments.RIGHT).bind(mainLayer, () -> application.redo(), LpColor.BLUE);

        hwElements.getButton(LabelCcAssignments.SOLO_CLICK)
                .bindPressed(mainLayer, states.getSoloButtonPressed(), LpColor.YELLOW);
        // soloButton.bindToggle(shiftLayer, transport.isMetronomeEnabled(),
        // LpColor.SKY_HI, LpColor.SKY_LO);

        hwElements.getButton(LabelCcAssignments.VOLUME)
                .bindPressed(mainLayer, states.getVolumeButtonPressed(), LpColor.CYAN);
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

    public HardwareElements getHwElements() {
        return hwElements;
    }


    public LpStateValues getStates() {
        return states;
    }

    public Application getApplication() {
        return application;
    }

}
