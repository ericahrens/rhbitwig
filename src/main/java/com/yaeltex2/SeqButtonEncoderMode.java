package com.yaeltex2;

public enum SeqButtonEncoderMode {
	NOTE(0), VELOCITY(1), CHANCE(2), TRANSPOSE(3);

	private final int index;

	private SeqButtonEncoderMode(final int index) {
		this.index = index;
	}

	public int getIndex() {
		return index;
	}

}
