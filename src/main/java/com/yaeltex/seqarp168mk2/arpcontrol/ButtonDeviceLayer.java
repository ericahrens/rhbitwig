package com.yaeltex.seqarp168mk2.arpcontrol;

import com.bitwig.extension.controller.api.Parameter;
import com.bitwig.extension.controller.api.SettableRangedValue;
import com.bitwig.extensions.framework.Layer;
import com.bitwig.extensions.framework.Layers;
import com.yaeltex.common.YaeltexButtonLedState;
import com.yaeltex.controls.RgbButton;
import com.yaeltex.seqarp168mk2.SeqArpHardwareElements;
import com.yaeltex.seqarp168mk2.device.FocusDevice;

public class ButtonDeviceLayer extends Layer {
    
    private final FocusDevice focusDevice;
    
    public ButtonDeviceLayer(final Layers layers, final String name, final FocusDevice focusDevice) {
        super(layers, name);
        this.focusDevice = focusDevice;
    }
    
    void bindButtonValues(final SeqArpHardwareElements hwElements) {
        bindValues(hwElements, 0, 3, focusDevice.getArpDevice().getRateModeParam(), YaeltexButtonLedState.ORANGE);
        bindValues(hwElements, 16, 7, focusDevice.getArpDevice().getRateParam(), YaeltexButtonLedState.ORANGE);
        bindNoteValuesTop(this, hwElements);
        bindNoteValuesBottom(this, hwElements);
    }
    
    void bindNoteValuesTop(final Layer layer, final SeqArpHardwareElements hwElements) {
        bindQuantizeValues(layer, hwElements.getStepButton(9), 1);
        bindQuantizeValues(layer, hwElements.getStepButton(10), 3);
        bindQuantizeValues(layer, hwElements.getStepButton(12), 6);
        bindQuantizeValues(layer, hwElements.getStepButton(13), 8);
        bindQuantizeValues(layer, hwElements.getStepButton(14), 10);
    }
    
    void bindNoteValuesBottom(final Layer layer, final SeqArpHardwareElements hwElements) {
        bindQuantizeValues(layer, hwElements.getStepButton(24), 0);
        bindQuantizeValues(layer, hwElements.getStepButton(25), 2);
        bindQuantizeValues(layer, hwElements.getStepButton(26), 4);
        bindQuantizeValues(layer, hwElements.getStepButton(27), 5);
        bindQuantizeValues(layer, hwElements.getStepButton(28), 7);
        bindQuantizeValues(layer, hwElements.getStepButton(29), 9);
        bindQuantizeValues(layer, hwElements.getStepButton(30), 11);
        bindQuantizeValues(layer, hwElements.getStepButton(31), 0);
    }
    
    private void bindQuantizeValues(final Layer layer, final RgbButton button, final int noteIndex) {
        button.bindPressed(layer, () -> focusDevice.toggleNoteQuantize(noteIndex));
        button.bindLight(layer, () -> quantizeNote(noteIndex));
    }
    
    private void bindValues(final SeqArpHardwareElements hwElements, final int startIndex, final int values,
        final Parameter parameter, final YaeltexButtonLedState onState) {
        
        for (int i = 0; i < values; i++) {
            final double value = (double) i / (double) (values - 1);
            final RgbButton button = hwElements.getStepButton(i + startIndex);
            button.bindPressed(this, () -> parameter.value().set(value));
            button.bindLight(this, () -> lightByValue(parameter.value(), value, YaeltexButtonLedState.ORANGE));
        }
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
    
}
