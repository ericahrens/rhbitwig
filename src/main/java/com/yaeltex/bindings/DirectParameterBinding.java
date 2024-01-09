package com.yaeltex.bindings;

import com.bitwig.extension.controller.api.AbsoluteHardwareControl;
import com.bitwig.extensions.framework.Binding;
import com.yaeltex.devices.DirectDeviceControl;
import com.yaeltex.devices.ParameterType;

public class DirectParameterBinding extends Binding<AbsoluteHardwareControl, ParameterType> {
    private final DirectDeviceControl device;
    
    
    public DirectParameterBinding(final AbsoluteHardwareControl source, final ParameterType target,
        DirectDeviceControl device) {
        super(source, source, target);
        this.device = device;
        source.value().addValueObserver(this::valueChange);
    }
    
    private void valueChange(double newValue) {
        //FuseExtension.println(" %d -  %s %f %s", device.getIndex(), getTarget(), newValue, isActive());
        if (isActive()) {
            device.applyValue(getTarget(), newValue);
        }
    }
    
    @Override
    protected void deactivate() {
    
    }
    
    @Override
    protected void activate() {
    
    }
    
}
