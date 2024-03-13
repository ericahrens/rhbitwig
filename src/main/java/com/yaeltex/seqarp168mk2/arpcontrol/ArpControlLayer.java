package com.yaeltex.seqarp168mk2.arpcontrol;

import java.util.HashMap;
import java.util.Map;

import com.bitwig.extensions.framework.Layer;
import com.bitwig.extensions.framework.Layers;
import com.bitwig.extensions.framework.di.Component;
import com.bitwig.extensions.framework.values.ValueObject;
import com.yaeltex.common.YaeltexButtonLedState;
import com.yaeltex.controls.RgbButton;
import com.yaeltex.seqarp168mk2.BitwigViewControl;
import com.yaeltex.seqarp168mk2.SeqArpHardwareElements;
import com.yaeltex.seqarp168mk2.device.DeviceSlotState;
import com.yaeltex.seqarp168mk2.device.FocusDevice;

@Component
public class ArpControlLayer extends Layer {
    
    private final BitwigViewControl viewControl;
    private final Encoder8StepConfig currentEncoderMode;
    private final ButtonDeviceConfiguration buttonDeviceConfiguration;
    
    private final Map<ControlMode, Encoder8StepConfig> encoderConfigMap = new HashMap<>();
    private final ControlMode controlMode = ControlMode.MODE8x1;
    
    private final ValueObject<FocusDeviceMode> deviceFocus = new ValueObject<>(FocusDeviceMode.DEVICE_1);
    
    public ArpControlLayer(final Layers layers, final SeqArpHardwareElements hwElements,
        final BitwigViewControl viewControl) {
        super(layers, "APR_LAYER");
        this.viewControl = viewControl;
        
        bindGlobalButtons(hwElements);
        
        encoderConfigMap.put(ControlMode.MODE8x1, new Encoder8StepConfig(layers, hwElements, viewControl, deviceFocus));
        this.buttonDeviceConfiguration = new ButtonDeviceConfiguration(layers, hwElements, viewControl, deviceFocus);
        currentEncoderMode = encoderConfigMap.get(controlMode);
    }
    
    private void bindGlobalButtons(final SeqArpHardwareElements hwElements) {
        final FocusDevice arp1 = viewControl.getArpDevice1();
        final RgbButton slot1Button = hwElements.getStepButton(7);
        slot1Button.bindLight(this, () -> slotState(arp1.getSlotState()));
        slot1Button.bindPressed(this, () -> arp1.toggleLock());
        
        final RgbButton slot2Button = hwElements.getStepButton(8);
        final FocusDevice arp2 = viewControl.getArpDevice2();
        slot2Button.bindLight(this, () -> slotState(arp2.getSlotState()));
        slot2Button.bindPressed(this, () -> arp2.toggleLock());
        
        final RgbButton focusButton = hwElements.getStepButton(15);
        focusButton.bindLight(this, () -> this.focusState());
        focusButton.bindPressed(this, this::toggleDeviceFocus);
        
        bindModeButton(hwElements.getStepButton(3), ControlMode.MODE8x1);
        bindModeButton(hwElements.getStepButton(4), ControlMode.MODE8x2);
        bindModeButton(hwElements.getStepButton(5), ControlMode.MODE16x1);
        bindModeButton(hwElements.getStepButton(6), ControlMode.MODE16x2);
    }
    
    private void bindModeButton(final RgbButton button, final ControlMode mode) {
        button.bindPressed(this, () -> selectMode(mode));
        button.bindLight(this, () -> controlMode == mode ? YaeltexButtonLedState.BLUE : YaeltexButtonLedState.WHITE);
    }
    
    private void selectMode(final ControlMode mode) {
    }
    
    private void toggleDeviceFocus() {
        if (deviceFocus.get() == FocusDeviceMode.DEVICE_1) {
            deviceFocus.set(FocusDeviceMode.DEVICE_2);
        } else if (deviceFocus.get() == FocusDeviceMode.DEVICE_2) {
            deviceFocus.set(FocusDeviceMode.DEVICE_1);
        }
    }
    
    private YaeltexButtonLedState slotState(final DeviceSlotState state) {
        return switch (state) {
            case EMPTY -> YaeltexButtonLedState.OFF;
            case PRESENT -> YaeltexButtonLedState.GREEN;
            case LOCKED -> YaeltexButtonLedState.RED;
        };
    }
    
    private YaeltexButtonLedState focusState() {
        return switch (deviceFocus.get()) {
            case DEVICE_1 -> YaeltexButtonLedState.AQUA;
            case DEVICE_2 -> YaeltexButtonLedState.RED;
            case NONE -> YaeltexButtonLedState.OFF;
        };
    }
    
    @Override
    protected void onActivate() {
        super.onActivate();
        currentEncoderMode.setIsActive(true);
        buttonDeviceConfiguration.setIsActive(true);
    }
    
    @Override
    protected void onDeactivate() {
        super.onDeactivate();
        currentEncoderMode.setIsActive(false);
        buttonDeviceConfiguration.setIsActive(false);
    }
}
