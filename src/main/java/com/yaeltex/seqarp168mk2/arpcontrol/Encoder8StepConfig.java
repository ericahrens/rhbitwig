package com.yaeltex.seqarp168mk2.arpcontrol;

import com.bitwig.extensions.framework.Layers;
import com.bitwig.extensions.framework.values.ValueObject;
import com.yaeltex.seqarp168mk2.BitwigViewControl;
import com.yaeltex.seqarp168mk2.SeqArpHardwareElements;

public class Encoder8StepConfig extends DeviceConfig {
    
    private final ArpEncoderDeviceLayer layer1;
    private final ArpEncoderDeviceLayer layer2;
    
    public Encoder8StepConfig(final Layers layers, final SeqArpHardwareElements hwElements,
        final BitwigViewControl viewControl, final ValueObject<FocusDeviceMode> deviceFocus) {
        super(deviceFocus);
        layer1 = new ArpEncoderDeviceLayer(layers, "1x8Device1", viewControl.getArpDevice1());
        layer2 = new ArpEncoderDeviceLayer(layers, "1x8Device2", viewControl.getArpDevice2());
        
        for (int i = 0; i < 8; i++) {
            layer1.bindStep8(i, hwElements);
            layer2.bindStep8(i, hwElements);
        }
        
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
    
    public void setIsActive(final boolean active) {
        this.active = active;
        if (deviceFocus.get() == FocusDeviceMode.DEVICE_1) {
            layer1.setIsActive(active);
        } else if (deviceFocus.get() == FocusDeviceMode.DEVICE_2) {
            layer2.setIsActive(active);
        }
    }
}
