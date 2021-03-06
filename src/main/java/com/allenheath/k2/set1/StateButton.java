package com.allenheath.k2.set1;

import java.util.function.Supplier;

import com.bitwig.extension.controller.api.HardwareButton;
import com.bitwig.extension.controller.api.HardwareSurface;
import com.bitwig.extension.controller.api.InternalHardwareLightState;
import com.bitwig.extension.controller.api.MidiIn;
import com.bitwig.extension.controller.api.MidiOut;
import com.bitwig.extension.controller.api.MultiStateHardwareLight;
import com.bitwig.extensions.framework.Layer;
import com.bitwig.extensions.rh.Midi;

public class StateButton {
	private final HardwareButton hwButton;
	private final MultiStateHardwareLight light;
	private final MidiOut midiOut;
	private final int noteValue;
	private final int channel;

	protected StateButton(final String id, final int noteValue, final int channel, final HardwareSurface surface,
			final MidiIn midiIn, final MidiOut midiOut) {
		this.midiOut = midiOut;
		this.noteValue = noteValue;
		this.channel = channel;
		hwButton = surface.createHardwareButton(id);
		light = surface.createMultiStateHardwareLight(id + "-light");
		hwButton.isPressed().markInterested();
		light.state().onUpdateHardware(this::updateButtonLed);
		hwButton.setBackgroundLight(light);
		hwButton.pressedAction().setPressureActionMatcher(midiIn.createNoteOnVelocityValueMatcher(channel, noteValue));
		hwButton.releasedAction().setActionMatcher(midiIn.createNoteOffActionMatcher(channel, noteValue));
	}

	void updateButtonLed(final InternalHardwareLightState state) {
		final RedGreenButtonState rgbState = (RedGreenButtonState) state;
		if (state != null) {
			final RedGreenColor color = rgbState.getColor();
			if (color == null || color == RedGreenColor.OFF) {
				midiOut.sendMidi(Midi.NOTE_ON + channel, noteValue, 0);
			} else {
				midiOut.sendMidi(Midi.NOTE_ON + channel, noteValue + color.getOffset(), 127);
			}
		} else {
			midiOut.sendMidi(Midi.NOTE_ON + channel, noteValue, 0);
		}
	}

	public void bind(final Layer layer, final Runnable action, final RedGreenButtonState onColor) {
		layer.bind(hwButton, hwButton.pressedAction(), action);
		layer.bindLightState(() -> hwButton.isPressed().get() ? onColor : RedGreenButtonState.OFF, light);
	}

	public void bind(final Layer layer, final Runnable action, final Supplier<InternalHardwareLightState> supplier) {
		layer.bind(hwButton, hwButton.pressedAction(), action);
		layer.bindLightState(supplier, light);
	}

}
