package com.yaeltex.fuse;

import com.bitwig.extension.controller.api.AbsoluteHardwareKnob;
import com.bitwig.extension.controller.api.HardwareSlider;
import com.yaeltex.controls.RgbButton;

public record SynthControl2(int index, AbsoluteHardwareKnob cutoff, AbsoluteHardwareKnob resonance, AbsoluteHardwareKnob mod,
                            AbsoluteHardwareKnob amount, AbsoluteHardwareKnob[] adsrKnobs) {
    
    public void layout() {
    
    }
}
