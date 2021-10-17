package com.yaeltex.device;

import com.bitwig.extension.controller.api.Parameter;
import com.yaeltex.EncoderUtil;
import com.yaeltex.QuantizeMode;

/**
 * Represents an single instance of an arpeggiator device. Contains and manages
 * states that are not in the device itself. These being
 * <ul>
 * <li>stored gate values
 * <li>stored velocity values
 * <li>separate offset note value and base note value as set by knob/slider
 * <li>quantize layout
 * <li>gate mute state (button and/or due to quantization)
 * </ul>
 *
 */
public class ArpInstance {

	private static final int NUMBER_OF_STEPS = 16;
	public static int GATE_NOMUTE = 0;
	public static int GATE_BUTTON_MUTE = 1;
	public static int GATE_QUANTIZE_MUTE = 2;
	public static int GATE_BUTTON_QUANTIZE_MUTE = 3;
	private static final int BASE_NOTE_RANGE = 24;

	private final int[] gateMuteState = new int[NUMBER_OF_STEPS];
	private final int[] velocityMuteState = new int[NUMBER_OF_STEPS];

	private final int[] baseNote = new int[NUMBER_OF_STEPS]; // Internal base note
	private final int[] offsetNote = new int[NUMBER_OF_STEPS]; // Internal offset note

	private String presetName;
	private String trackName;

	private final boolean quanitze[] = new boolean[12];
	// private final ArpParameterContainer parameterContainer;
	private QuantizeMode quantizeMode = QuantizeMode.NEAREST_VALUE;

	public ArpInstance(final String trackName, final String presetName, final QuantizeMode mode) {
		this.presetName = presetName;
		this.trackName = trackName;

		for (int i = 0; i < 12; i++) {
			quanitze[i] = true;
		}
		for (int i = 0; i < gateMuteState.length; i++) {
			gateMuteState[i] = -1;
			velocityMuteState[i] = -1;
		}
		this.quantizeMode = mode;
	}

	@Override
	public String toString() {
		return String.format("ARP INSTANCE %s pn=[%s]", trackName, presetName);
	}

	public boolean isInitArp() {
		return this.trackName == null;
	}

	public int getNoteOffset(final int index) {
		return offsetNote[index];
	}

	public void changeBaseNote(final int index, final int offset) {
		baseNote[index] = Math.min(BASE_NOTE_RANGE, Math.max(baseNote[index] + offset, -BASE_NOTE_RANGE));
	}

	public void changeNoteOffset(final int index, final int offset) {
		offsetNote[index] = Math.min(EncoderUtil.OFFSET_NOTE_RANGE,
				Math.max(offsetNote[index] + offset, -EncoderUtil.OFFSET_NOTE_RANGE));
	}

	public void resetNoteOffset(final int index) {
		offsetNote[index] = 0;
	}

	/**
	 * According to mode this returns the current note values set by base note and
	 * offset.
	 *
	 * @param index index of note
	 * @return note value (possibly quantized) normalized between 0 and 1;
	 */
	public double getNoteValueAct(final int index) {
		if (quantizeMode == QuantizeMode.MUTE) {
			return (Math.min(Math.max(-24, baseNote[index] + offsetNote[index]), 24) + 24.0) / 48.0;
		} else {
			final int sum = Math.min(Math.max(-24, baseNote[index] + offsetNote[index]), 24);
			return (quantizeToNearest(sum) + 24) / 48;
		}
	}

	protected void toggleGate(final int index, final int actualValue, final Parameter parameter) {
		final int prevState = gateMuteState[index];
		if (prevState == -1) { // NOT Muted
			gateMuteState[index] = actualValue;
			parameter.setImmediately(0);
		} else { // IS Muted
			final double v = gateMuteState[index] / 127.0;
			parameter.setImmediately(Math.max(0, v));
			gateMuteState[index] = -1;
		}
	}

	protected void toggleVelocity(final int index, final int actualValue, final Parameter parameter) {
		final int prevState = velocityMuteState[index];
		if (prevState == -1) { // NOT Muted
			velocityMuteState[index] = actualValue;
			parameter.setImmediately(0);
		} else { // IS Muted
			final double v = velocityMuteState[index] / 127.0;
			parameter.setImmediately(v);
			velocityMuteState[index] = -1;
		}
	}

	public void updateVelocityValue(final int index, final int newValue) {
		if (velocityMuteState[index] != -1) {
			velocityMuteState[index] = newValue;
		}
	}

	public void updateGateValue(final int index, final int newValue) {
		if (gateMuteState[index] != -1) {
			gateMuteState[index] = newValue;
		}
	}

	public boolean matches(final String trackName, final String presetName) {
//		if (presetName.length() == 0) {
//			return trackName.equals(this.trackName);
//		}
		return trackName.equals(this.trackName) && presetName.equals(this.presetName);
	}

	public String getPresetName() {
		return presetName;
	}

	public void setPresetName(final String name) {
		this.presetName = name;
	}

	public String getTrackName() {
		return trackName;
	}

	public void setTrackName(final String trackName) {
		this.trackName = trackName;
	}

	public void setOffsetNote(final int index, final int value) {
		this.offsetNote[index] = value;
		updateNoteMuteState(index);
	}

	/**
	 * Determine if a certain step is located in the scale selection.
	 *
	 * @param index index of step.
	 * @return true if note (offset + base) in scale
	 */
	private boolean inScale(final int index) {
		final int value = Math.min(Math.max(-24, baseNote[index] + offsetNote[index]), 24);
		return quanitze[(value + 24) % 12];
	}

	private double quantizeToNearest(final int value) {
		final int nv = (value + 24) % 12;
		if (quanitze[nv]) {
			return value;
		}
		int vu = value;
		while (vu < 24) {
			if (quanitze[(vu + 24) % 12]) {
				return vu;
			}
			vu++;
		}
		vu = value;
		while (vu > -25) {
			if (quanitze[(vu + 24) % 12]) {
				return vu;
			}
			vu--;
		}
		return value;
	}

	public boolean isGateMuted(final int index) {
		return gateMuteState[index] != -1;
	}

	public boolean isVelocityMuted(final int index) {
		return velocityMuteState[index] != -1;
	}

	public int getGateMute(final int index) {
		return gateMuteState[index];
	}

	public int getVelMute(final int index) {
		return velocityMuteState[index];
	}

	public boolean isQuantizeNoteSet(final int note) {
		return quanitze[note];
	}

	public void toggleQuantizeNote(final int note) {
		quanitze[note] = !quanitze[note];
		for (int i = 0; i < NUMBER_OF_STEPS; i++) {
			updateNoteMuteState(i);
		}
	}

	private void updateNoteMuteState(final int index) {
		final int prevState = gateMuteState[index];
		if (inScale(index)) {
			gateMuteState[index] &= ~GATE_QUANTIZE_MUTE;
		} else {
			gateMuteState[index] |= GATE_QUANTIZE_MUTE;
		}
		if (quantizeMode == QuantizeMode.MUTE) {
			if (prevState == GATE_NOMUTE && gateMuteState[index] > 0) {
				// State was switched to muted
//				final double value = parameterContainer.getGateValue(index);
//				if (value > 0) {
//					storedGates[index] = value;
//				}
//				parameterContainer.applyGateValueToParameter(index, 0.0);
			} else if (prevState > 0 && gateMuteState[index] == GATE_NOMUTE) {
				// State was switched, so restore value
//				parameterContainer.applyGateValueToParameter(index, storedGates[index]);
			}
		} else {
			if (!inScale(index)) {
//				final double qValue = getNoteValue(index);
//				parameterContainer.applyNoteValueToParameter(index, qValue);
			}
		}
	}

}
