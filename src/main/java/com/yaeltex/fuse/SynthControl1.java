package com.yaeltex.fuse;

import com.bitwig.extension.controller.api.AbsoluteHardwareKnob;
import com.bitwig.extension.controller.api.HardwareSlider;
import com.yaeltex.controls.RgbButton;

public record SynthControl1(int index, HardwareSlider cutoff, HardwareSlider resonance, HardwareSlider mod,
                            HardwareSlider amount, AbsoluteHardwareKnob[] adsrKnobs, RgbButton[] buttons) {
    
 }
