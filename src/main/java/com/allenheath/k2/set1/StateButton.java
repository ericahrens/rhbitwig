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
	private static final boolean VERBOSE_LED_LOGS = false;
	private final HardwareButton hwButton;
	private final MultiStateHardwareLight light;
	private final MidiOut midiOut;
	private final int ledNoteValue;
	private final int channel;
	private final boolean useColorOffset;

	protected StateButton(final String id, final int noteValue, final int channel, final HardwareSurface surface,
			final MidiIn midiIn, final MidiOut midiOut) {
		this(id, noteValue, noteValue, channel, surface, midiIn, midiOut, true);
	}

	protected StateButton(final String id, final int noteValue, final int channel, final HardwareSurface surface,
			final MidiIn midiIn, final MidiOut midiOut, final boolean useColorOffset) {
		this(id, noteValue, noteValue, channel, surface, midiIn, midiOut, useColorOffset);
	}

	protected StateButton(final String id, final int noteValue, final int ledNoteValue, final int channel,
			final HardwareSurface surface, final MidiIn midiIn, final MidiOut midiOut, final boolean useColorOffset) {
		this.midiOut = midiOut;
		this.ledNoteValue = ledNoteValue;
		this.channel = channel;
		this.useColorOffset = useColorOffset;
        
		hwButton = surface.createHardwareButton(id);
		light = surface.createMultiStateHardwareLight(id + "-light");
		hwButton.isPressed().markInterested();
		light.state().onUpdateHardware(this::updateButtonLed);
		hwButton.setBackgroundLight(light);
        
		hwButton.pressedAction().setPressureActionMatcher(midiIn.createNoteOnVelocityValueMatcher(channel, noteValue));
		hwButton.releasedAction().setActionMatcher(midiIn.createNoteOffActionMatcher(channel, noteValue));
	}

	void updateButtonLed(final InternalHardwareLightState state) {
		final int status = Midi.NOTE_ON | (channel & 0x0F);
		if (state instanceof RedGreenButtonState) {
			final RedGreenButtonState rgbState = (RedGreenButtonState) state;
			final RedGreenColor color = rgbState.getColor();
			final int ledNote = useColorOffset && color != null ? ledNoteValue + color.getOffset() : ledNoteValue;
			if (color == null || color == RedGreenColor.OFF) {
				sendLedMidi(status, ledNoteValue, 0);
			} else {
				sendLedMidi(status, ledNote, 127);
			}
		} else {
			sendLedMidi(status, ledNoteValue, 0);
		}
	}

	private void sendLedMidi(final int status, final int data1, final int data2) {
		midiOut.sendMidi(status, data1, data2);
		if (VERBOSE_LED_LOGS) {
			System.out.println("K2 LED OUT -> status=" + status + " note=" + data1 + " value=" + data2);
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