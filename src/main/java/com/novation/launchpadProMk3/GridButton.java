package com.novation.launchpadProMk3;

import com.bitwig.extension.controller.api.HardwareSurface;
import com.bitwig.extension.controller.api.InternalHardwareLightState;
import com.bitwig.extension.controller.api.MidiIn;
import com.bitwig.extension.controller.api.MidiOut;
import com.bitwig.extensions.rh.Midi;

public class GridButton extends LpButton {
	private final int row;
	private final int col;
	private final int notevalue;

	public GridButton(final HardwareSurface surface, final MidiIn midiIn, final MidiOut midiOut, final int row,
			final int col) {
		super("grid_" + row + "_" + col, surface, midiOut);
		this.row = row;
		this.col = col;
		this.notevalue = 10 * (8 - row) + col + 1;
		initButtonNote(midiIn, notevalue);
		light.state().setValue(RgbState.of(0));
		light.state().onUpdateHardware(this::updatePadLed);
		hwButton.setBackgroundLight(light);
	}

	public int getRow() {
		return row;
	}

	public int getCol() {
		return col;
	}

	void updatePadLed(final InternalHardwareLightState state) {
		final RgbState rgbState = (RgbState) state;
		if (state != null) {
			midiOut.sendMidi(Midi.NOTE_ON + rgbState.getState().getChannel(), notevalue, rgbState.getColorIndex());
		} else {
			midiOut.sendMidi(Midi.NOTE_ON, notevalue, 0);
		}
	}
}
