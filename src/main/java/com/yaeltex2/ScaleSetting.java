package com.yaeltex2;

public class ScaleSetting {

	private int baseNote = 4;
	private Scale scale = Scale.MINOR;

	public int applyIncrement(final int currentNoteValue, final int increment) {
		return scale.incNext(baseNote, currentNoteValue, increment);
	}

	public int getBaseNote() {
		return baseNote;
	}

	public void setBaseNote(final int baseNote) {
		this.baseNote = baseNote;
	}

	public Scale getScale() {
		return scale;
	}

	public void setScale(final Scale scale) {
		this.scale = scale;
	}
}
