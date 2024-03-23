package com.yaeltex.seqarp168mk2.arpcontrol;

import com.bitwig.extensions.framework.Layers;
import com.bitwig.extensions.framework.values.ValueObject;
import com.yaeltex.seqarp168mk2.BitwigViewControl;
import com.yaeltex.seqarp168mk2.SeqArpHardwareElements;
import com.yaeltex.seqarp168mk2.device.FocusDevice;

public class Encoder2x8StepConfig extends DeviceConfig {
    
    private final ArpEncoderLayer layer1;
    private final ArpEncoderLayer layer2;
    
    public Encoder2x8StepConfig(final Layers layers, final SeqArpHardwareElements hwElements,
        final BitwigViewControl viewControl, final ValueObject<FocusDeviceMode> deviceFocus) {
        super(ControlMode.MODE8x2, deviceFocus);
        layer1 = new ArpEncoderLayer(layers, "2x8Device1");
        layer2 = new ArpEncoderLayer(layers, "2x8Device2");
        
        for (int i = 0; i < 8; i++) {
            bindNotesGates(layer1, i, 0, hwElements, viewControl.getArpDevice1());
            bindNotesGates(layer1, i, 1, hwElements, viewControl.getArpDevice2());
            bindVelocities(layer2, i, 0, hwElements, viewControl.getArpDevice1());
            bindVelocities(layer2, i, 1, hwElements, viewControl.getArpDevice2());
        }
        //deviceFocus.addValueObserver(((oldValue, newValue) -> changeDeviceFocus(newValue)));
    }
    
    private void bindNotesGates(final ArpEncoderLayer layer, final int index, final int section,
        final SeqArpHardwareElements hwElements, final FocusDevice focusDevice) {
        final int buttonIndex = (index / 4) * 8 + (index % 4) + section * 4;
        layer.bindNotes(index, buttonIndex, hwElements, focusDevice);
        layer.bindGate(index, buttonIndex + 16, hwElements, focusDevice);
    }
    
    private void bindVelocities(final ArpEncoderLayer layer, final int index, final int section,
        final SeqArpHardwareElements hwElements, final FocusDevice focusDevice) {
        final int buttonIndex = (index / 4) * 8 + (index % 4) + section * 4;
        layer.bindVelocity(index, buttonIndex + 16, hwElements, focusDevice);
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
    
    private void updateLayers() {
        if (!active) {
            return;
        }
        layer1.setIsActive(true);
        if (this.stepEncoderMode == StepEncoderMode.MODE_1) {
            layer2.setIsActive(true);
        }
    }
    
    public void setIsActive(final boolean active) {
        this.active = active;
        layer1.setIsActive(active);
        if (active) {
            updateLayers();
        } else {
            layer1.setIsActive(false);
            layer2.setIsActive(false);
        }
    }
}
