package com.novation.launchpadProMk3;

import com.bitwig.extension.controller.api.HardwareSurface;
import com.bitwig.extension.controller.api.InternalHardwareLightState;
import com.bitwig.extension.controller.api.MidiIn;
import com.bitwig.extension.controller.api.MidiOut;
import com.bitwig.extensions.rh.Midi;

public class LabeledButton extends LpButton {
	private final int ccValue;

	public LabeledButton(final String name, final HardwareSurface surface, final MidiIn midiIn, final MidiOut midiOut,
			final int ccValue) {
		super(name.toLowerCase() + "_" + ccValue, surface, midiOut);
		this.ccValue = ccValue;
		initButtonCc(midiIn, ccValue);
		light.state().onUpdateHardware(this::updatePadLed);
		hwButton.setBackgroundLight(light);
	}

	public LabeledButton(final HardwareSurface surface, final MidiIn midiIn, final MidiOut midiOut,
			final LabelCcAssignments ccAssignment) {
		super(ccAssignment.toString().toLowerCase(), surface, midiOut);
		this.ccValue = ccAssignment.getCcValue();
		initButtonCc(midiIn, ccAssignment);
		light.state().onUpdateHardware(this::updatePadLed);
		hwButton.setBackgroundLight(light);
	}

	void updatePadLed(final InternalHardwareLightState state) {
		final RgbState rgbState = (RgbState) state;
		if (state != null) {
			midiOut.sendMidi(Midi.CC + rgbState.getState().getChannel(), ccValue, rgbState.getColorIndex());
		} else {
			midiOut.sendMidi(Midi.CC, ccValue, 0);
		}
	}

}
