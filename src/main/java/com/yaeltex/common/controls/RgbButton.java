package com.yaeltex.common.controls;

import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

import com.bitwig.extension.api.Color;
import com.bitwig.extension.controller.api.HardwareActionBindable;
import com.bitwig.extension.controller.api.HardwareButton;
import com.bitwig.extension.controller.api.HardwareSurface;
import com.bitwig.extension.controller.api.InternalHardwareLightState;
import com.bitwig.extension.controller.api.MidiIn;
import com.bitwig.extension.controller.api.MultiStateHardwareLight;
import com.bitwig.extension.controller.api.Parameter;
import com.bitwig.extension.controller.api.RelativePosition;
import com.bitwig.extensions.framework.Layer;
import com.bitwig.extensions.framework.time.TimeRepeatEvent;
import com.bitwig.extensions.framework.time.TimedDelayEvent;
import com.bitwig.extensions.framework.time.TimedEvent;
import com.bitwig.extensions.framework.values.Midi;
import com.yaeltex.common.YaeltexButtonLedState;
import com.yaeltex.common.YaeltexIntensityColorState;
import com.yaeltex.common.YaeltexMidiProcessor;

public class RgbButton {
    
    private TimedEvent currentTimer;
    private long recordedDownTime;
    private final int channel;
    public static final int STD_REPEAT_DELAY = 400;
    public static final int STD_REPEAT_FREQUENCY = 50;
    protected MultiStateHardwareLight light;
    protected HardwareButton hwButton;
    protected YaeltexMidiProcessor midiProcessor;
    protected final int midiId;
    
    public RgbButton(final int midiId, final String name, final HardwareSurface surface,
        final YaeltexMidiProcessor midiProcessor) {
        this(0, midiId, name, surface, midiProcessor);
    }
    
    public RgbButton(final int channel, final int midiId, final String name, final HardwareSurface surface,
        final YaeltexMidiProcessor midiProcessor) {
        this.midiProcessor = midiProcessor;
        final MidiIn midiIn = midiProcessor.getMidiIn();
        this.midiId = midiId;
        this.channel = channel;
        light = surface.createMultiStateHardwareLight(name + "_LIGHT_" + midiId);
        hwButton = surface.createHardwareButton(name + "_" + midiId);
        hwButton.pressedAction().setPressureActionMatcher(midiIn.createNoteOnVelocityValueMatcher(channel, midiId));
        hwButton.releasedAction().setActionMatcher(midiIn.createNoteOffActionMatcher(channel, midiId));
        hwButton.setBackgroundLight(light);
        light.state().setValue(YaeltexButtonLedState.OFF);
        hwButton.isPressed().markInterested();
        light.state().onUpdateHardware(this::updateState);
    }
    
    private void updateState(final InternalHardwareLightState internalHardwareLightState) {
        if (internalHardwareLightState instanceof YaeltexButtonLedState state) {
            midiProcessor.sendMidi(Midi.NOTE_ON | channel, midiId, state.getColorCode());
            //midiProcessor.sendMidi(Midi.NOTE_ON | 0x15, midiId, 1);
        } else if (internalHardwareLightState instanceof YaeltexIntensityColorState state) {
            midiProcessor.sendMidi(Midi.NOTE_ON | channel, midiId, state.getColorCode());
            midiProcessor.sendMidi(Midi.NOTE_ON | 0x14, midiId, state.getIntensity());
        } else {
            midiProcessor.sendMidi(Midi.NOTE_ON, midiId, 0);
        }
    }
    
    public void refresh() {
        light.state().setValue(null);
    }
    
    public void bindIsPressed(final Layer layer, final Consumer<Boolean> handler) {
        layer.bind(hwButton, hwButton.pressedAction(), () -> handler.accept(true));
        layer.bind(hwButton, hwButton.releasedAction(), () -> handler.accept(false));
    }
    
    public void bindPressed(final Layer layer, final Runnable action) {
        layer.bind(hwButton, hwButton.pressedAction(), action);
    }
    
    public void bindPressed(final Layer layer, final HardwareActionBindable action) {
        layer.bind(hwButton, hwButton.pressedAction(), action);
    }
    
    public void bindRelease(final Layer layer, final Runnable action) {
        layer.bind(hwButton, hwButton.releasedAction(), action);
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
    
    public void bindToggleValue(final Layer layer, final Parameter parameter) {
        parameter.value().markInterested();
        layer.bindPressed(hwButton, () -> {
            if (parameter.value().get() < 1) {
                parameter.value().set(1);
            } else {
                parameter.value().set(0);
            }
        });
    }
    
    public void bindLightPressed(final Layer layer, final InternalHardwareLightState state,
        final InternalHardwareLightState holdState) {
        layer.bindLightState(() -> hwButton.isPressed().get() ? holdState : state, light);
    }
    
    /**
     * Models following behavior. Pressing and Releasing the button within the given delay time executes the click
     * event.
     * Long Pressing the button invokes the holdAction with true and then the same action with false once released.
     *
     * @param layer       the layer
     * @param clickAction the action invoked if the button is pressed and release in less than the given delay time
     * @param holdAction  action called with true when the delay time expires and with false if released under this
     *                    condition
     * @param delayTime   the delay time
     */
    public void bindDelayedHold(final Layer layer, final Runnable clickAction, final Consumer<Boolean> holdAction,
        final long delayTime) {
        layer.bind(hwButton, hwButton.pressedAction(), () -> initiateHold(holdAction, delayTime));
        layer.bind(hwButton, hwButton.releasedAction(), () -> handleDelayedRelease(clickAction, holdAction));
    }
    
    private void initiateHold(final Consumer<Boolean> holdAction, final long delayTime) {
        recordedDownTime = System.currentTimeMillis();
        currentTimer = new TimedDelayEvent(() -> {
            holdAction.accept(true);
        }, delayTime);
        midiProcessor.queueEvent(currentTimer);
    }
    
    private void handleDelayedRelease(final Runnable clickAction, final Consumer<Boolean> holdAction) {
        if (currentTimer != null && !currentTimer.isCompleted()) {
            currentTimer.cancel();
            clickAction.run();
            currentTimer = null;
        } else {
            holdAction.accept(false);
        }
    }
    
    /**
     * Binds the given action to a button. Upon pressing the button the action is immediately executed. However while
     * holding the button, the action repeats after an initial delay. The standard delay time of 400ms and repeat
     * frequency of 50ms are used.
     *
     * @param layer  the layer this is bound to
     * @param action action to be invoked and after a delay repeat
     */
    public void bindRepeatHold(final Layer layer, final Runnable action) {
        layer.bind(hwButton, hwButton.pressedAction(),
            () -> initiateRepeat(action, STD_REPEAT_DELAY, STD_REPEAT_FREQUENCY));
        layer.bind(hwButton, hwButton.releasedAction(), this::cancelEvent);
    }
    
    public void initiateRepeat(final Runnable action, final int repeatDelay, final int repeatFrequency) {
        action.run();
        currentTimer = new TimeRepeatEvent(action, repeatDelay, repeatFrequency);
        midiProcessor.queueEvent(currentTimer);
    }
    
    private void cancelEvent() {
        if (currentTimer != null) {
            currentTimer.cancel();
            currentTimer = null;
        }
    }
    
    public void setBounds(final double xMm, final double yMm, final double widthMm, final double heightMm) {
        hwButton.setBounds(xMm, yMm, widthMm, heightMm);
        light.setBounds(xMm + 1, yMm + 1, widthMm - 2, heightMm - 2);
    }
    
    public void setLabel(final String label) {
        hwButton.setLabel(label);
        hwButton.setLabelPosition(RelativePosition.BELOW);
        hwButton.setLabelColor(Color.fromHex("#fff"));
    }
    
}
