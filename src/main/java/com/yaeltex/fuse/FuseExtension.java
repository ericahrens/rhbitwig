package com.yaeltex.fuse;

import java.time.LocalDateTime;
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
import com.yaeltex.common.YaelTexColors;
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
    
    public static void println(final String format, final Object... args) {
        if (debugHost != null) {
            final LocalDateTime now = LocalDateTime.now();
            //debugHost.println(now.format(DF) + " > " + String.format(format, args));
            debugHost.println(format.formatted(args));
        }
    }
    
    protected FuseExtension(final FuseControlExtensionDefinition definition, final ControllerHost host) {
        super(definition, host);
    }
    
    @Override
    public void init() {
        debugHost = getHost();
        final Context diContext = new Context(this);
        final YaeltexMidiProcessor midiProcessor = new YaeltexMidiProcessor(getHost());
        diContext.registerService(YaeltexMidiProcessor.class, midiProcessor);
        mainLayer = new Layer(diContext.getService(Layers.class), "MAIN_LAYER");
        surface = diContext.getService(HardwareSurface.class);
        final HwElements hwElements = diContext.getService(HwElements.class);
        
        final HardwareSlider masterFilter = hwElements.getMasterSlider();
        bindTracks(diContext);
        midiProcessor.start();
        
        diContext.activate();
        mainLayer.setIsActive(true);
        deviceControls.forEach(control -> control.activate());
    }
    
    
    int vx = 0;
    int vx1 = 0;
    
    private void bindTracks(Context diContext) {
        final HwElements hwElements = diContext.getService(HwElements.class);
        final BitwigControl bitwigControl = diContext.getService(BitwigControl.class);
        final Transport transport = diContext.getService(Transport.class);
        final Project project = diContext.getService(Project.class);
        final ControllerHost host = diContext.getService(ControllerHost.class);
        final Layers layers = diContext.getService(Layers.class);
        
        final RingEncoder[] encoders = hwElements.getEncoders();
        
        encoders[0].bind(mainLayer, v -> {
            vx = Math.min(127, Math.max(0, vx + v));
            encoders[0].sendValue(vx);
        });
        encoders[0].sendValue(vx);
        encoders[0].setColor(50);
        encoders[1].bind(mainLayer, v -> {
            vx1 = Math.min(127, Math.max(0, vx1 + v));
            encoders[1].sendValue(vx1);
        });
        encoders[1].sendValue(vx1);
        encoders[1].setColor(YaelTexColors.DEEP_PINK.getValue());
        
        encoders[2].bind(mainLayer, project.cueMix(), YaelTexColors.DEEP_SKY_BLUE);
        
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
            bind(stripControl, track);
        }
        
        for (int i = 0; i < 4; i++) {
            final Track effectsTrack = bitwigControl.getEffectTrackBank().getItemAt(i);
            final AbsoluteHardwareKnob control = hwElements.getFxControls()[i];
            mainLayer.bind(control, effectsTrack.volume());
        }
    }
    
    private void bindSynControl(final int index, final HwElements hwElements, final ControllerHost host,
        final Track track, Layers layers) {
        final DirectDeviceControl control = new DirectDeviceControl(index, track, host, layers);
        deviceControls.add(control);
        Arrays.stream(DirectDevice.values()).forEach(type -> control.addSpecificBitwig(type));
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
    
    private void bind(StripControl control, Track track) {
        mainLayer.bind(control.mainFader(), track.volume());
        control.cueButton().bindPressed(mainLayer, () -> track.solo().toggle(false));
        control.cueButton()
            .bindLight(mainLayer, () -> track.solo().get() ? YaeltexButtonLedState.YELLOW : YaeltexButtonLedState.OFF);
        control.abButton().bindLight(mainLayer, () -> fromXFadeMode(track));
        control.abButton().bindPressed(mainLayer, () -> selectAb(track));
        control.muteButton().bindPressed(mainLayer, () -> track.mute().toggle());
        control.muteButton()
            .bindLight(mainLayer, () -> track.mute().get() ? YaeltexButtonLedState.ORANGE : YaeltexButtonLedState.OFF);
        for (int i = 0; i < 4; i++) {
            final AbsoluteHardwareKnob knob = control.fxKnobs().get(i);
            final Send send = track.sendBank().getItemAt(i);
            mainLayer.bind(knob, send);
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
    
    private YaeltexButtonLedState fromXFadeMode(Track track) {
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
