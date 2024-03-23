package com.yaeltex.seqarp168mk2.arpcontrol;

import com.bitwig.extensions.framework.Layer;
import com.bitwig.extensions.framework.Layers;
import com.yaeltex.common.YaeltexButtonLedState;
import com.yaeltex.common.controls.RingEncoder;
import com.yaeltex.seqarp168mk2.SeqArpHardwareElements;
import com.yaeltex.seqarp168mk2.device.BitwigArpDevice;
import com.yaeltex.seqarp168mk2.device.FocusDevice;

public class ArpEncoderLayer extends Layer {
    
    public ArpEncoderLayer(final Layers layers, final String name) {
        super(layers, name);
    }
    
    protected void bindNotes(final int index, final int buttonIndex, final SeqArpHardwareElements hwElements,
        final FocusDevice focusDevice) {
        final BitwigArpDevice arpDevice = focusDevice.getArpDevice();
        final RingEncoder noteEncoder = hwElements.getEncoder(buttonIndex);
        
        noteEncoder.bindNoteValue(this, arpDevice.getNoteValues(index), arpDevice.getNoteParam(index),
            focusDevice.getFocusDevice().exists());
        noteEncoder.getButton().bindLight(this, () -> getStepState(index, focusDevice));
        noteEncoder.getButton().bindPressed(this, () -> focusDevice.toggleSkip(index));
    }
    
    protected void bindOffset(final int index, final int buttonIndex, final SeqArpHardwareElements hwElements,
        final FocusDevice focusDevice) {
        final BitwigArpDevice arpDevice = focusDevice.getArpDevice();
        final RingEncoder offsetEncoder = hwElements.getEncoder(buttonIndex);
        offsetEncoder.bindOffsetValue(this, arpDevice.getNoteValues(index), focusDevice.getFocusDevice().exists());
        offsetEncoder.bindLight(this, () -> YaeltexButtonLedState.ORANGE);
        offsetEncoder.getButton().bindPressed(this, () -> arpDevice.getNoteValues(index).setOffsetValue(0));
        offsetEncoder.getButton().bindLight(this, () -> getOffsetState(index, focusDevice));
    }
    
    protected void bindVelocity(final int index, final int buttonIndex, final SeqArpHardwareElements hwElements,
        final FocusDevice focusDevice) {
        final BitwigArpDevice arpDevice = focusDevice.getArpDevice();
        final RingEncoder velocityEncoder = hwElements.getEncoder(buttonIndex);
        velocityEncoder.bind(this, arpDevice.getVelocity(index).value());
        velocityEncoder.bindLight(this, () -> YaeltexButtonLedState.AQUA);
        velocityEncoder.getButton().bindLight(this, () -> getVelocityState(index, focusDevice));
        velocityEncoder.getButton().bindPressed(this, () -> focusDevice.toggleVelocity(index));
    }
    
    protected void bindGate(final int index, final int buttonIndex, final SeqArpHardwareElements hwElements,
        final FocusDevice focusDevice) {
        final BitwigArpDevice arpDevice = focusDevice.getArpDevice();
        final RingEncoder gateEncoder = hwElements.getEncoder(buttonIndex);
        gateEncoder.bind(this, arpDevice.getGate(index).value());
        gateEncoder.bindLight(this, () -> YaeltexButtonLedState.YELLOW);
        gateEncoder.getButton().bindPressed(this, () -> focusDevice.toggleGate(index));
        gateEncoder.getButton().bindLight(this, () -> getGateState(index, focusDevice));
    }
    
    private YaeltexButtonLedState getOffsetState(final int index, final FocusDevice focusDevice) {
        if (focusDevice.isPresent() && focusDevice.getArpDevice().isInStepRange(index)) {
            return YaeltexButtonLedState.ORANGE;
        }
        return YaeltexButtonLedState.OFF;
    }
    
    private YaeltexButtonLedState getStepState(final int index, final FocusDevice focusDevice) {
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
    
    private YaeltexButtonLedState getGateState(final int index, final FocusDevice focusDevice) {
        if (focusDevice.isPresent() && focusDevice.getArpDevice().isInStepRange(index)) {
            if (focusDevice.isStepMuted(index)) {
                return YaeltexButtonLedState.WHITE;
            }
            return YaeltexButtonLedState.YELLOW;
        }
        return YaeltexButtonLedState.OFF;
    }
    
    private YaeltexButtonLedState getVelocityState(final int index, final FocusDevice focusDevice) {
        if (focusDevice.isPresent() && focusDevice.getArpDevice().isInStepRange(index)) {
            if (focusDevice.isVelocityMuted(index)) {
                return YaeltexButtonLedState.AQUA;
            }
            return YaeltexButtonLedState.BLUE;
        }
        return YaeltexButtonLedState.OFF;
    }
    
}
