package com.yaeltex.common.controls;

import java.util.function.Function;
import java.util.function.Supplier;

import com.bitwig.extension.api.Color;
import com.bitwig.extension.controller.api.HardwareSurface;
import com.bitwig.extension.controller.api.InternalHardwareLightState;
import com.bitwig.extension.controller.api.MultiStateHardwareLight;
import com.bitwig.extension.controller.api.Parameter;
import com.bitwig.extensions.framework.Layer;
import com.yaeltex.common.YaeltexButtonLedState;
import com.yaeltex.common.YaeltexMidiProcessor;
import com.yaeltex.djm.DjmControllerExtension;

public class RgbButton extends AbstractYaeltexButton {
    
    protected MultiStateHardwareLight light;
    
    public RgbButton(final int midiId, final String name, final HardwareSurface surface,
        final YaeltexMidiProcessor midiProcessor) {
        this(0, 0, midiId, name, surface, midiProcessor);
    }
    
    public RgbButton(final int port, final int midiId, final String name, final HardwareSurface surface,
        final YaeltexMidiProcessor midiProcessor) {
        this(port, 0, midiId, name, surface, midiProcessor);
    }
    
    public RgbButton(final int port, final int channel, final int midiId, final String name,
        final HardwareSurface surface, final YaeltexMidiProcessor midiProcessor) {
        super(port, channel, midiId, name, surface, midiProcessor);
        light = surface.createMultiStateHardwareLight(name + "_LIGHT_" + midiId);
        hwButton.setBackgroundLight(light);
        light.state().setValue(YaeltexButtonLedState.OFF);
        light.state().onUpdateHardware(this::updateState);
        light.setColorToStateFunction(this::handleStateToColor);
    }
    
    private InternalHardwareLightState handleStateToColor(final Color color) {
        DjmControllerExtension.println("HST => %d %d %d", color.getRed255(), color.getGreen255(), color.getBlue255());
        return YaeltexButtonLedState.OFF;
    }
    
    private void updateState(final InternalHardwareLightState internalHardwareLightState) {
        //        if (midiPort == 0 && midiId < 4) {
        //            DjmControllerExtension.println(" UD %d %s", midiId, internalHardwareLightState);
        //        }
        if (internalHardwareLightState instanceof final YaeltexButtonLedState state) {
            midiProcessor.sendNoteColor(midiPort, channel, midiId, state);
        } else {
            midiProcessor.sendColorOff(midiPort, channel, midiId);
        }
    }
    
    public void refresh() {
        updateState(light.state().currentValue());
    }
    
    public void bindLight(final Layer layer, final Supplier<InternalHardwareLightState> supplier) {
        layer.bindLightState(supplier, light);
    }
    
    public void bindLightPressed(final Layer layer, final Function<Boolean, InternalHardwareLightState> supplier) {
        layer.bindLightState(() -> supplier.apply(hwButton.isPressed().get()), light);
    }
    
    public void bindLight(final Layer layer, final Function<Boolean, InternalHardwareLightState> pressedCombine) {
        layer.bindLightState(() -> pressedCombine.apply(hwButton.isPressed().get()), light);
    }
    
    public void bindToggleValue(final Layer layer, final Parameter parameter, final YaeltexButtonLedState color) {
        bindToggleValue(layer, parameter);
        layer.bindLightState(() -> parameter.value().get() == 0 ? YaeltexButtonLedState.OFF : color, light);
    }
    
    public void bindToggleValueDimmed(final Layer layer, final Parameter parameter, final YaeltexButtonLedState color) {
        bindToggleValue(layer, parameter);
        final YaeltexButtonLedState dimmed = color.intensity(1);
        layer.bindLightState(() -> parameter.value().get() == 0 ? dimmed : color, light);
    }
    
    public void bindToggleValue(final Layer layer, final Parameter parameter, final YaeltexButtonLedState onColor,
        final YaeltexButtonLedState offColor) {
        bindToggleValue(layer, parameter);
        layer.bindLightState(() -> parameter.value().get() == 0 ? offColor : onColor, light);
    }
    
    public void bindLightPressed(final Layer layer, final InternalHardwareLightState state,
        final InternalHardwareLightState holdState) {
        layer.bindLightState(() -> hwButton.isPressed().get() ? holdState : state, light);
    }
    
    public void bindMomentaryValue(final Layer layer, final Parameter parameter, final YaeltexButtonLedState color) {
        bindMomentaryValue(layer, parameter);
        layer.bindLightState(() -> parameter.value().get() == 0 ? YaeltexButtonLedState.OFF : color, light);
    }
    
    public void bindToggleValue(final Layer layer, final Parameter parameter) {
        parameter.value().markInterested();
        layer.bindPressed(
            hwButton, () -> {
                if (parameter.value().get() < 1) {
                    parameter.value().set(1);
                } else {
                    parameter.value().set(0);
                }
            });
    }
    
    
    public void setBounds(final double xMm, final double yMm, final double widthMm, final double heightMm) {
        hwButton.setBounds(xMm, yMm, widthMm, heightMm);
        light.setBounds(xMm + 1, yMm + 1, widthMm - 2, heightMm - 2);
    }
    
    
}
