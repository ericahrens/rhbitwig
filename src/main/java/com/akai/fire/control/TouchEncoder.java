package com.akai.fire.control;

import java.util.function.Consumer;
import java.util.function.IntConsumer;

import com.akai.fire.AkaiFireDrumSeqExtension;
import com.bitwig.extension.controller.api.ControllerHost;
import com.bitwig.extension.controller.api.HardwareButton;
import com.bitwig.extension.controller.api.HardwareSurface;
import com.bitwig.extension.controller.api.MidiIn;
import com.bitwig.extension.controller.api.RelativeHardwarControlBindable;
import com.bitwig.extension.controller.api.RelativeHardwareKnob;
import com.bitwig.extensions.framework.Layer;

public class TouchEncoder {
	private final ControllerHost host;
	private final RelativeHardwareKnob encoder;
	private final HardwareButton touchButton;

	public TouchEncoder(final int controlId, final int buttonId, final AkaiFireDrumSeqExtension driver) {
		this.host = driver.getHost();
		final int noteValue = controlId;
		final HardwareSurface surface = driver.getSurface();
		final MidiIn midiIn = driver.getMidiIn();
		encoder = surface.createRelativeHardwareKnob("ENDLESKNOB_" + controlId);

		encoder.setAdjustValueMatcher(midiIn.createRelativeBinOffsetCCValueMatcher(0, noteValue, 127));
		encoder.setStepSize(0.25);

		// mainLayer.bind(encoder, createIncrementBinder(this::handleIncrement));
		touchButton = surface.createHardwareButton("ENDLESKNOB_BUTTON_" + buttonId);
		touchButton.pressedAction().setPressureActionMatcher(midiIn.createNoteOnVelocityValueMatcher(0, buttonId));
		touchButton.releasedAction().setActionMatcher(midiIn.createNoteOffActionMatcher(0, buttonId));
	}

	public void setStepSize(final double value) {
		encoder.setStepSize(value);
	}

	public void bindTouched(final Layer layer, final Consumer<Boolean> target) {
		layer.bind(touchButton, touchButton.pressedAction(), () -> target.accept(true));
		layer.bind(touchButton, touchButton.releasedAction(), () -> target.accept(false));
	}

	public void bindEncoder(final Layer layer, final IntConsumer action) {
		layer.bind(encoder, createIncrementBinder(action));
	}

	public RelativeHardwarControlBindable createIncrementBinder(final IntConsumer consumer) {
		return host.createRelativeHardwareControlStepTarget(//
				host.createAction(() -> consumer.accept(-1), () -> "-"),
				host.createAction(() -> consumer.accept(1), () -> "+"));
	}

}
