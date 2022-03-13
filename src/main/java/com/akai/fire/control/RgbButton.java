package com.akai.fire.control;

import com.akai.fire.AkaiFireDrumSeqExtension;
import com.akai.fire.lights.RgbLigthState;
import com.bitwig.extension.controller.api.HardwareButton;
import com.bitwig.extension.controller.api.MidiIn;
import com.bitwig.extension.controller.api.MultiStateHardwareLight;
import com.bitwig.extension.controller.api.SettableBooleanValue;
import com.bitwig.extensions.framework.Layer;

import java.util.function.Consumer;
import java.util.function.Supplier;

import static com.akai.fire.AkaiFireDrumSeqExtension.*;

public class RgbButton {
    private final byte[] sysExCmd = new byte[]{SE_ST, MAN_ID_AKAI, DEVICE_ID, PRODUCT_ID, SE_CMD_RGB, 0, 4, 0, 0, 0, 0, SE_EN};
    private final int index;
    private final HardwareButton hwButton;
    private final MultiStateHardwareLight light;

    public RgbButton(final int pad, final AkaiFireDrumSeqExtension driver) {
        index = pad;
        sysExCmd[7] = (byte) pad;
        final MidiIn midiIn = driver.getMidiIn();
        hwButton = driver.getSurface().createHardwareButton("RGB_PAD_" + index);
        hwButton.pressedAction().setPressureActionMatcher(midiIn.createNoteOnVelocityValueMatcher(0, 0x36 + index));
        hwButton.releasedAction().setActionMatcher(midiIn.createNoteOffActionMatcher(0, 0x36 + index));
        hwButton.isPressed().markInterested();
        light = driver.getSurface().createMultiStateHardwareLight("RGB_PAD_LIGHT_" + index);
        hwButton.setBackgroundLight(light);
        light.state().onUpdateHardware(state -> {
            if (state instanceof RgbLigthState) {
                driver.updateRgbPad(index, (RgbLigthState) state);
            } else {
                driver.updateRgbPad(index, RgbLigthState.OFF);
            }
        });
    }

    public void bindPressed(final Layer layer, final Consumer<Boolean> target) {
        layer.bind(hwButton, hwButton.pressedAction(), () -> target.accept(true));
        layer.bind(hwButton, hwButton.releasedAction(), () -> target.accept(false));
        // layer.bindLightState(() -> colorProvider.get(), light);
    }

    public void bindPressed(final Layer layer, final Consumer<Boolean> target,
                            final Supplier<RgbLigthState> lightSource) {
        layer.bind(hwButton, hwButton.pressedAction(), () -> target.accept(true));
        layer.bind(hwButton, hwButton.releasedAction(), () -> target.accept(false));
        layer.bindLightState(lightSource::get, light);
    }

    public void bindToggle(final Layer layer, final SettableBooleanValue value) {
        layer.bind(hwButton, hwButton.pressedAction(), value::toggle);
    }

    public void bind(final Layer layer, final Runnable action) {
        layer.bindPressed(hwButton, action);
    }

    public void bindLight(final Layer layer, final Supplier<RgbLigthState> lightSupplier) {
        layer.bindLightState(lightSupplier::get, light);
    }

}
