package com.yaeltex.fuse;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.bitwig.extension.controller.ControllerExtension;
import com.bitwig.extension.controller.api.AbsoluteHardwareKnob;
import com.bitwig.extension.controller.api.ControllerHost;
import com.bitwig.extension.controller.api.HardwareSlider;
import com.bitwig.extension.controller.api.HardwareSurface;
import com.bitwig.extension.controller.api.Project;
import com.bitwig.extension.controller.api.Send;
import com.bitwig.extension.controller.api.Track;
import com.bitwig.extension.controller.api.Transport;
import com.bitwig.extensions.framework.Layer;
import com.bitwig.extensions.framework.Layers;
import com.bitwig.extensions.framework.di.Context;
import com.yaeltex.common.YaeltexButtonLedState;
import com.yaeltex.common.YaeltexMidiProcessor;
import com.yaeltex.controls.RingEncoder;
import com.yaeltex.devices.DirectDevice;
import com.yaeltex.devices.DirectDeviceControl;

public class FuseExtension extends ControllerExtension {
    
    private static ControllerHost debugHost;
    private Layer mainLayer;
    private HardwareSurface surface;
    private final List<DirectDeviceControl> deviceControls = new ArrayList<>();
    private final SendMode[][] modeStates = new SendMode[6][4];
    private final boolean[][] preFaderStates = new boolean[6][4];
    private int selectedFxChan = -1;
    private BitwigControl bitwigControl;
    
    public static void println(final String format, final Object... args) {
        if (debugHost != null) {
            debugHost.println(format.formatted(args));
        }
    }
    
    protected FuseExtension(final FuseControlExtensionDefinition definition, final ControllerHost host) {
        super(definition, host);
    }
    
    @Override
    public void init() {
        debugHost = getHost();
        for (int i = 0; i < modeStates.length; i++) {
            Arrays.fill(modeStates[i], SendMode.AUTO);
        }
        
        final Context diContext = new Context(this);
        final YaeltexMidiProcessor midiProcessor = new YaeltexMidiProcessor(getHost());
        diContext.registerService(YaeltexMidiProcessor.class, midiProcessor);
        mainLayer = new Layer(diContext.getService(Layers.class), "MAIN_LAYER");
        surface = diContext.getService(HardwareSurface.class);
        final HwElements hwElements = diContext.getService(HwElements.class);
        bitwigControl = diContext.getService(BitwigControl.class);
        
        final HardwareSlider masterFilter = hwElements.getMasterSlider();
        bindTracks(diContext);
        midiProcessor.start();
        
        diContext.activate();
        mainLayer.setIsActive(true);
        deviceControls.forEach(DirectDeviceControl::activate);
    }
    
    private void bindTracks(final Context diContext) {
        final HwElements hwElements = diContext.getService(HwElements.class);
        final Transport transport = diContext.getService(Transport.class);
        final Project project = diContext.getService(Project.class);
        final ControllerHost host = diContext.getService(ControllerHost.class);
        final Layers layers = diContext.getService(Layers.class);
        
        final RingEncoder[] encoders = hwElements.getEncoders();
        
        //        final IntValueObject iv = new IntValueObject(0, 0, 100);
        //        encoders[0].bind(mainLayer, iv);
        //        encoders[0].bindLight(mainLayer, () -> YaeltexButtonLedState.YELLOW);
        //        encoders[1].bind(mainLayer, project.cueMix(), YaelTexColors.DEEP_SKY_BLUE);
        //
        final HardwareSlider xfader = hwElements.getCrossFader();
        mainLayer.bind(xfader, transport.crossfade());
        
        final AbsoluteHardwareKnob[] masterKnobs = hwElements.getMasterControls();
        
        final Track rootTrack = bitwigControl.getRootTrack();
        mainLayer.bind(masterKnobs[0], rootTrack.volume());
        mainLayer.bind(masterKnobs[1], project.cueMix());
        mainLayer.bind(masterKnobs[2], project.cueVolume());
        
        for (int i = 0; i < 6; i++) {
            final StripControl stripControl = hwElements.getStripControls().get(i);
            final Track track = bitwigControl.getTrackBank().getItemAt(i);
            bindSynControl(i, hwElements, host, track, layers);
            bind(stripControl, track, i);
        }
        
        for (int i = 0; i < 4; i++) {
            final Track effectsTrack = bitwigControl.getEffectTrackBank().getItemAt(i);
            final AbsoluteHardwareKnob control = hwElements.getFxControls()[i];
            mainLayer.bind(control, effectsTrack.volume());
        }
        
        for (int i = 0; i < 4; i++) {
            final int sendIndex = i;
            final RingEncoder encoder = encoders[i];
            encoder.getButton().bindPressed(mainLayer, () -> togglePrePostLocal(sendIndex));
            encoder.getButton().bindLight(mainLayer, () -> getSendSate(sendIndex));
        }
        
    }
    
    private void togglePrePostLocal(final int sendIndex) {
        if (selectedFxChan == -1) {
            return;
        }
        final Track track = bitwigControl.getTrackBank().getItemAt(selectedFxChan);
        final Send send = track.sendBank().getItemAt(sendIndex);
        final SendMode newState = modeStates[selectedFxChan][sendIndex].toggle();
        send.sendMode().set(newState.getEnumRaw());
    }
    
    private YaeltexButtonLedState getSendSate(final int sendIndex) {
        if (selectedFxChan == -1) {
            return YaeltexButtonLedState.OFF;
        }
        return preFaderStates[selectedFxChan][sendIndex] ? YaeltexButtonLedState.AQUA : YaeltexButtonLedState.YELLOW;
    }
    
    private void bindSynControl(final int index, final HwElements hwElements, final ControllerHost host,
        final Track track, final Layers layers) {
        final DirectDeviceControl control = new DirectDeviceControl(index, track, host, layers);
        deviceControls.add(control);
        Arrays.stream(DirectDevice.values()).forEach(control::addSpecificBitwig);
        if (index == 0) {
            control.bindSynth(hwElements.getSynthControl1());
        } else if (index == 1) {
            control.bindSynth(hwElements.getSynthControl2());
        }
        switch (index) {
            case 0 -> control.bindSynth(hwElements.getSynthControl1());
            case 1 -> control.bindSynth(hwElements.getSynthControl2());
            case 2 -> control.bindSynth(hwElements.getSynthControl3());
            case 3 -> control.bindSynth(hwElements.getSynthControl4());
            case 4 -> control.bindSynth(hwElements.getSynthControl5());
            case 5 -> control.bindSynth(hwElements.getSynthControl6());
        }
    }
    
    private void bind(final StripControl control, final Track track, final int index) {
        mainLayer.bind(control.mainFader(), track.volume());
        control.cueButton().bindPressed(mainLayer, () -> track.solo().toggle(false));
        control.cueButton()
            .bindLight(mainLayer, () -> track.solo().get() ? YaeltexButtonLedState.YELLOW : YaeltexButtonLedState.OFF);
        control.abButton().bindLight(mainLayer, () -> fromXFadeMode(track));
        control.abButton().bindPressed(mainLayer, () -> selectAb(track));
        control.muteButton().bindPressed(mainLayer, () -> track.mute().toggle());
        control.muteButton()
            .bindLight(mainLayer, () -> track.mute().get() ? YaeltexButtonLedState.ORANGE : YaeltexButtonLedState.OFF);
        //control.fxButton().bindPressed(mainLayer, () -> togglePrePost(track, index));
        //control.fxButton().bindLight(mainLayer, () -> prePostColor(index));
        for (int i = 0; i < 4; i++) {
            final int sendIndex = i;
            final AbsoluteHardwareKnob knob = control.fxKnobs().get(i);
            final Send send = track.sendBank().getItemAt(i);
            send.sendMode().addValueObserver(enumRaw -> modeStates[index][sendIndex] = SendMode.toMode(enumRaw));
            send.isPreFader().addValueObserver(preFader -> preFaderStates[index][sendIndex] = preFader);
            mainLayer.bind(knob, send);
        }
        control.fxButton().bindPressed(mainLayer, () -> selectFxFocus(index));
        control.fxButton().bindLight(mainLayer, () -> fxSelectionIndex(index));
    }
    
    private void selectFxFocus(final int index) {
        if (selectedFxChan == index) {
            selectedFxChan = -1;
        } else {
            selectedFxChan = index;
        }
    }
    
    private YaeltexButtonLedState fxSelectionIndex(final int index) {
        return selectedFxChan == index ? YaeltexButtonLedState.AQUA : YaeltexButtonLedState.OFF;
    }
    
    private YaeltexButtonLedState prePostColor(final int index) {
        return preFaderStates[index][0] ? YaeltexButtonLedState.DEEP_GREEN : YaeltexButtonLedState.AQUA;
    }
    
    private void togglePrePost(final Track track, final int index) {
        modeStates[index][0] = modeStates[index][0].toggle();
        for (int i = 0; i < 4; i++) {
            final Send send = track.sendBank().getItemAt(i);
            send.sendMode().set(modeStates[index][0].getEnumRaw());
        }
    }
    
    private void selectAb(final Track track) {
        final String mode = track.crossFadeMode().get();
        if (mode.equals("AB")) {
            track.crossFadeMode().set("A");
        }
        if (mode.equals("A")) {
            track.crossFadeMode().set("B");
        }
        if (mode.equals("B")) {
            track.crossFadeMode().set("AB");
        }
    }
    
    private YaeltexButtonLedState fromXFadeMode(final Track track) {
        final String mode = track.crossFadeMode().get();
        if (mode.equals("AB")) {
            return YaeltexButtonLedState.OFF;
        }
        if (mode.equals("A")) {
            return YaeltexButtonLedState.BLUE;
        }
        return YaeltexButtonLedState.GREEN;
    }
    
    @Override
    public void exit() {
    
    }
    
    @Override
    public void flush() {
        surface.updateHardware();
    }
    
}
