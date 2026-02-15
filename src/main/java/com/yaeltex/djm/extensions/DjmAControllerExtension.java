package com.yaeltex.djm.extensions;

import java.util.List;

import com.bitwig.extension.controller.ControllerExtension;
import com.bitwig.extension.controller.api.ControllerHost;
import com.bitwig.extension.controller.api.HardwareSurface;
import com.bitwig.extension.controller.api.Track;
import com.bitwig.extensions.framework.Layer;
import com.bitwig.extensions.framework.Layers;
import com.bitwig.extensions.framework.di.Context;
import com.yaeltex.common.IntValueObject;
import com.yaeltex.common.YaeltexButtonLedState;
import com.yaeltex.common.controls.RgbButton;
import com.yaeltex.common.controls.RingEncoder;
import com.yaeltex.common.controls.VuMeter;
import com.yaeltex.djm.DjmAHwElements;
import com.yaeltex.djm.DjmMidiProcessor;
import com.yaeltex.djm.DjmViewControl;
import com.yaeltex.djm.definitions.DjmAExtensionDefinition;

public class DjmAControllerExtension extends ControllerExtension {
    
    private static ControllerHost debugHost;
    private HardwareSurface surface;
    private Layer mainLayer;
    
    public static void println(final String format, final Object... args) {
        if (debugHost != null) {
            debugHost.println(format.formatted(args));
        }
    }
    
    public DjmAControllerExtension(final DjmAExtensionDefinition definition, final ControllerHost host) {
        super(definition, host);
    }
    
    @Override
    public void init() {
        debugHost = getHost();
        
        final Context diContext = new Context(this);
        final DjmMidiProcessor midiProcessor = new DjmMidiProcessor(getHost(), 1);
        surface = diContext.getService(HardwareSurface.class);
        surface.setPhysicalSize(190, 360);
        final DjmAHwElements hwElements = new DjmAHwElements(getHost(), surface, midiProcessor, 0);
        diContext.registerService(DjmAHwElements.class, hwElements);
        diContext.registerService(DjmMidiProcessor.class, midiProcessor);
        mainLayer = new Layer(diContext.getService(Layers.class), "MAIN_LAYER");
        for (int i = 0; i < 2; i++) {
            midiProcessor.sendText(0, i, "");
        }
        
        final RgbButton button1 = hwElements.getSideButtons().get(0);
        button1.bindLight(mainLayer, () -> YaeltexButtonLedState.YELLOW.intensity(10));
        final RgbButton button2 = hwElements.getSideButtons().get(1);
        button2.bindLight(mainLayer, () -> YaeltexButtonLedState.YELLOW.intensity(127));
        final RingEncoder encoder1 = hwElements.getSculptEncoders().get(0);
        final RgbButton b1 = encoder1.getButton();
        encoder1.bindLight(mainLayer, () -> YaeltexButtonLedState.RED);
        b1.bindLight(mainLayer, () -> YaeltexButtonLedState.ORANGE);
        
        encoder1.bindAccelerated(mainLayer, v -> println(" ENCODER %d", v), 100);
        
        final RingEncoder encoder2 = hwElements.getSculptEncoders().get(1);
        final RgbButton b2 = encoder2.getButton();
        encoder2.bindLight(mainLayer, () -> YaeltexButtonLedState.RED.intensity(50));
        b2.bindLight(mainLayer, () -> YaeltexButtonLedState.ORANGE);
        
        final RingEncoder encoder3 = hwElements.getSculptEncoders().get(2);
        final IntValueObject testValue = new IntValueObject(0, 0, 127);
        final RgbButton b3 = encoder3.getButton();
        encoder3.bindLight(mainLayer, () -> YaeltexButtonLedState.RED);
        encoder3.bindValueLight(mainLayer, testValue);
        encoder3.bindAccelerated(mainLayer, inc -> testValue.increment(inc), 169);
        b3.bindLight(mainLayer, () -> YaeltexButtonLedState.ORANGE.intensity(30));
        
        final RingEncoder encoder4 = hwElements.getSculptEncoders().get(3);
        final RgbButton b4 = encoder4.getButton();
        encoder4.bindLight(mainLayer, () -> YaeltexButtonLedState.GREEN);
        encoder4.updateValue(0);
        b4.bindLight(mainLayer, () -> YaeltexButtonLedState.PURPLE);
        
        final DjmViewControl viewControl = diContext.getService(DjmViewControl.class);
        final Track rootTrack = viewControl.getRootTrack();
        final VuMeter vuLeft = hwElements.getVuLeft();
        final VuMeter vuRight = hwElements.getVuRight();
        rootTrack.addVuMeterObserver(128, 0, true, value -> vuLeft.sendVuValue(value));
        rootTrack.addVuMeterObserver(128, 1, true, value -> vuRight.sendVuValue(value));
        
        testValue.addValueObserver(v -> {
            vuRight.sendEncoderValue(v);
        });
        disableLights(mainLayer, hwElements.getMelodyEncoders());
        disableLights(mainLayer, hwElements.getDrumEncoders());
        disableLights(mainLayer, hwElements.getVocalEncoders());
        disableLights(mainLayer, hwElements.getBaselineEncoders());
        
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
