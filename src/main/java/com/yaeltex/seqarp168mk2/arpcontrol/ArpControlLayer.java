package com.yaeltex.seqarp168mk2.arpcontrol;

import java.util.HashMap;
import java.util.Map;

import com.bitwig.extensions.framework.Layer;
import com.bitwig.extensions.framework.Layers;
import com.bitwig.extensions.framework.di.Component;
import com.bitwig.extensions.framework.values.ValueObject;
import com.yaeltex.common.YaeltexButtonLedState;
import com.yaeltex.common.controls.RgbButton;
import com.yaeltex.seqarp168mk2.BitwigViewControl;
import com.yaeltex.seqarp168mk2.SeqArpHardwareElements;
import com.yaeltex.seqarp168mk2.device.DeviceSlotState;
import com.yaeltex.seqarp168mk2.device.FocusDevice;

@Component
public class ArpControlLayer extends Layer {
    // TODO 1 Retrigger consider both layers
    // TODO 2 Time Warp is momentary RATE bothLayers
    
    private final BitwigViewControl viewControl;
    private DeviceConfig currentEncoderMode;
    private final ButtonDeviceConfiguration defaultButtonConfiguration;
    private final ButtonDeviceConfiguration optConfiguration;
    private final ButtonDeviceConfiguration stepsGateGlobalConfiguration;
    private final ButtonDeviceConfiguration stepsVelocityGlobalConfiguration;
    private final ButtonDeviceConfiguration stepMuteConfiguration;
    private final ButtonDeviceConfiguration velMuteConfiguration;
    private final ButtonDeviceConfiguration retrigConfiguration;
    private final ButtonDeviceConfiguration timeWarpConfiguration;
    
    private final Map<ControlMode, DeviceConfig> encoderConfigMap = new HashMap<>();
    private ArpButtonMode buttonMode = ArpButtonMode.DEFAULT;
    private ButtonDeviceConfiguration buttonConfig = null;
    
    private final ValueObject<FocusDeviceMode> deviceFocus = new ValueObject<>(FocusDeviceMode.DEVICE_1);
    
    public ArpControlLayer(final Layers layers, final SeqArpHardwareElements hwElements,
        final BitwigViewControl viewControl) {
        super(layers, "APR_LAYER");
        this.viewControl = viewControl;
        
        bindGlobalButtons(hwElements);
        
        encoderConfigMap.put(ControlMode.MODE8x1, new Encoder8StepConfig(layers, hwElements, viewControl, deviceFocus));
        encoderConfigMap.put(ControlMode.MODE8x2,
            new Encoder2x8StepConfig(layers, hwElements, viewControl, deviceFocus));
        encoderConfigMap.put(ControlMode.MODE16x1,
            new Encoder16StepConfig(layers, hwElements, viewControl, deviceFocus));
        encoderConfigMap.put(ControlMode.MODE16x2,
            new Encoder2x16StepConfig(layers, hwElements, viewControl, deviceFocus));
        
        this.defaultButtonConfiguration =
            new ButtonDeviceConfiguration(layers, "DEFAULT_ARP", viewControl, deviceFocus);
        this.defaultButtonConfiguration.bind(layer -> layer.bindDefaultValues(hwElements));
        
        this.optConfiguration = new ButtonDeviceConfiguration(layers, "OPT_ARP", viewControl, deviceFocus);
        this.optConfiguration.bind(layer -> layer.bindOptValues(hwElements));
        
        this.stepsGateGlobalConfiguration =
            new ButtonDeviceConfiguration(layers, "STEP_GATE_GLOB_ARP", viewControl, deviceFocus);
        this.stepsGateGlobalConfiguration.bind(layer -> layer.bindStepsAndGateGlobal(hwElements));
        
        stepsVelocityGlobalConfiguration =
            new ButtonDeviceConfiguration(layers, "STEP_VEL_GLOB_ARP", viewControl, deviceFocus);
        this.stepsVelocityGlobalConfiguration.bind(layer -> layer.bindStepsAndVelocityGlobal(hwElements));
        
        stepMuteConfiguration = new ButtonDeviceConfiguration(layers, "STEP_MUTE_ARP", viewControl, deviceFocus);
        this.stepMuteConfiguration.bind(layer -> layer.bindStepsMute(hwElements));
        
        velMuteConfiguration = new ButtonDeviceConfiguration(layers, "VEL_MUTE_ARP", viewControl, deviceFocus);
        this.velMuteConfiguration.bind(layer -> layer.bindVelMute(hwElements));
        
        retrigConfiguration = new ButtonDeviceConfiguration(layers, "RETRIG_ARP", viewControl, deviceFocus);
        this.retrigConfiguration.bind(layer -> layer.bindRetrigger(hwElements));
        
        timeWarpConfiguration = new ButtonDeviceConfiguration(layers, "TIMEWARP_ARP", viewControl, deviceFocus);
        timeWarpConfiguration.bind(layer -> layer.bindTimeWarp(hwElements));
        
        currentEncoderMode = encoderConfigMap.get(ControlMode.MODE8x1);
        this.buttonConfig = this.defaultButtonConfiguration;
    }
    
    public void setMode(final boolean pressed, final ArpButtonMode mode) {
        if (!pressed) {
            return;
        }
        if (this.buttonMode != mode) {
            this.buttonMode = mode;
            updateButtonMode();
        }
    }
    
    public void handleOptMode() {
        if (this.buttonMode == ArpButtonMode.DEFAULT) {
            this.buttonMode = ArpButtonMode.PAT;
        } else {
            this.buttonMode = ArpButtonMode.DEFAULT;
        }
        updateButtonMode();
    }
    
    public ArpButtonMode getButtonMode() {
        return buttonMode;
    }
    
    private void updateButtonMode() {
        buttonConfig.setIsActive(false);
        switch (buttonMode) {
            case DEFAULT -> this.buttonConfig = this.defaultButtonConfiguration;
            case PAT -> this.buttonConfig = this.optConfiguration;
            case STEP_GATE_GLOBAL -> this.buttonConfig = this.stepsGateGlobalConfiguration;
            case STEP_VEL_GLOBAL -> this.buttonConfig = this.stepsVelocityGlobalConfiguration;
            case STEP_MUTE -> this.buttonConfig = this.stepMuteConfiguration;
            case VEL_MUTE -> this.buttonConfig = this.velMuteConfiguration;
        }
        buttonConfig.setIsActive(true);
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
        
        final RgbButton encoderModeButton = hwElements.getControlButton(7);
        encoderModeButton.bindLight(this, this::toEncoderModeState);
        encoderModeButton.bindIsPressed(this, this::handleEncoderModeButton);
        
        bindModeButton(hwElements.getStepButton(3), ControlMode.MODE8x1);
        bindModeButton(hwElements.getStepButton(4), ControlMode.MODE8x2);
        bindModeButton(hwElements.getStepButton(5), ControlMode.MODE16x1);
        bindModeButton(hwElements.getStepButton(6), ControlMode.MODE16x2);
    }
    
    private void handleEncoderModeButton(final boolean pressed) {
        if (currentEncoderMode != null) {
            currentEncoderMode.changeEncoderMode(pressed);
        }
    }
    
    private YaeltexButtonLedState toEncoderModeState() {
        return switch (currentEncoderMode.getStepEncoderMode()) {
            case DEFAULT -> YaeltexButtonLedState.WHITE;
            case MODE_1 -> currentEncoderMode.getMode() == ControlMode.MODE16x2
                ? YaeltexButtonLedState.YELLOW
                : YaeltexButtonLedState.BLUE;
            case MODE_2 -> YaeltexButtonLedState.ORANGE;
            case MODE_3 -> YaeltexButtonLedState.BLUE;
        };
    }
    
    private void bindModeButton(final RgbButton button, final ControlMode mode) {
        button.bindPressed(this, () -> selectMode(mode));
        button.bindLight(this,
            () -> currentEncoderMode.getMode() == mode ? YaeltexButtonLedState.BLUE : YaeltexButtonLedState.WHITE);
    }
    
    private void selectMode(final ControlMode mode) {
        if (currentEncoderMode.getMode() != mode && encoderConfigMap.containsKey(mode)) {
            currentEncoderMode.setIsActive(false);
            currentEncoderMode = encoderConfigMap.get(mode);
            currentEncoderMode.setIsActive(true);
        }
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
        buttonConfig.setIsActive(true);
        retrigConfiguration.setIsActive(true);
    }
    
    @Override
    protected void onDeactivate() {
        super.onDeactivate();
        currentEncoderMode.setIsActive(false);
        this.buttonConfig.setIsActive(false);
        retrigConfiguration.setIsActive(false);
    }
    
    public void setTimeWarpActive(final boolean active) {
        timeWarpConfiguration.setIsActive(active);
    }
}
