package com.allenheath.k2.set1;

import com.bitwig.extension.controller.ControllerExtension;
import com.bitwig.extension.controller.ControllerExtensionDefinition;
import com.bitwig.extension.controller.api.*;
import com.bitwig.extensions.framework.Layer;
import com.bitwig.extensions.framework.Layers;

import java.util.ArrayList;
import java.util.List;

public class AllenHeathK2ControllerExtension extends ControllerExtension {

    private static final String[] DEFAULT_PAD_ASSIGNMENTS = {"1,2", "4", "3", "5,6", "7", "9", "10", "11"};
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

        host.showPopupNotification("Intialize Xone:K2 DJ Set");
    }

    private void initDeckCaptureButtons() {
        for (int i = 0; i < 4; i++) {
            final int index = i;
            HardwareButton button = hwElements.getCaptureKeysFromDeckButton(i);
            mainLayer.bindPressed(button, () -> fetchKey(index));
        }
    }

    private void fetchKey(int index) {
        println(" FETCH KEY %d", index);
    }

    private void onMidi(int msg, int data1, int data2) {
        println("MIDI> %02X %02X %02X", msg, data1, data2);
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
        sequencerLayer.exit();
    }

    @Override
    public void flush() {
        surface.updateHardware();
    }

}
