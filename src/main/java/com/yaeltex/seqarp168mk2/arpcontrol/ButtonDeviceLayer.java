package com.yaeltex.seqarp168mk2.arpcontrol;

import com.bitwig.extension.controller.api.Parameter;
import com.bitwig.extension.controller.api.SettableRangedValue;
import com.bitwig.extensions.framework.Layer;
import com.bitwig.extensions.framework.Layers;
import com.yaeltex.common.YaelTexColors;
import com.yaeltex.common.YaeltexButtonLedState;
import com.yaeltex.common.controls.RgbButton;
import com.yaeltex.seqarp168mk2.SeqArpHardwareElements;
import com.yaeltex.seqarp168mk2.device.FocusDevice;

public class ButtonDeviceLayer extends Layer {
    
    private final FocusDevice focusDevice;
    
    
    @FunctionalInterface
    private interface IndexToValue {
        double accept(int index);
    }
    
    public ButtonDeviceLayer(final Layers layers, final String name, final FocusDevice focusDevice) {
        super(layers, name);
        this.focusDevice = focusDevice;
    }
    
    void bindDefaultValues(final SeqArpHardwareElements hwElements) {
        bindValues(hwElements, 0, 3, focusDevice.getArpDevice().getRateModeParam(), YaeltexButtonLedState.ORANGE);
        bindValues(hwElements, 16, 7, focusDevice.getArpDevice().getRateParam(), YaeltexButtonLedState.ORANGE);
        bindToggleButton(hwElements.getStepButton(23), focusDevice.getArpDevice().getShuffleParam(),
            YaeltexButtonLedState.RED, YaeltexButtonLedState.OFF);
        bindNoteValuesTop(this, hwElements);
        bindNoteValuesBottom(this, hwElements);
    }
    
    private void bindToggleButton(final RgbButton shuffleButton, final Parameter shuffleParam,
        final YaeltexButtonLedState red, final YaeltexButtonLedState off) {
        shuffleButton.bindToggleValue(this, shuffleParam);
        shuffleButton.bindLight(this, () -> getLightValue(shuffleParam, red, off));
    }
    
    public void bindRetrigger(final SeqArpHardwareElements hwElements) {
        bindToggleButton(hwElements.getControlButton(2), focusDevice.getArpDevice().getRetriggerParam(),
            YaeltexButtonLedState.DEEP_GREEN, YaeltexButtonLedState.WHITE);
    }
    
    public void bindTimeWarp(final SeqArpHardwareElements hwElements) {
        bindValues(hwElements, 16, 7, focusDevice.getArpDevice().getRateParam(), YaeltexButtonLedState.ORANGE);
        bindToggleButton(hwElements.getStepButton(23), focusDevice.getArpDevice().getShuffleParam(),
            YaeltexButtonLedState.RED, YaeltexButtonLedState.OFF);
    }
    
    private YaeltexButtonLedState getLightValue(final Parameter parameter, final YaeltexButtonLedState on,
        final YaeltexButtonLedState off) {
        if (focusDevice.isPresent()) {
            return parameter.value().get() == 0.0 ? off : on;
        }
        return YaeltexButtonLedState.OFF;
    }
    
    public void bindOptValues(final SeqArpHardwareElements hwElements) {
        bindValues(hwElements, 0, 4, focusDevice.getArpDevice().getOctaveParam(),
            YaeltexButtonLedState.of(YaelTexColors.ELECTRIC_PURPLE));
        bindNoteValuesTop(this, hwElements);
        bindValues(hwElements, 16, 16, focusDevice.getArpDevice().getModeParam(),
            YaeltexButtonLedState.of(YaelTexColors.DEEP_SKY_BLUE), 1);
    }
    
    public void bindStepsAndGateGlobal(final SeqArpHardwareElements hwElements) {
        bindValue(hwElements, focusDevice.getArpDevice().getStepLength(), 0, YaeltexButtonLedState.GREEN);
        bindValue(hwElements, focusDevice.getArpDevice().getGlobalGateParam(), 1, YaeltexButtonLedState.YELLOW,
            index -> index == 15 ? 1.0 : (double) index / 16.0);
    }
    
    public void bindStepsAndVelocityGlobal(final SeqArpHardwareElements hwElements) {
        bindValue(hwElements, focusDevice.getArpDevice().getStepLength(), 0, YaeltexButtonLedState.GREEN);
        bindValue(hwElements, focusDevice.getArpDevice().getGlobalVelParam(), 1, YaeltexButtonLedState.AQUA);
    }
    
    
    public void bindStepsMute(final SeqArpHardwareElements hwElements) {
        bindValue(hwElements, focusDevice.getArpDevice().getStepLength(), 0, YaeltexButtonLedState.GREEN);
        bindMutes(hwElements, 1);
    }
    
    public void bindVelMute(final SeqArpHardwareElements hwElements) {
        bindMutes(hwElements, 0);
        bindVelMutes(hwElements, 1);
    }
    
    private void bindMutes(final SeqArpHardwareElements hwElements, final int row) {
        for (int i = 0; i < 16; i++) {
            final int index = i;
            final RgbButton button = hwElements.getStepButton(i + row * 16);
            button.bindPressed(this, () -> focusDevice.toggleGate(index));
            button.bindLight(this, () -> getGateState(index));
        }
    }
    
    private void bindVelMutes(final SeqArpHardwareElements hwElements, final int row) {
        for (int i = 0; i < 16; i++) {
            final int index = i;
            final RgbButton button = hwElements.getStepButton(i + row * 16);
            button.bindPressed(this, () -> focusDevice.toggleVelocity(index));
            button.bindLight(this, () -> getVelState(index));
        }
    }
    
    
    void bindNoteValuesTop(final Layer layer, final SeqArpHardwareElements hwElements) {
        bindQuantizeValues(hwElements.getStepButton(9), 1);
        bindQuantizeValues(hwElements.getStepButton(10), 3);
        bindQuantizeValues(hwElements.getStepButton(12), 6);
        bindQuantizeValues(hwElements.getStepButton(13), 8);
        bindQuantizeValues(hwElements.getStepButton(14), 10);
    }
    
    void bindNoteValuesBottom(final Layer layer, final SeqArpHardwareElements hwElements) {
        bindQuantizeValues(hwElements.getStepButton(24), 0);
        bindQuantizeValues(hwElements.getStepButton(25), 2);
        bindQuantizeValues(hwElements.getStepButton(26), 4);
        bindQuantizeValues(hwElements.getStepButton(27), 5);
        bindQuantizeValues(hwElements.getStepButton(28), 7);
        bindQuantizeValues(hwElements.getStepButton(29), 9);
        bindQuantizeValues(hwElements.getStepButton(30), 11);
        bindQuantizeValues(hwElements.getStepButton(31), 0);
    }
    
    private void bindQuantizeValues(final RgbButton button, final int noteIndex) {
        button.bindPressed(this, () -> focusDevice.toggleNoteQuantize(noteIndex));
        button.bindLight(this, () -> quantizeNote(noteIndex));
    }
    
    private void bindValue(final SeqArpHardwareElements hwElements, final Parameter parameter, final int row,
        final YaeltexButtonLedState onState, final IndexToValue indexToValue) {
        for (int i = 0; i < 16; i++) {
            final RgbButton button = hwElements.getStepButton(i + row * 16);
            final double refValue = indexToValue.accept(i);
            button.bindPressed(this, () -> parameter.value().set(refValue));
            button.bindLight(this, () -> lightByValueRange(parameter.value(), refValue, onState));
        }
    }
    
    private void bindValue(final SeqArpHardwareElements hwElements, final Parameter parameter, final int row,
        final YaeltexButtonLedState onState) {
        bindValue(hwElements, parameter, row, onState, index -> (double) index / 15.0);
    }
    
    private void bindValues(final SeqArpHardwareElements hwElements, final int startIndex, final int values,
        final Parameter parameter, final YaeltexButtonLedState onState) {
        bindValues(hwElements, startIndex, values, parameter, onState, 0);
    }
    
    private void bindValues(final SeqArpHardwareElements hwElements, final int startIndex, final int values,
        final Parameter parameter, final YaeltexButtonLedState onState, final int offset) {
        
        for (int i = 0; i < values; i++) {
            final double value = (double) (i + offset) / (double) (values - 1 + offset);
            final RgbButton button = hwElements.getStepButton(i + startIndex);
            button.bindPressed(this, () -> parameter.value().set(value));
            button.bindLight(this, () -> lightByValue(parameter.value(), value, onState));
        }
    }
    
    private YaeltexButtonLedState lightByValueRange(final SettableRangedValue value, final double compareValue,
        final YaeltexButtonLedState color) {
        if (focusDevice.isPresent()) {
            return compareValue <= value.get() ? color : YaeltexButtonLedState.OFF;
        }
        return YaeltexButtonLedState.OFF;
    }
    
    private YaeltexButtonLedState lightByValue(final SettableRangedValue value, final double compareValue,
        final YaeltexButtonLedState color) {
        if (focusDevice.isPresent()) {
            return value.get() == compareValue ? color : YaeltexButtonLedState.OFF;
        }
        return YaeltexButtonLedState.OFF;
    }
    
    private YaeltexButtonLedState quantizeNote(final int noteIndex) {
        if (focusDevice.isPresent()) {
            return focusDevice.isQuantizeNoteSet(noteIndex)
                ? YaeltexButtonLedState.GREEN
                : YaeltexButtonLedState.GREEN_DIM;
        }
        return YaeltexButtonLedState.OFF;
    }
    
    private YaeltexButtonLedState getGateState(final int index) {
        if (focusDevice.isPresent()) { // && focusDevice.getArpDevice().isInStepRange(index)
            if (focusDevice.isStepMuted(index)) {
                return YaeltexButtonLedState.WHITE;
            }
            return YaeltexButtonLedState.YELLOW;
        }
        return YaeltexButtonLedState.OFF;
    }
    
    private YaeltexButtonLedState getVelState(final int index) {
        if (focusDevice.isPresent()) {
            if (focusDevice.isVelocityMuted(index)) {
                return YaeltexButtonLedState.WHITE;
            }
            return YaeltexButtonLedState.BLUE;
        }
        return YaeltexButtonLedState.OFF;
    }
    
}
