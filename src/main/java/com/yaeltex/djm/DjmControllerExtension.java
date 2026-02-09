package com.yaeltex.djm;

import com.bitwig.extension.controller.ControllerExtension;
import com.bitwig.extension.controller.api.ControllerHost;
import com.bitwig.extension.controller.api.HardwareSurface;
import com.bitwig.extensions.framework.Layer;
import com.bitwig.extensions.framework.di.Context;
import com.yaeltex.djm.definitions.DjmExtensionDefinition;

public class DjmControllerExtension extends ControllerExtension {
    
    private static ControllerHost debugHost;
    private HardwareSurface surface;
    private Layer mainLayer;
    
    public static void println(final String format, final Object... args) {
        if (debugHost != null) {
            debugHost.println(format.formatted(args));
        }
    }
    
    public DjmControllerExtension(final DjmExtensionDefinition definition, final ControllerHost host) {
        super(definition, host);
    }
    
    @Override
    public void init() {
        debugHost = getHost();
        final Context diContext = new Context(this);
        final DjmMidiProcessor midiProcessor = new DjmMidiProcessor(getHost(), 2);
        surface = diContext.getService(HardwareSurface.class);
        surface.setPhysicalSize(380, 360);
        final DjmAHwElements hwElementsA = new DjmAHwElements(getHost(), surface, midiProcessor, 0);
        final DjmBHwElements hwElementsB = new DjmBHwElements(getHost(), surface, midiProcessor, 1);
    }
    
    
    @Override
    public void exit() {
    
    }
    
    @Override
    public void flush() {
        surface.updateHardware();
    }
    
}
