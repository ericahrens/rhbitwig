package com.yaeltex.seqarp168mk2.arpcontrol;

import java.util.function.Consumer;

import com.bitwig.extensions.framework.Layers;
import com.bitwig.extensions.framework.values.ValueObject;
import com.yaeltex.seqarp168mk2.BitwigViewControl;

public class ButtonDeviceConfiguration extends DeviceConfig {
    private final ButtonDeviceLayer layer1;
    private final ButtonDeviceLayer layer2;
    
    public ButtonDeviceConfiguration(final Layers layers, final String name, final BitwigViewControl viewControl,
        final ValueObject<FocusDeviceMode> deviceFocus) {
        super(null, deviceFocus);
        layer1 = new ButtonDeviceLayer(layers, "%s_BUTTON_1".formatted(name), viewControl.getArpDevice1());
        layer2 = new ButtonDeviceLayer(layers, "%s_BUTTON_2".formatted(name), viewControl.getArpDevice2());
        
        deviceFocus.addValueObserver(((oldValue, newValue) -> changeDeviceFocus(newValue)));
    }
    
    public void bind(final Consumer<ButtonDeviceLayer> binder) {
        binder.accept(layer1);
        binder.accept(layer2);
    }
    
    private void changeDeviceFocus(final FocusDeviceMode newValue) {
        if (!this.active) {
            return;
        }
        if (newValue == FocusDeviceMode.DEVICE_1) {
            layer2.setIsActive(false);
            layer1.setIsActive(true);
        } else {
            layer1.setIsActive(false);
            layer2.setIsActive(true);
        }
    }
    
    @Override
    public void setIsActive(final boolean active) {
        this.active = active;
        if (deviceFocus.get() == FocusDeviceMode.DEVICE_1) {
            layer1.setIsActive(active);
        } else if (deviceFocus.get() == FocusDeviceMode.DEVICE_2) {
            layer2.setIsActive(active);
        }
    }
    
    
}
