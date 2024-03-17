package com.yaeltex.common.devices;

import java.util.Arrays;
import java.util.HashMap;

import com.bitwig.extension.controller.api.Device;

public class DeviceState {
    private final HashMap<ParameterType, Double> state = new HashMap<>();
    private final Device device;
    private final DirectDevice deviceDefinition;
    
    public DeviceState(final DirectDevice deviceDefinition, final Device device) {
        this.device = device;
        this.deviceDefinition = deviceDefinition;
        Arrays.stream(ParameterType.values()).forEach(param -> state.put(param, 0.0));
    }
    
    public void applyValue(final ParameterType type, final double v) {
        final String name = deviceDefinition.getParamName(type);
        if (name != null) {
            device.setDirectParameterValueNormalized(name, v, 1.0);
        }
    }
    
    public void updateValue(final ParameterType parameterType, final double value) {
        state.put(parameterType, value);
    }
}
