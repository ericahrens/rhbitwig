package com.yaeltex.seqarp168mk2.arpcontrol;

import com.bitwig.extensions.framework.Layers;
import com.bitwig.extensions.framework.values.ValueObject;
import com.yaeltex.seqarp168mk2.BitwigViewControl;
import com.yaeltex.seqarp168mk2.SeqArpHardwareElements;
import com.yaeltex.seqarp168mk2.device.FocusDevice;

public class Encoder2x16StepConfig extends DeviceConfig {
    
    private final ArpEncoderLayer noteLayer;
    private final ArpEncoderLayer offsetLayer;
    private final ArpEncoderLayer gateLayer;
    private final ArpEncoderLayer velLayer;
    
    public Encoder2x16StepConfig(final Layers layers, final SeqArpHardwareElements hwElements,
        final BitwigViewControl viewControl, final ValueObject<FocusDeviceMode> deviceFocus) {
        super(ControlMode.MODE16x2, deviceFocus);
        noteLayer = new ArpEncoderLayer(layers, "2x16Device_note");
        offsetLayer = new ArpEncoderLayer(layers, "2x16Device_offset");
        gateLayer = new ArpEncoderLayer(layers, "2x16Device_gate");
        velLayer = new ArpEncoderLayer(layers, "2x16Device_vel");
        
        for (int i = 0; i < 16; i++) {
            bindNotes(noteLayer, i, 0, hwElements, viewControl.getArpDevice1());
            bindNotes(noteLayer, i, 1, hwElements, viewControl.getArpDevice2());
            bindOffsets(offsetLayer, i, 0, hwElements, viewControl.getArpDevice1());
            bindOffsets(offsetLayer, i, 1, hwElements, viewControl.getArpDevice2());
            bindGates(gateLayer, i, 0, hwElements, viewControl.getArpDevice1());
            bindGates(gateLayer, i, 1, hwElements, viewControl.getArpDevice2());
            bindVelocities(velLayer, i, 0, hwElements, viewControl.getArpDevice1());
            bindVelocities(velLayer, i, 1, hwElements, viewControl.getArpDevice2());
        }
    }
    
    private void bindNotes(final ArpEncoderLayer layer, final int index, final int section,
        final SeqArpHardwareElements hwElements, final FocusDevice focusDevice) {
        final int buttonIndex = index + section * 16;
        layer.bindNotes(index, buttonIndex, hwElements, focusDevice);
    }
    
    private void bindOffsets(final ArpEncoderLayer layer, final int index, final int section,
        final SeqArpHardwareElements hwElements, final FocusDevice focusDevice) {
        final int buttonIndex = index + section * 16;
        layer.bindOffset(index, buttonIndex, hwElements, focusDevice);
    }
    
    private void bindGates(final ArpEncoderLayer layer, final int index, final int section,
        final SeqArpHardwareElements hwElements, final FocusDevice focusDevice) {
        final int buttonIndex = index + section * 16;
        layer.bindGate(index, buttonIndex, hwElements, focusDevice);
    }
    
    private void bindVelocities(final ArpEncoderLayer layer, final int index, final int section,
        final SeqArpHardwareElements hwElements, final FocusDevice focusDevice) {
        final int buttonIndex = index + section * 16;
        layer.bindVelocity(index, buttonIndex, hwElements, focusDevice);
    }
    
    public void changeEncoderMode(final boolean pressed) {
        if (!pressed) {
            return;
        }
        if (getStepEncoderMode() == StepEncoderMode.DEFAULT) {
            setEncoderMode(StepEncoderMode.MODE_1);
        } else if (getStepEncoderMode() == StepEncoderMode.MODE_1) {
            setEncoderMode(StepEncoderMode.MODE_2);
        } else if (getStepEncoderMode() == StepEncoderMode.MODE_2) {
            setEncoderMode(StepEncoderMode.MODE_3);
        } else if (getStepEncoderMode() == StepEncoderMode.MODE_3) {
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
        noteLayer.setIsActive(stepEncoderMode == StepEncoderMode.DEFAULT);
        offsetLayer.setIsActive(stepEncoderMode == StepEncoderMode.MODE_1);
        gateLayer.setIsActive(stepEncoderMode == StepEncoderMode.MODE_2);
        velLayer.setIsActive(stepEncoderMode == StepEncoderMode.MODE_3);
    }
    
    public void setIsActive(final boolean active) {
        this.active = active;
        if (active) {
            updateLayers();
        } else {
            noteLayer.setIsActive(false);
            offsetLayer.setIsActive(false);
            gateLayer.setIsActive(false);
            velLayer.setIsActive(false);
        }
    }
}
