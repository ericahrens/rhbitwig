package com.yaeltex.fuse;

import java.util.List;

import com.bitwig.extension.controller.api.AbsoluteHardwareKnob;
import com.bitwig.extension.controller.api.HardwareSlider;
import com.bitwig.extension.controller.api.RelativePosition;
import com.yaeltex.common.controls.RgbButton;

public record StripControl(int index, HardwareSlider mainFader, RgbButton abButton, RgbButton cueButton,
                           RgbButton fxButton, RgbButton muteButton, List<AbsoluteHardwareKnob> fxKnobs,
                           AbsoluteHardwareKnob plusKnob, AbsoluteHardwareKnob multKnob) {
    
    public void applySurface() {
        final double width = 30;
        final double knobSize = 14;
        final double buttonSize = 10;
        final double leftBound = index * width + 1.0;
        mainFader.setBounds(index * width + 0.2, 190, width * 0.9, 130);
        for (int i = 0; i < 4; i++) {
            final AbsoluteHardwareKnob knob = fxKnobs.get(i);
            knob.setBounds(leftBound + 5, 70 + knobSize * 1.5 * i, knobSize, knobSize);
            knob.setLabel("FX%d".formatted(i + 1));
            knob.setLabelPosition(RelativePosition.ABOVE);
        }
        final double buttonY = 155;
        final double factor = 1.4;
        abButton.setBounds(leftBound, buttonY, buttonSize, buttonSize);
        cueButton.setBounds(leftBound + buttonSize * factor, buttonY, buttonSize, buttonSize);
        fxButton.setBounds(leftBound, buttonY + buttonSize * factor, buttonSize, buttonSize);
        muteButton.setBounds(leftBound + buttonSize * factor, buttonY + buttonSize * factor, buttonSize, buttonSize);
        plusKnob.setBounds(leftBound, 43, knobSize - 1, knobSize - 1);
        plusKnob.setLabel("#");
        plusKnob.setLabelPosition(RelativePosition.ABOVE);
        multKnob.setBounds(leftBound + knobSize + 1, 43, knobSize - 1, knobSize - 1);
        multKnob.setLabel("*");
        multKnob.setLabelPosition(RelativePosition.ABOVE);
    }
}
