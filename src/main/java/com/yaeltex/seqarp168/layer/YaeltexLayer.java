package com.yaeltex.seqarp168.layer;

import com.bitwig.extension.controller.api.InternalHardwareLightState;
import com.bitwig.extension.controller.api.Parameter;
import com.bitwig.extensions.framework.Binding;
import com.bitwig.extensions.framework.Layer;
import com.yaeltex.seqarp168.ColorButtonLedState;
import com.yaeltex.seqarp168.LayerActiveValue;
import com.yaeltex.seqarp168.RgbButton;
import com.yaeltex.seqarp168.YaeltexArpControlExtension;

import java.util.function.Supplier;

public class YaeltexLayer extends Layer {
    private final YaeltexArpControlExtension driver;
    private final LayerActiveValue active = new LayerActiveValue();

    public YaeltexLayer(final YaeltexArpControlExtension driver, final String name) {
        super(driver.getLayers(), name);
        this.driver = driver;
    }

    protected void toggleParameter(final int index, final Parameter parm) {
        if (parm.value().get() == 0) {
            parm.value().setImmediately(1);
        } else {
            parm.value().setImmediately(0);
        }
    }

    public YaeltexArpControlExtension getDriver() {
        return driver;
    }

    public void bindPressed(final RgbButton bt, final Runnable runnable) {
        bindPressed(bt.getHwButton(), runnable);
    }

    @SuppressWarnings("rawtypes")
    public Binding bindLightState(final Supplier<InternalHardwareLightState> supplier, final RgbButton button) {
        return bindLightState(supplier, button.getLight());
    }

    @SuppressWarnings("rawtypes")
    public Binding bindLightState(final ColorButtonLedState state, final RgbButton button) {
        return bindLightState(() -> state, button);
    }

    protected void bindModeButton(final RgbButton button, final Parameter parameter, final int activeValue,
                                  final int resolution, final ColorButtonLedState activeColor,
                                  final ColorButtonLedState nonActiveColor) {
        bindPressed(button, () -> {
            parameter.set(activeValue, resolution);
        });
        bindLightState(
                () -> Math.round(parameter.get() * (resolution - 1)) == activeValue ? activeColor : nonActiveColor,
                button);
    }

    protected void bindModeButton(final RgbButton button, final Parameter parameter, final double activeValue,
                                  final ColorButtonLedState activeColor, final ColorButtonLedState nonActiveColor) {
        bindPressed(button, () -> {
            parameter.set(activeValue);
        });
        bindLightState(() -> parameter.get() == activeValue ? activeColor : nonActiveColor, button);
    }
	
    public LayerActiveValue getActive() {
        return active;
    }

    public void bindButtonMode(final RgbButton bt, final YaeltexLayer mode, final ColorButtonLedState onColor) {
        assert mode != null;
        bindPressed(bt.getHwButton(), () -> driver.setButtonMode(mode));
        mode.getActive().addValueObserver(v -> {
            bt.getLight().state().setValue(v ? onColor : ColorButtonLedState.OFF);
        });
    }

    @Override
    protected void onActivate() {
        active.setValue(true);
    }

    @Override
    protected void onDeactivate() {
        active.setValue(false);
    }

}
