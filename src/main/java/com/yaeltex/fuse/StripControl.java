package com.yaeltex.fuse;

import java.util.List;

import com.bitwig.extension.controller.api.AbsoluteHardwareKnob;
import com.bitwig.extension.controller.api.HardwareSlider;
import com.yaeltex.controls.RgbButton;

public record StripControl(HardwareSlider mainFader, RgbButton abButton, RgbButton cueButton, RgbButton fxButton,
                           RgbButton MuteButton, List<AbsoluteHardwareKnob> fxKnobs, AbsoluteHardwareKnob plusKnob,
                           AbsoluteHardwareKnob multKnob) {
}
