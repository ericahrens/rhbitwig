package com.yaeltex.seqarp168mk2.arpcontrol;

import com.bitwig.extensions.framework.Layers;
import com.bitwig.extensions.framework.values.ValueObject;
import com.yaeltex.seqarp168mk2.BitwigViewControl;
import com.yaeltex.seqarp168mk2.SeqArpHardwareElements;
import com.yaeltex.seqarp168mk2.device.FocusDevice;

public class Encoder8StepConfig extends DeviceConfig {
    
    private final ArpEncoderLayer layer1;
    private final ArpEncoderLayer layer2;
    
    public Encoder8StepConfig(final Layers layers, final SeqArpHardwareElements hwElements,
        final BitwigViewControl viewControl, final ValueObject<FocusDeviceMode> deviceFocus) {
        super(ControlMode.MODE8x1, deviceFocus);
        layer1 = new ArpEncoderLayer(layers, "1x8Device1");
        layer2 = new ArpEncoderLayer(layers, "1x8Device2");
        
        for (int i = 0; i < 8; i++) {
            bind(layer1, i, hwElements, viewControl.getArpDevice1());
            bind(layer2, i, hwElements, viewControl.getArpDevice2());
        }
        deviceFocus.addValueObserver(((oldValue, newValue) -> changeDeviceFocus(newValue)));
    }
    
    private void bind(final ArpEncoderLayer layer, final int index, final SeqArpHardwareElements hwElements,
        final FocusDevice focusDevice) {
        layer.bindNotes(index, index, hwElements, focusDevice);
        layer.bindOffset(index, index + 8, hwElements, focusDevice);
        layer.bindVelocity(index, index + 16, hwElements, focusDevice);
        layer.bindGate(index, index + 24, hwElements, focusDevice);
    }
    
    private void changeDeviceFocus(final FocusDeviceMode newValue) {
        if (!this.active) {
            return;
        }
        layer1.setIsActive(deviceFocus.get() == FocusDeviceMode.DEVICE_1);
        layer2.setIsActive(deviceFocus.get() == FocusDeviceMode.DEVICE_2);
    }
    
    public void setIsActive(final boolean active) {
        this.active = active;
        if (deviceFocus.get() == FocusDeviceMode.DEVICE_1) {
            layer1.setIsActive(active);
        } else if (deviceFocus.get() == FocusDeviceMode.DEVICE_2) {
            layer2.setIsActive(active);
        }
        if (active) {
            layer1.setIsActive(deviceFocus.get() == FocusDeviceMode.DEVICE_1);
            layer2.setIsActive(deviceFocus.get() == FocusDeviceMode.DEVICE_2);
        } else {
            layer1.setIsActive(false);
            layer2.setIsActive(false);
        }
    }
}
