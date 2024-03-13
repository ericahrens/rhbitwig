package com.yaeltex.common;

import com.bitwig.extension.controller.api.HardwareLightVisualState;
import com.bitwig.extension.controller.api.InternalHardwareLightState;

public class YaeltexIntensityColorState extends InternalHardwareLightState {
    
    private final int colorCode;
    private final int intensity;
    
    public YaeltexIntensityColorState(final int colorCode, final int intensity) {
        this.colorCode = colorCode;
        this.intensity = intensity;
    }
    
    public YaeltexIntensityColorState(final YaeltexButtonLedState state, final int intensity) {
        this.colorCode = state.getColorCode();
        this.intensity = intensity;
    }
    
    public int getColorCode() {
        return colorCode;
    }
    
    public int getIntensity() {
        return intensity;
    }
    
    @Override
    public HardwareLightVisualState getVisualState() {
        return null;
    }
    
    @Override
    public boolean equals(final Object obj) {
        return obj instanceof YaeltexIntensityColorState color && equals(color);
    }
    
    public boolean equals(final YaeltexIntensityColorState obj) {
        if (obj == this) {
            return true;
        }
        
        return colorCode == obj.colorCode && intensity == obj.intensity;
    }
    
    
}
