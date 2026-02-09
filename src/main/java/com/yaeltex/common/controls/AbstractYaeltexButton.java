package com.yaeltex.common.controls;

import java.util.function.Consumer;

import com.bitwig.extension.api.Color;
import com.bitwig.extension.controller.api.HardwareActionBindable;
import com.bitwig.extension.controller.api.HardwareButton;
import com.bitwig.extension.controller.api.HardwareSurface;
import com.bitwig.extension.controller.api.MidiIn;
import com.bitwig.extension.controller.api.Parameter;
import com.bitwig.extension.controller.api.RelativePosition;
import com.bitwig.extensions.framework.Layer;
import com.bitwig.extensions.framework.time.TimeRepeatEvent;
import com.bitwig.extensions.framework.time.TimedDelayEvent;
import com.bitwig.extensions.framework.time.TimedEvent;
import com.yaeltex.common.YaeltexMidiProcessor;

public class AbstractYaeltexButton {
    protected TimedEvent currentTimer;
    protected long recordedDownTime;
    protected final int channel;
    public static final int STD_REPEAT_DELAY = 400;
    public static final int STD_REPEAT_FREQUENCY = 50;
    protected final int midiId;
    protected final int midiPort;
    protected HardwareButton hwButton;
    protected YaeltexMidiProcessor midiProcessor;
    
    public AbstractYaeltexButton(final int port, final int channel, final int midiId, final String name,
        final HardwareSurface surface, final YaeltexMidiProcessor midiProcessor) {
        this.midiProcessor = midiProcessor;
        this.midiPort = port;
        final MidiIn midiIn = midiProcessor.getMidiIn(port);
        this.midiId = midiId;
        this.channel = channel;
        hwButton = surface.createHardwareButton(name + "_" + midiId);
        hwButton.pressedAction().setPressureActionMatcher(midiIn.createNoteOnVelocityValueMatcher(channel, midiId));
        hwButton.releasedAction().setActionMatcher(midiIn.createNoteOffActionMatcher(channel, midiId));
        hwButton.isPressed().markInterested();
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
    
    public void bindMomentaryValue(final Layer layer, final Parameter parameter) {
        parameter.value().markInterested();
        layer.bindPressed(
            hwButton, () -> {
                if (parameter.value().get() < 1) {
                    parameter.value().set(1);
                }
            });
        layer.bindReleased(
            hwButton, () -> {
                if (parameter.value().get() > 0) {
                    parameter.value().set(0);
                }
            });
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
        currentTimer = new TimedDelayEvent(
            () -> {
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
        layer.bind(
            hwButton, hwButton.pressedAction(),
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
    
    public void setBounds(final double x, final double y, final double width, final double height) {
        hwButton.setBounds(x, y, width, height);
    }
    
    public void setLabel(final String label) {
        hwButton.setLabel(label);
        hwButton.setLabelPosition(RelativePosition.BELOW);
        hwButton.setLabelColor(Color.fromHex("#fff"));
    }
    
}
