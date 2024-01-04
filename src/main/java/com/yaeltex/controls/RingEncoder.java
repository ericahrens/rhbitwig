package com.yaeltex.controls;

import java.util.function.IntConsumer;

import com.bitwig.extension.controller.api.HardwareSurface;
import com.bitwig.extension.controller.api.MidiIn;
import com.bitwig.extension.controller.api.RelativeHardwareKnob;
import com.bitwig.extensions.framework.Layer;
import com.bitwig.extensions.framework.values.Midi;
import com.yaeltex.common.YaeltexMidiProcessor;

public class RingEncoder {
	private final YaeltexMidiProcessor midiProcessor;
	private final RelativeHardwareKnob encoder;
	private int lastValueSent = -1;
	private int lastColorSent = -1;
	private final int midiValue;
	private final RgbButton button;
	
	public RingEncoder(final int channel, final int midiValue, String name, final HardwareSurface surface, YaeltexMidiProcessor midiProcessor) {
		super();
		this.midiProcessor = midiProcessor;
		final MidiIn midiIn = midiProcessor.getMidiIn();
		this.midiValue = midiValue;

		encoder = surface.createRelativeHardwareKnob(name);
		encoder.setAdjustValueMatcher(midiIn.createRelativeBinOffsetCCValueMatcher(channel, midiValue, 64));
		encoder.setStepSize(1 / 64.0);
		button = new RgbButton(channel, midiValue, name+"_BUTTON", surface, midiProcessor);
	}

	public void bind(final Layer layer, final IntConsumer incHandler) {
		//layer.bind(encoder, driver.createIncrementBinder(incHandler::accept));
	}

	public RelativeHardwareKnob getEncoder() {
		return encoder;
	}

	public void sendValue(final int value) {
		if (value != lastValueSent) {
			midiProcessor.sendMidi(Midi.CC, midiValue, value);
			lastValueSent = value;
		}
	}

	public void setColor(final int value) {
		if (value != lastColorSent) {
			midiProcessor.sendMidi(Midi.CC | 15, midiValue, value);
			lastColorSent = value;
		}
	}

	public void refresh() {
		midiProcessor.sendMidi(Midi.CC, midiValue, lastValueSent);
		midiProcessor.sendMidi(Midi.CC | 15, midiValue, lastColorSent);
	}

	public void clear() {
		midiProcessor.sendMidi(Midi.CC, midiValue, 0);
	}

}
