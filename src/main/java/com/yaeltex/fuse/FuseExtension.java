package com.yaeltex.fuse;

import java.time.LocalDateTime;

import com.bitwig.extension.controller.ControllerExtension;
import com.bitwig.extension.controller.api.ControllerHost;
import com.bitwig.extension.controller.api.HardwareSlider;
import com.bitwig.extension.controller.api.HardwareSurface;
import com.bitwig.extensions.framework.Layer;
import com.bitwig.extensions.framework.Layers;
import com.bitwig.extensions.framework.di.Context;
import com.yaeltex.common.YaeltexMidiProcessor;

public class FuseExtension extends ControllerExtension {
    
    private static ControllerHost debugHost;
    private Layer mainLayer;
    private HardwareSurface surface;
    
    public static void println(final String format, final Object... args) {
        if (debugHost != null) {
            final LocalDateTime now = LocalDateTime.now();
            //debugHost.println(now.format(DF) + " > " + String.format(format, args));
            debugHost.println(format.formatted(args));
        }
    }

    protected FuseExtension(
        final FuseControlExtensionDefinition definition, final ControllerHost host) {
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
        
        mainLayer.setIsActive(true);
        diContext.activate();
        midiProcessor.start();
    
        final HardwareSlider masterFilter = hwElements.getMasterSlider();
    }
    
    @Override
    public void exit() {
    
    }
    
    @Override
    public void flush() {
        surface.updateHardware();
    }
  
}
