package com.yaeltex.seqarp168mk2.arpcontrol;

import com.bitwig.extensions.framework.Layers;
import com.bitwig.extensions.framework.values.ValueObject;
import com.yaeltex.seqarp168mk2.BitwigViewControl;
import com.yaeltex.seqarp168mk2.SeqArpHardwareElements;
import com.yaeltex.seqarp168mk2.device.FocusDevice;

public class Encoder16StepConfig extends DeviceConfig {
    private final ArpEncoderLayer mainLayer1;
    private final ArpEncoderLayer mainLayer2;
    private final ArpEncoderLayer scondaryLayer1;
    private final ArpEncoderLayer scondaryLayer2;
    
    public Encoder16StepConfig(final Layers layers, final SeqArpHardwareElements hwElements,
        final BitwigViewControl viewControl, final ValueObject<FocusDeviceMode> deviceFocus) {
        super(ControlMode.MODE16x1, deviceFocus);
        mainLayer1 = new ArpEncoderLayer(layers, "1x16Device1-1");
        mainLayer2 = new ArpEncoderLayer(layers, "1x16Device2-1");
        scondaryLayer1 = new ArpEncoderLayer(layers, "1x16Device1-2");
        scondaryLayer2 = new ArpEncoderLayer(layers, "1x16Device2-2");
        
        for (int i = 0; i < 16; i++) {
            bindMain(mainLayer1, i, hwElements, viewControl.getArpDevice1());
            bindMain(mainLayer2, i, hwElements, viewControl.getArpDevice2());
            bindSecond(scondaryLayer1, i, hwElements, viewControl.getArpDevice1());
            bindSecond(scondaryLayer2, i, hwElements, viewControl.getArpDevice2());
        }
        
        deviceFocus.addValueObserver(((oldValue, newValue) -> changeDeviceFocus(newValue)));
    }
    
    private void bindMain(final ArpEncoderLayer layer, final int index, final SeqArpHardwareElements hwElements,
        final FocusDevice focusDevice) {
        layer.bindNotes(index, index, hwElements, focusDevice);
        layer.bindGate(index, index + 16, hwElements, focusDevice);
    }
    
    private void bindSecond(final ArpEncoderLayer layer, final int index, final SeqArpHardwareElements hwElements,
        final FocusDevice focusDevice) {
        layer.bindVelocity(index, index + 16, hwElements, focusDevice);
    }
    
    public void changeEncoderMode(final boolean pressed) {
        if (!pressed) {
            return;
        }
        if (getStepEncoderMode() == StepEncoderMode.DEFAULT) {
            setEncoderMode(StepEncoderMode.MODE_1);
        } else if (getStepEncoderMode() == StepEncoderMode.MODE_1) {
            setEncoderMode(StepEncoderMode.DEFAULT);
        }
    }
    
    private void setEncoderMode(final StepEncoderMode mode) {
        this.stepEncoderMode = mode;
        updateLayers();
    }
    
    private void changeDeviceFocus(final FocusDeviceMode newValue) {
        if (!this.active) {
            return;
        }
        updateLayers();
    }
    
    private void updateLayers() {
        mainLayer1.setIsActive(deviceFocus.get() == FocusDeviceMode.DEVICE_1);
        mainLayer2.setIsActive(deviceFocus.get() == FocusDeviceMode.DEVICE_2);
        if (stepEncoderMode == StepEncoderMode.MODE_1) {
            scondaryLayer1.setIsActive(deviceFocus.get() == FocusDeviceMode.DEVICE_1);
            scondaryLayer2.setIsActive(deviceFocus.get() == FocusDeviceMode.DEVICE_2);
        } else {
            scondaryLayer1.setIsActive(false);
            scondaryLayer2.setIsActive(false);
        }
    }
    
    public void setIsActive(final boolean active) {
        this.active = active;
        if (deviceFocus.get() == FocusDeviceMode.DEVICE_1) {
            mainLayer1.setIsActive(active);
        } else if (deviceFocus.get() == FocusDeviceMode.DEVICE_2) {
            mainLayer2.setIsActive(active);
        }
    }
}
