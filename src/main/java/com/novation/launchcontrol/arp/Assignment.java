package com.novation.launchcontrol.arp;

import com.bitwig.extension.controller.api.HardwareActionMatcher;
import com.bitwig.extension.controller.api.MidiIn;
import com.bitwig.extensions.framework.values.Midi;

public enum Assignment {
	SEND_UP(Midi.CC, 104, 0), //
	SEND_DOWN(Midi.CC, 105, 0), //
	TRACK_LEFT(Midi.CC, 106, 0), //
	TRACK_RIGHT(Midi.CC, 107, 0), //
	DEVICE(Midi.NOTE_ON, 105, 0), //
	MUTE(Midi.NOTE_ON, 106, 0), //
	SOLO(Midi.NOTE_ON, 107, 0), //
	ARM(Midi.NOTE_ON, 108, 0);

	private int midiStatus;
	private int dataValue;
	private int channel;

	private Assignment(final int midiStatus, final int value, final int channel) {
		this.midiStatus = midiStatus;
		this.dataValue = value;
		this.channel = channel;
	}

	public HardwareActionMatcher createActionMatcherPressed(final MidiIn midiIn) {
		if (midiStatus == Midi.CC) {
			return midiIn.createCCActionMatcher(channel, dataValue, 127);
		} else if (midiStatus == Midi.NOTE_ON) {
			return midiIn.createNoteOnActionMatcher(channel, dataValue);
		}
		return null;
	}

	public HardwareActionMatcher createActionMatcherReleaseed(final MidiIn midiIn) {
		if (midiStatus == Midi.CC) {
			return midiIn.createCCActionMatcher(channel, dataValue, 0);
		} else if (midiStatus == Midi.NOTE_ON) {
			return midiIn.createNoteOffActionMatcher(channel, dataValue);
		}
		return null;
	}

	public int getMidiStatus() {
		return midiStatus | channel;
	}

	public int getDataValue() {
		return dataValue;
	}

}
