package com.yaeltex.seqarp168mk2.arpcontrol;

import com.bitwig.extensions.framework.Layers;
import com.bitwig.extensions.framework.values.ValueObject;
import com.yaeltex.seqarp168mk2.BitwigViewControl;
import com.yaeltex.seqarp168mk2.SeqArpHardwareElements;

public class ButtonDeviceConfiguration extends DeviceConfig {
    private final ButtonDeviceLayer layer1;
    private final ButtonDeviceLayer layer2;
    
    public ButtonDeviceConfiguration(final Layers layers, final SeqArpHardwareElements hwElements,
        final BitwigViewControl viewControl, final ValueObject<FocusDeviceMode> deviceFocus) {
        super(deviceFocus);
        layer1 = new ButtonDeviceLayer(layers, "BUTTON_1", viewControl.getArpDevice1());
        layer2 = new ButtonDeviceLayer(layers, "BUTTON_2", viewControl.getArpDevice2());
        
        layer1.bindButtonValues(hwElements);
        layer2.bindButtonValues(hwElements);
        
        deviceFocus.addValueObserver(((oldValue, newValue) -> changeDeviceFocus(newValue)));
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
