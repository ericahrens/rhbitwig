package com.yaeltex2;

import java.util.function.IntConsumer;

import com.bitwig.extension.controller.api.MidiIn;
import com.bitwig.extension.controller.api.MidiOut;
import com.bitwig.extension.controller.api.RelativeHardwareKnob;
import com.bitwig.extensions.framework.Layer;
import com.bitwig.extensions.rh.Midi;

public class RingEncoder {
	private final MidiOut midi;
	private final RelativeHardwareKnob encoder;
	private final ColorButton button;
	private int lastValueSent = -1;
	private int lastColorSent = -1;
	private final int midiValue;
	private final ExtensionDriver driver;
	private final int index;

	public RingEncoder(final int index, final int midiValue, final ExtensionDriver driver) {
		super();
		this.driver = driver;
		this.midi = driver.getMidiOut();
		final MidiIn midiIn = driver.getMidiIn();
		this.midiValue = midiValue;
		this.index = index;

		encoder = driver.getSurface().createRelativeHardwareKnob("ENDLESKNOB_" + index);
		encoder.setAdjustValueMatcher(midiIn.createRelativeBinOffsetCCValueMatcher(0, midiValue, 64));
		encoder.setStepSize(1 / 64.0);
		button = new ColorButton(driver, "ENC", index, midiValue, 0);

		// encoder.addBinding(host.createRelativeHardwareControlStepTarget(incAction,
		// decAction));
	}

	public void bind(final Layer layer, final IntConsumer incHandler) {
		layer.bind(encoder, driver.createIncrementBinder(incHandler::accept));
	}

	public RelativeHardwareKnob getEncoder() {
		return encoder;
	}

	public ColorButton getButton() {
		return button;
	}

	public void sendValue(final int value) {
		if (value != lastValueSent) {
			midi.sendMidi(Midi.CC, midiValue, value);
			lastValueSent = value;
		}
	}

	public void setColor(final int value) {
		if (value != lastColorSent) {
			midi.sendMidi(Midi.CC | 15, midiValue, value);
			lastColorSent = value;
		}
	}

	public void refresh() {
		midi.sendMidi(Midi.CC, midiValue, lastValueSent);
		midi.sendMidi(Midi.CC | 15, midiValue, lastColorSent);
	}

	public void clear() {
		midi.sendMidi(Midi.CC, midiValue, 0);
	}

}
