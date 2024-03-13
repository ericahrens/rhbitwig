package com.yaeltex.seqarp168mk2;

import com.bitwig.extension.controller.ControllerExtension;
import com.bitwig.extension.controller.api.ControllerHost;
import com.bitwig.extension.controller.api.HardwareSurface;
import com.bitwig.extensions.framework.Layer;
import com.bitwig.extensions.framework.di.Context;
import com.yaeltex.common.YaeltexButtonLedState;
import com.yaeltex.common.YaeltexIntensityColorState;
import com.yaeltex.common.YaeltexMidiProcessor;
import com.yaeltex.controls.RgbButton;
import com.yaeltex.seqarp168mk2.arpcontrol.ArpControlLayer;

public class SeqArp168Extension extends ControllerExtension {
    
    private static ControllerHost debugHost;
    private Layer mainLayer;
    private HardwareSurface surface;
    private final YaeltexIntensityColorState cursorColor = new YaeltexIntensityColorState(12, 127);
    private Mode mode = Mode.ARP;
    private Layer currentLayer;
    
    public static void println(final String format, final Object... args) {
        if (debugHost != null) {
            debugHost.println(format.formatted(args));
        }
    }
    
    protected SeqArp168Extension(final SeqArp168ExtensionDefinition definition, final ControllerHost host) {
        super(definition, host);
    }
    
    @Override
    public void init() {
        debugHost = getHost();
        final Context diContext = new Context(this);
        final YaeltexMidiProcessor midiProcessor = new YaeltexMidiProcessor(getHost());
        diContext.registerService(YaeltexMidiProcessor.class, midiProcessor);
        mainLayer = diContext.createLayer("MAIN_LAYER");
        surface = diContext.getService(HardwareSurface.class);
        initModeButton(diContext);
        
        mainLayer.activate();
        currentLayer.activate();
        diContext.activate();
        midiProcessor.start();
    }
    
    private void initModeButton(final Context diContext) {
        final SeqArpHardwareElements hwElements = diContext.getService(SeqArpHardwareElements.class);
        final RgbButton modeButton = hwElements.getControlButton(7);
        
        final ArpControlLayer arpLayer = diContext.getService(ArpControlLayer.class);
        currentLayer = arpLayer;
        
        modeButton.bindPressed(mainLayer, () -> {
            this.mode = this.mode.next();
            if (currentLayer != null) {
                currentLayer.setIsActive(false);
            }
            if (this.mode == Mode.ARP) {
                currentLayer = arpLayer;
            } else {
                currentLayer = null;
            }
            if (currentLayer != null) {
                currentLayer.setIsActive(true);
            }
        });
        modeButton.bindLight(mainLayer, () -> getModeLight());
    }
    
    private YaeltexButtonLedState getModeLight() {
        return switch (mode) {
            case ARP -> YaeltexButtonLedState.BLUE;
            case SEQUENCER -> YaeltexButtonLedState.ORANGE;
            case DEVICE -> YaeltexButtonLedState.PURPLE;
        };
    }
    
    private void initTest() {
        //        viewControl.getCursorTrack().color().addValueObserver(
        //            (r, g, b) -> cursorColor = new YaeltexIntensityColorState(YaeltexButtonLedState.of(r, g, b),
        //            127));
        //        final RingEncoder encoder1 = hwElements.getEncoder(0);
        //        final RingEncoder encoder2 = hwElements.getEncoder(1);
        //        final RingEncoder encoder3 = hwElements.getEncoder(2);
        //        //color.addValueObserver(c -> println(" Colo = %d <= %s", c, ColorUtil.getValue(c)));
        //
        //        encoder1.bind(this, viewControl.getRootTrack().volume(), YaelTexColors.RED);
        //        encoder2.bind(this, viewControl.getRootTrack().pan(), YaelTexColors.BRIGHT_YELLOW);
        //        encoder3.bind(this, color);
        //        encoder3.getButton().bindLight(this, () -> YaeltexButtonLedState.of(color.getValue()));
        //        encoder1.getButton().bindLight(this, () -> cursorColor);
        //
        //        encoder3.bindLight(this, () -> YaeltexButtonLedState.of(color.getValue()));
        //        for (int i = 0; i < 32; i++) {
        //            final int index = i;
        //            final RgbButton button = hwElements.getStepButton(i);
        //            button.bindLight(this, () -> cursorColor);
        //            //button.bindPressed(this, () ->  println(" > %d", index));
        //        }
        //
        //        for (int i = 0; i < 8; i++) {
        //            final RgbButton button = hwElements.getControlButton(i);
        //            button.bindLight(this, () -> YaeltexButtonLedState.BLUE);
        //        }
        //
    }
    
    public void exit() {
        getHost().showPopupNotification("YaeltexArpControl Exited");
    }
    
    @Override
    public void flush() {
        surface.updateHardware();
    }
    
}
