package com.akai.fire;

public enum NoteAssign {
	KNOB_MODE(0x1A), //
	KNOB_MODE_LIGHT(0x1B), //
	PATTERN_UP(0x1F), //
	PATTERN_DOWN(0x20), //
	BROWSER(0x21), //
	ENCODER_PRESS(0x19), //
	BANK_L(0x22), //
	BANK_R(0x23), //
	MUTE_1(0x24), //
	MUTE_2(0x25), //
	MUTE_3(0x26), //
	MUTE_4(0x27), //
	TRACK_SELECT_1(0x28), //
	TRACK_SELECT_2(0x29), //
	TRACK_SELECT_3(0x2A), //
	TRACK_SELECT_4(0x2B), //
	STEP_SEQ(0x2C, true), //
	NOTE(0x2D, true), //
	DRUM(0x2E, true), //
	PERFORM(0x2F, true), //
	SHIFT(0x30), //
	ALT(0x31), //
	PATTERN(0x32, true), //
	PLAY(0x33, true), //
	STOP(0x34), //
	REC(0x35, true);

	private final int noteValue;
	private boolean biColor;

	private NoteAssign(final int noteValue) {
		this(noteValue, false);
	}

	private NoteAssign(final int noteValue, final boolean biColor) {
		this.noteValue = noteValue;
		this.biColor = biColor;
	}

	public boolean isBiColor() {
		return biColor;
	}

	public int getNoteValue() {
		return noteValue;
	}
}
