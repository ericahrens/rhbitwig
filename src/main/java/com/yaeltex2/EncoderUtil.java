package com.yaeltex2;

public class EncoderUtil {
	public static final int OFFSET_NOTE_RANGE = 24;
	public static final int[] NOTE_ENCODER_MAPPING = new int[] { //
			// -23,-22,-21,-20,-19,-18,-17,-16,-15,-14,-13
			10, 18, 18, 18, 18, 18, 18, 18, 18, 18, 18, 18, //
			// -11,-10,-09,-08,-07,-06,-05,-04,-03,-02,-01
			30, 36, 36, 36, 36, 46, 50, 58, 58, 58, 58, 58, //
			// 01, 02, 03, 04, 05, 06, 07, 08, 09, 10, 11
			64, 73, 73, 73, 73, 73, 73, 81, 90, 90, 90, 90, //
			// 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23, 24
			100, 108, 108, 108, 108, 108, 108, 108, 108, 108, 108, 108, 127 //
	};

	public static final int[] ENCODER_INDEX = new int[] { //
			0, 1, 2, 3, 16, 17, 18, 19, //
			4, 5, 6, 7, 20, 21, 22, 23, //
			8, 9, 10, 11, 24, 25, 26, 27, //
			12, 13, 14, 15, 28, 29, 30, 31 };

	private static final int SPOT_RANGE = 117;
	private static final int LED_FIRST = 10;
	private static final float RATION_OFFSET = (float) SPOT_RANGE / (OFFSET_NOTE_RANGE * 2);

	/**
	 * @param value between -12 and 12
	 * @return MIDI CC between 10 == first LED and 127 LAST LED
	 */
	public static int toOffsetCc(final int value) {
		final int v = Math.max(-OFFSET_NOTE_RANGE, Math.min(OFFSET_NOTE_RANGE, value)) + OFFSET_NOTE_RANGE; // 0-24
		return (int) (LED_FIRST + RATION_OFFSET * v);
	}

	public static int indexFromMapping(final double[] mapping, final double value) {
		final int n = mapping.length - 1;
		if (value <= mapping[0]) {
			return 0;
		}
		if (value >= mapping[n]) {
			return n;
		}
		double prev = mapping[0];
		for (int i = 1; i < mapping.length; i++) {
			if (prev < value && value <= mapping[i]) {
				return i;
			}
			prev = mapping[i];
		}
		return n;
	}

}
