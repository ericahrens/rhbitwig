package com.yaeltex.seqarp168mk2.arpcontrol;

import com.bitwig.extensions.framework.values.ValueObject;

public abstract class DeviceConfig {
    
    protected final ValueObject<FocusDeviceMode> deviceFocus;
    protected boolean active;
    
    public DeviceConfig(final ValueObject<FocusDeviceMode> deviceFocus) {
        this.deviceFocus = deviceFocus;
    }
    
    public abstract void setIsActive(final boolean active);
    
    public boolean isActive() {
        return active;
    }
}
