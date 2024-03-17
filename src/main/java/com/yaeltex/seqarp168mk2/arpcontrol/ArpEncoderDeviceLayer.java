package com.yaeltex.seqarp168mk2.arpcontrol;

import com.bitwig.extensions.framework.Layer;
import com.bitwig.extensions.framework.Layers;
import com.yaeltex.common.YaeltexButtonLedState;
import com.yaeltex.common.controls.RingEncoder;
import com.yaeltex.seqarp168mk2.SeqArpHardwareElements;
import com.yaeltex.seqarp168mk2.device.BitwigArpDevice;
import com.yaeltex.seqarp168mk2.device.FocusDevice;

public class ArpEncoderDeviceLayer extends Layer {
    
    private final FocusDevice focusDevice;
    
    public ArpEncoderDeviceLayer(final Layers layers, final String name, final FocusDevice focusDevice) {
        super(layers, name);
        this.focusDevice = focusDevice;
    }
    
    public void bindStep8(final int index, final SeqArpHardwareElements hwElements) {
        bindNotes(index, hwElements);
        bindOffset(index, hwElements, 8);
        bindVelocity(index, hwElements, 16);
        bindGate(index, hwElements, 24);
    }
    
    private void bindNotes(final int index, final SeqArpHardwareElements hwElements) {
        final BitwigArpDevice arpDevice = focusDevice.getArpDevice();
        final RingEncoder noteEncoder = hwElements.getEncoder(index);
        
        noteEncoder.bindNoteValue(this, arpDevice.getNoteValues(index), arpDevice.getNoteParam(index),
            focusDevice.getFocusDevice().exists());
        noteEncoder.getButton().bindLight(this, () -> getStepState(index));
        noteEncoder.getButton().bindPressed(this, () -> focusDevice.toggleSkip(index));
    }
    
    private void bindOffset(final int index, final SeqArpHardwareElements hwElements, final int buttonOffset) {
        final BitwigArpDevice arpDevice = focusDevice.getArpDevice();
        final RingEncoder offsetEncoder = hwElements.getEncoder(index + buttonOffset);
        offsetEncoder.bindOffsetValue(this, arpDevice.getNoteValues(index), focusDevice.getFocusDevice().exists());
        offsetEncoder.bindLight(this, () -> YaeltexButtonLedState.ORANGE);
        offsetEncoder.getButton().bindPressed(this, () -> arpDevice.getNoteValues(index).setOffsetValue(0));
        offsetEncoder.getButton().bindLight(this, () -> getOffsetState(index));
    }
    
    private void bindVelocity(final int index, final SeqArpHardwareElements hwElements, final int buttonOffset) {
        final BitwigArpDevice arpDevice = focusDevice.getArpDevice();
        final RingEncoder velocityEncoder = hwElements.getEncoder(index + buttonOffset);
        velocityEncoder.bind(this, arpDevice.getVelocity(index));
        velocityEncoder.bindLight(this, () -> YaeltexButtonLedState.AQUA);
        velocityEncoder.getButton().bindLight(this, () -> getVelocityState(index));
        velocityEncoder.getButton().bindPressed(this, () -> focusDevice.toggleVelocity(index));
    }
    
    private void bindGate(final int index, final SeqArpHardwareElements hwElements, final int buttonOffset) {
        final BitwigArpDevice arpDevice = focusDevice.getArpDevice();
        final RingEncoder gateEncoder = hwElements.getEncoder(index + buttonOffset);
        gateEncoder.bind(this, arpDevice.getGate(index));
        gateEncoder.bindLight(this, () -> YaeltexButtonLedState.YELLOW);
        gateEncoder.getButton().bindPressed(this, () -> focusDevice.toggleGate(index));
        gateEncoder.getButton().bindLight(this, () -> getGateState(index));
    }
    
    private YaeltexButtonLedState getOffsetState(final int index) {
        if (focusDevice.isPresent() && focusDevice.getArpDevice().isInStepRange(index)) {
            return YaeltexButtonLedState.ORANGE;
        }
        return YaeltexButtonLedState.OFF;
    }
    
    private YaeltexButtonLedState getStepState(final int index) {
        if (focusDevice.isPresent() && focusDevice.getArpDevice().isInStepRange(index)) {
            if (focusDevice.getArpDevice().isPlayingStep(index)) {
                return YaeltexButtonLedState.GREEN;
            } else if (focusDevice.getArpDevice().isStepSkip(index)) {
                return YaeltexButtonLedState.RED;
            } else {
                return YaeltexButtonLedState.YELLOW_DIM;
            }
        }
        return YaeltexButtonLedState.OFF;
    }
    
    private YaeltexButtonLedState getGateState(final int index) {
        if (focusDevice.isPresent() && focusDevice.getArpDevice().isInStepRange(index)) {
            if (focusDevice.isStepMuted(index)) {
                return YaeltexButtonLedState.WHITE;
            }
            return YaeltexButtonLedState.YELLOW;
        }
        return YaeltexButtonLedState.OFF;
    }
    
    private YaeltexButtonLedState getVelocityState(final int index) {
        if (focusDevice.isPresent() && focusDevice.getArpDevice().isInStepRange(index)) {
            if (focusDevice.isVelocityMuted(index)) {
                return YaeltexButtonLedState.AQUA;
            }
            return YaeltexButtonLedState.BLUE;
        }
        return YaeltexButtonLedState.OFF;
    }
    
}
