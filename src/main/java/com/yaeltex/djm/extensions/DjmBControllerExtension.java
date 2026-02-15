package com.yaeltex.djm.extensions;

import java.util.List;

import com.bitwig.extension.controller.ControllerExtension;
import com.bitwig.extension.controller.api.ControllerHost;
import com.bitwig.extension.controller.api.HardwareSlider;
import com.bitwig.extension.controller.api.HardwareSurface;
import com.bitwig.extension.controller.api.Track;
import com.bitwig.extension.controller.api.TrackBank;
import com.bitwig.extensions.framework.Layer;
import com.bitwig.extensions.framework.Layers;
import com.bitwig.extensions.framework.di.Context;
import com.yaeltex.common.YaeltexButtonLedState;
import com.yaeltex.common.controls.RingEncoder;
import com.yaeltex.common.controls.VuMeter;
import com.yaeltex.djm.DjmBHwElements;
import com.yaeltex.djm.DjmMidiProcessor;
import com.yaeltex.djm.DjmViewControl;
import com.yaeltex.djm.VuTrackIndicatorBinding;
import com.yaeltex.djm.definitions.DjmBExtensionDefinition;

public class DjmBControllerExtension extends ControllerExtension {
    
    private static ControllerHost debugHost;
    private HardwareSurface surface;
    private Layer mainLayer;
    
    public static void println(final String format, final Object... args) {
        if (debugHost != null) {
            debugHost.println(format.formatted(args));
        }
    }
    
    public DjmBControllerExtension(final DjmBExtensionDefinition definition, final ControllerHost host) {
        super(definition, host);
    }
    
    @Override
    public void init() {
        debugHost = getHost();
        
        final Context diContext = new Context(this);
        final DjmMidiProcessor midiProcessor = new DjmMidiProcessor(getHost(), 1);
        diContext.registerService(DjmMidiProcessor.class, midiProcessor);
        surface = diContext.getService(HardwareSurface.class);
        surface.setPhysicalSize(190, 360);
        mainLayer = new Layer(diContext.getService(Layers.class), "MAIN_LAYER");
        final DjmBHwElements hwElements = new DjmBHwElements(surface, midiProcessor, 0);
        diContext.registerService(DjmBHwElements.class, hwElements);
        
        for (int i = 0; i < 14; i++) {
            midiProcessor.sendText(0, i, "");
        }
        
        final DjmViewControl viewControl = diContext.getService(DjmViewControl.class);
        final Track rootTrack = viewControl.getRootTrack();
        final VuMeter vuLeft = hwElements.getVuLeft();
        final VuMeter vuRight = hwElements.getVuRight();
        rootTrack.addVuMeterObserver(128, 0, true, value -> vuLeft.sendVuValue(value));
        rootTrack.addVuMeterObserver(128, 1, true, value -> vuRight.sendVuValue(value));
        
        final TrackBank trackBank = viewControl.getTrackBank();
        for (int i = 0; i < 4; i++) {
            final Track track = trackBank.getItemAt(i);
            final VuMeter vuMeter = hwElements.getTrackMeters().get(i);
            final HardwareSlider slider = hwElements.getTrackSliders().get(i);
            mainLayer.addBinding(new VuTrackIndicatorBinding(track, vuMeter));
            mainLayer.bind(slider, track.volume());
        }
        for (int i = 0; i < 8; i++) {
            final Track track = trackBank.getItemAt(i + 4);
            final VuMeter vuMeter = hwElements.getDeckMeters().get(i);
            final HardwareSlider slider = hwElements.getDeckSliders().get(i);
            mainLayer.addBinding(new VuTrackIndicatorBinding(track, vuMeter));
            mainLayer.bind(slider, track.volume());
        }
        
        midiProcessor.start();
        diContext.activate();
        mainLayer.setIsActive(true);
    }
    
    private void disableLights(final Layer layer, final List<RingEncoder> encoders) {
        for (final RingEncoder encoder : encoders) {
            encoder.bindLight(mainLayer, () -> YaeltexButtonLedState.OFF);
        }
    }
    
    @Override
    public void exit() {
        
    }
    
    @Override
    public void flush() {
        surface.updateHardware();
    }
    
}
