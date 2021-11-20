package com.yaeltex2;

public enum Scale {
	CHROMATIC("Chromatic", 0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11), //
	MAJOR("Ionian/Major", 0, 2, 4, 5, 7, 9, 11), //
	MINOR("Aeolian/Minor", 0, 2, 3, 5, 7, 8, 10), //
	PENTATONIC("Pentatonic", 0, 2, 4, 7, 9), //
	PENTATONIC_MINOR("Pentatonic_Minor", 0, 3, 5, 7, 10), //
	DORIAN("Dorian (B/g)", 0, 2, 3, 5, 7, 9, 10), //
	PHRYGIAN("Phrygian (A-flat/f)", 0, 1, 3, 5, 7, 8, 10), //
	LYDIAN("Lydian (D/e)", 0, 2, 4, 6, 7, 9, 11), //
	MIXOLYDIAN("Mixolydian (F/d)", 0, 2, 4, 5, 7, 9, 10), //
	LOCRIAN("Locrian", 0, 1, 3, 5, 6, 8, 10), //
	DIMINISHED("Diminished", 0, 2, 3, 5, 6, 8, 9, 10), //
	MAJOR_BLUES("Major Blues", 0, 3, 4, 7, 9, 10), //
	MINOR_BLUES("Minor Blues", 0, 3, 4, 6, 7, 10), //
	WHOLE("Whole", 0, 2, 4, 6, 8, 10);

	private final String name;
	private final int[] intervalls;
	private final boolean[] calc = new boolean[12];

	Scale(final String name, final int... notes) {
		this.name = name;
		this.intervalls = notes;
		for (final int noff : notes) {
			calc[noff] = true;
		}
	}

	public String getName() {
		return name;
	}

	public int[] getIntervalls() {
		return intervalls;
	}

	public int incNext(final int baseNote, final int note, final int amount) {
		int newNote = note + amount;
		final int inc = amount < 0 ? -1 : 1;
		while (!calc[(newNote - baseNote + 24) % 12]) {
			newNote += inc;
		}
		if (newNote < 0) {
			return 0;
		}
		if (newNote > 127) {
			return 127;
		}
		return newNote;
	}

	public int getNextNote(final int startNote, final int baseNote, final int amount) {
		final int noteIndex = (startNote + 12 - baseNote) % 12;
		int octave = startNote < baseNote ? (startNote - baseNote - 12) / 12 : (startNote - baseNote) / 12;

		final int index = findScaleIndex(noteIndex, intervalls);

		int nextIndex = index + amount;
		if (nextIndex >= intervalls.length) {
			nextIndex = 0;
			octave++;
		} else if (nextIndex < 0) {
			nextIndex = intervalls.length - 1;
			octave--;
		}
		return intervalls[nextIndex] + baseNote + octave * 12;
	}

	private static int findScaleIndex(final int noteIndex, final int[] intervalls) {
		for (int i = 0; i < intervalls.length; i++) {
			if (intervalls[i] >= noteIndex) {
				return i;
			}
		}
		return -1;
	}

	/**
	 * Given a starting note, determines the highest note at the end of the range.
	 *
	 * @param startNote starting note
	 * @param noteRange available notes
	 * @return last note in range
	 */
	public int highestNote(final int startNote, final int noteRange) {
		final int octaves = noteRange / intervalls.length;
		final int lastvalue = intervalls[(noteRange - 1) % intervalls.length];
		return startNote + octaves * 12 + lastvalue;
	}

}
