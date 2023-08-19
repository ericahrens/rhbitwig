package com.allenheath.k2.set1;

import com.bitwig.extension.api.opensoundcontrol.OscAddressSpace;
import com.bitwig.extension.api.opensoundcontrol.OscConnection;
import com.bitwig.extension.api.opensoundcontrol.OscMessage;
import com.bitwig.extension.api.opensoundcontrol.OscModule;
import com.bitwig.extension.controller.ControllerExtension;
import com.bitwig.extension.controller.ControllerExtensionDefinition;
import com.bitwig.extension.controller.api.*;
import com.bitwig.extensions.framework.Layer;
import com.bitwig.extensions.framework.Layers;
import com.rhcommons.SpecialVst3Devices;
import com.rhcommons.SpecialVstDevices;
import com.sun.net.httpserver.HttpServer;
import com.torso.t1.DeviceTrack;
import com.torso.t1.T1DataPack;

import java.io.IOException;
import java.lang.reflect.Array;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Executors;

public class AllenHeathK2ControllerExtension extends ControllerExtension {

    private static final String[] DEFAULT_PAD_ASSIGNMENTS = {"1,2", "4", "3", "5,6", "7", "9", "10", "11"};
    public static final String TRAKTOR_CONTROL_LABEL = "Traktor Http Control";
    public static final String TRAKTOR_KONDUIT_CATEGORY = "Traktor Konduit OSC";
    private HardwareSurface surface;
    private MidiIn midiIn;
    private MidiOut midiOut;
    private Layers layers;
    private NoteInput noteInput;
    private ViewCursorControl viewControl;
    private Layer mainLayer;
    private final StateButton[] reverbSendButtons = new StateButton[8];
    private final StateButton[] delaySendButtons = new StateButton[8];
    private final List<PadContainer> drumPadsList = new ArrayList<>();
    private final PadGrouping padGrouping = new PadGrouping();
    private DirectParameterControl delayControl;
    private DirectParameterControl reverbControl;

    private static ControllerHost debugHost;
    private HwElements hwElements;
    private SequencerLayer sequencerLayer;
    private OscModule oscModule;
    private OscConnection oscConnection;
    
    private HttpServer server;
    private TraktorState traktorState = new TraktorState();
    
    public static void println(final String format, final Object... args) {
        if (debugHost != null) {
            debugHost.println(format.format(format, args));
        }
    }

    protected AllenHeathK2ControllerExtension(final ControllerExtensionDefinition definition,
                                              final ControllerHost host) {
        super(definition, host);
    }

    @Override
    public void init() {
        final ControllerHost host = getHost();
        debugHost = host;
        layers = new Layers(this);
        surface = host.createHardwareSurface();
        midiIn = host.getMidiInPort(0);
        midiOut = host.getMidiOutPort(0);
        noteInput = midiIn.createNoteInput("MIDI", "80????", "90????", "A0????", "D0????");
        noteInput.setShouldConsumeEvents(false);
        midiIn.setMidiCallback(this::onMidi);
        oscModule = host.getOscModule();
        initOsc();
    
        delayControl = new DirectParameterControl(SpecialVstDevices.LEXICON_PSP, SpecialParam.PSP_REPEAT_INF);
        reverbControl = new DirectParameterControl(SpecialVst3Devices.MEAGAVERB3, SpecialParam.MEGA_VERB_GATE);

        final List<DirectParameterControl> controlList = new ArrayList<>();
        controlList.add(delayControl);
        controlList.add(reverbControl);

        viewControl = new ViewCursorControl(host, controlList, 16);
        mainLayer = new Layer(layers, "MainLayer");
        hwElements = new HwElements(surface, host, midiIn, midiOut);
        initSendsButtons();
        initDeckCaptureButtons();
        initDocumentProperties();
        sequencerLayer = new SequencerLayer(layers, hwElements, viewControl, padGrouping);
        mainLayer.activate();
        initTestButtonsInDocumentState(host);
        initServers();
        host.showPopupNotification("Intialize Xone:K2 DJ Set");
    }
    
    private void initTestButtonsInDocumentState(final ControllerHost host) {
        if(oscConnection!=null) {
            final Signal signal = host.getDocumentState().getSignalSetting("Test OSC", "OSC", "TEST");
            signal.addSignalObserver(() -> {
                try {
                    oscConnection.sendMessage("/deck/A/metadata/key", true);
                }
                catch (IOException e) {
                    println(" Failed send %s", e.getMessage());
                }
            });
        }
        Signal fetchDeckA = host.getDocumentState().getSignalSetting("Apply Key Deck A","Test","Deck A");
        fetchDeckA.addSignalObserver(() -> {
            fetchKey(0);
        });
        Signal fetchDeckB = host.getDocumentState().getSignalSetting("Apply Key Deck B","Test","Deck B");
        fetchDeckB.addSignalObserver(() -> {
            fetchKey(1);
        });
    }
    
    private void initServers() {
        final Preferences preferences = getHost().getPreferences();
        final SettableBooleanValue active = preferences.getBooleanSetting("Active", TRAKTOR_CONTROL_LABEL, true);
        final SettableStringValue hostValue = preferences.getStringSetting("Host",
            TRAKTOR_CONTROL_LABEL, 15, "127.0.0.1");
        final SettableRangedValue portValue =
            preferences.getNumberSetting("Port", TRAKTOR_CONTROL_LABEL, 2000.0, 6000.0, 1, "", 3000);
        if(active.get()) {
            int port = (int)portValue.getRaw();
            try {
                server = HttpServer.create(new InetSocketAddress(hostValue.get(), port), 0);
                server.createContext("/", new HttpJsonHandler(traktorState));
                server.setExecutor(Executors.newSingleThreadExecutor());
                server.start();
                println(" Http Server on port %d",port);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
    
    private void initOsc() {
        final OscAddressSpace address = oscModule.createAddressSpace();
        final Preferences preferences = getHost().getPreferences();
        final SettableBooleanValue active = preferences.getBooleanSetting("Active", TRAKTOR_KONDUIT_CATEGORY, false);
        final SettableRangedValue inPortValue =
            preferences.getNumberSetting("Port Receive", TRAKTOR_KONDUIT_CATEGORY, 6000, 13000, 1, "", 12345);
        final SettableRangedValue outPortValue =
            preferences.getNumberSetting("Port Send", TRAKTOR_KONDUIT_CATEGORY, 6000, 10000, 1, "", 9800);
    
        if(active.get()) {
            int inPort = (int) inPortValue.getRaw();
            int outPort = (int) outPortValue.getRaw();
            address.registerDefaultMethod(this::handleMessage);
            oscModule.createUdpServer(inPort, address);
            oscConnection = oscModule.connectToUdpServer("127.0.0.1", outPort, null);
        }
    }
    
    private void initDeckCaptureButtons() {
        for (int i = 0; i < 4; i++) {
            final int index = i;
            HardwareButton button = hwElements.getCaptureKeysFromDeckButton(i);
            mainLayer.bindPressed(button, () -> fetchKey(index));
        }
    }

    private void fetchKey(int index) {
        if(oscConnection != null) {
            try {
                println(" SEND OSC > %s", "/deck/%c/metadata/key".formatted('A'+index));
                oscConnection.sendMessage("/deck/%c/metadata/key".formatted('A'+index), true);
            }
            catch (IOException e) {
                println("Failed to send message %s", e.getMessage());
            }
        }
        traktorState.getKey(index).ifPresent(key-> {
            println(" Setting Scale %s",key);
            viewControl.setScale(key);
        });
    }
    
    private void handleMessage(final OscConnection connection, final OscMessage message) {
        String path = message.getAddressPattern();
        println(" %s %s", message.getAddressPattern(),message.getArguments());
        if(path.matches("/deck/[A-B]/metadata/key") && message.getArguments().get(0) instanceof String) {
            String[] x = path.split("/");
            int index = 'A'-x[2].charAt(0);
            traktorState.setKey(index, message.getString(0));
        }
    }
    
    private void onMidi(int msg, int data1, int data2) {
//        println("MIDI> %02X %02X %02X", msg, data1, data2);
//        if (msg == 0x9D && data1 == 0x0C && data2 == 0x7F) {
//            for (int i = 0; i < 0x38; i++) {
//                midiOut.sendMidi(Midi.NOTE_ON + 13, i, 0);
//            }
//        }
    }

    private void initDocumentProperties() {
        final DocumentState documentState = getHost().getDocumentState();
        for (int i = 0; i < 8; i++) {
            final int index = i;
            final SettableStringValue padAssignment = documentState.getStringSetting("Column " + (i + 1),
                    "Pad Assignments", 10, DEFAULT_PAD_ASSIGNMENTS[i]);
            padAssignment.addValueObserver(value -> padGrouping.assign(index, value, drumPadsList));
        }
    }

    private void initSendsButtons() {
        for (int i = 0; i < 16; i++) {
            final PadContainer pad = new PadContainer(i, viewControl.getDrumPadBank().getItemAt(i));
            drumPadsList.add(pad);
        }


        for (int i = 0; i < 8; i++) {
            final int channel = 13 + i / 4;
            final int noteOffset = i % 4;
            final StateButton revButton = new StateButton("REV_" + i + "_BUTTON", 44 + noteOffset, channel, surface,
                    midiIn, midiOut);
            final StateButton delButton = new StateButton("DEL_" + i + "_BUTTON", 40 + noteOffset, channel, surface,
                    midiIn, midiOut);
            final PadAssignment assignment = padGrouping.getAssignment(i);
            delButton.bind(mainLayer, () -> assignment.toggleSendValue(0), () -> assignment.sendStatusColor(0));
            revButton.bind(mainLayer, () -> assignment.toggleSendValue(1), () -> assignment.sendStatusColor(1));
            reverbSendButtons[i] = revButton;
            delaySendButtons[i] = delButton;
        }

        final StateButton delRptButton = new StateButton("DLY_INF_BUTTON", 50, 13, surface, midiIn, midiOut);

        delRptButton.bind(mainLayer, () -> delayControl.toggle(SpecialParam.PSP_REPEAT_INF),
                () -> delayControl.getState(SpecialParam.PSP_REPEAT_INF));

        final StateButton revGateButton = new StateButton("RVB_GATE_BUTTON", 50, 14, surface, midiIn, midiOut);
        revGateButton.bind(mainLayer, () -> reverbControl.toggle(SpecialParam.MEGA_VERB_GATE),
                () -> reverbControl.getState(SpecialParam.MEGA_VERB_GATE));
    }

    @Override
    public void exit() {
        if(server != null) {
            server.stop(0);
        }
        sequencerLayer.exit();
    }

    @Override
    public void flush() {
        surface.updateHardware();
    }

}
