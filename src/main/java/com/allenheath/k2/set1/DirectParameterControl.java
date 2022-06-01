package com.allenheath.k2.set1;

import com.bitwig.extension.controller.api.CursorDevice;
import com.bitwig.extension.controller.api.Device;
import com.bitwig.extension.controller.api.Parameter;
import com.bitwig.extension.controller.api.SpecificPluginDevice;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DirectParameterControl {
    private final SpecialDevice deviceType;
    private final List<SpecialParam> paramIds = new ArrayList<>();
    private final Map<SpecialParam, Parameter> currentParameter = new HashMap<>();

    public DirectParameterControl(final SpecialDevice device, final SpecialParam... paramIds) {
        deviceType = device;
        for (final SpecialParam paramId : paramIds) {
            if (paramId.getDeviceType() != deviceType) {
                throw new IllegalArgumentException("Parameter has to be of matching overall type " + device);
            }
            this.paramIds.add(paramId);
        }
    }

    public void register(final Device followDevice, final CursorDevice cursorDevice) {
        final SpecificPluginDevice specDevice = getDeviceType().createDevice(cursorDevice);
        for (final SpecialParam paramId : paramIds) {
            register(specDevice, paramId);
        }
        followDevice.exists().addValueObserver(exists -> {
            if (exists) {
                cursorDevice.selectDevice(followDevice);
            }
        });
    }

    private void register(final SpecificPluginDevice specDevice, final SpecialParam paramId) {
        final Parameter param = specDevice.createParameter(paramId.getParamId());
        param.value().markInterested();
        param.exists().addValueObserver(exists -> {
            if (exists) {
                currentParameter.put(paramId, param);
            }
        });
    }

    public RedGreenButtonState getState(final SpecialParam paramId) {
        final Parameter param = currentParameter.get(paramId);
        if (param != null) {
            final double value = param.get();
            if (value >= 1.0) {
                return RedGreenButtonState.GREEN;
            }
            if (param.get() > 0) {
                return RedGreenButtonState.YELLOW;
            }
        }
        return RedGreenButtonState.OFF;
    }

    public void toggle(final SpecialParam paramId) {
        final Parameter param = currentParameter.get(paramId);
        if (param != null) {
            final double value = param.get();
            if (value > 0) {
                param.setImmediately(0);
            } else {
                param.setImmediately(1);
            }
        }
    }

    public SpecialDevice getDeviceType() {
        return deviceType;
    }

    @Override
    public String toString() {
        return deviceType.toString();
    }

}
