package com.novation.launchcontrol.arp;

import com.bitwig.extension.controller.api.Parameter;

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

	private final double[] storedVelocities = new double[NUMBER_OF_STEPS];
	private final double[] storedGates = new double[NUMBER_OF_STEPS];

	private final int[] gateMuteState = new int[NUMBER_OF_STEPS];

	private final int[] baseNote = new int[NUMBER_OF_STEPS];
	private final int[] offsetNote = new int[NUMBER_OF_STEPS];
	private String identifier;

	private final boolean quanitze[] = new boolean[12];
	private final ArpParameterContainer parameterContainer;
	private QuantizeMode quantizeMode;

	public ArpInstance(final String identifier, final ArpParameterContainer paramContainer, final QuantizeMode mode) {
		this.identifier = identifier;
		this.parameterContainer = paramContainer;
		for (int i = 0; i < 12; i++) {
			quanitze[i] = true;
		}
		this.quantizeMode = mode;
	}

	public void setQuantizeMode(final QuantizeMode quantizeMode) {
		this.quantizeMode = quantizeMode;
		if (quantizeMode == QuantizeMode.MUTE) {
			for (int i = 0; i < NUMBER_OF_STEPS; i++) {
				transitionToMuteValueMode(i);
			}
		} else {
			for (int i = 0; i < NUMBER_OF_STEPS; i++) {
				transitionToNearestValueMode(i);
			}
		}
	}

	private void transitionToNearestValueMode(final int index) {
		final boolean previouslyDeactivated = gateMuteState[index] > 0;
		final boolean nowDeactivated = (gateMuteState[index] & GATE_BUTTON_MUTE) != 0;

		if (!previouslyDeactivated && nowDeactivated) {
			// State was switched to muted
			storedGates[index] = parameterContainer.getGateValue(index);
			parameterContainer.applyGateValueToParameter(index, 0.0);
		} else if (previouslyDeactivated && !nowDeactivated) {
			// State was switched, so restore value
			parameterContainer.applyGateValueToParameter(index, storedGates[index]);
		}
	}

	private void transitionToMuteValueMode(final int index) {
		final boolean previouslyDeactivated = (gateMuteState[index] & GATE_BUTTON_MUTE) != 0;
		final boolean nowDeactivated = gateMuteState[index] > 0;

		if (!previouslyDeactivated && nowDeactivated) {
			// State was switched to muted
			storedGates[index] = parameterContainer.getGateValue(index);
			parameterContainer.applyGateValueToParameter(index, 0.0);
		} else if (previouslyDeactivated && !nowDeactivated) {
			// State was switched, so restore value
			parameterContainer.applyGateValueToParameter(index, storedGates[index]);
		}
	}

	public void updateGateParam(final int index, final Parameter param, final double value) {
		final int state = gateMuteState[index];
		if (isActive(state)) {
			param.value().set(value);
		}
		storeGate(index, value);
	}

	protected void toggleGate(final int index, final Parameter parm) {
		final int prevState = gateMuteState[index];
		final int newState = toggleGateButtonMuteState(index);
		if (isActive(newState)) {
			parm.value().setImmediately(getStoredGate(index));
		} else {
			if (isActive(prevState)) {
				storeGate(index, parm.value().get());
			}
			parm.value().setImmediately(0);
		}
	}

	protected void toggleSkip(final int index, final Parameter parm) {
		if (parm.value().get() == 0) {
			parm.value().setImmediately(1);
		} else {
			parm.value().setImmediately(0);
		}
	}

	private boolean isActive(final int state) {
		if (quantizeMode == QuantizeMode.MUTE) {
			return state == GATE_NOMUTE;
		}
		return (state & GATE_BUTTON_MUTE) == 0;
	}

	public ColorButtonLedState gateValueToLed(final double value, final int index) {
		final int state = getGateMute(index);
		if (quantizeMode == QuantizeMode.MUTE) {
			if (state == GATE_BUTTON_MUTE) { // Button
				return ColorButtonLedState.ORANGE_DIM;
			} else if (state == GATE_QUANTIZE_MUTE) {
				return ColorButtonLedState.GREEN_DIM;
			} else if (state == GATE_BUTTON_QUANTIZE_MUTE) {
				return ColorButtonLedState.RED_DIM;
			} else if (value == 0) {
				return ColorButtonLedState.OFF;
			} else if (value == 1) {
				return ColorButtonLedState.AMBER_FULL;
			}
			return ColorButtonLedState.AMBER_SEMI;
		}

		if (!isActive(state)) { // Button
			return ColorButtonLedState.ORANGE_DIM;
		} else if (value == 0) {
			return ColorButtonLedState.OFF;
		} else if (value == 1) {
			return ColorButtonLedState.AMBER_FULL;
		} else {
			return ColorButtonLedState.AMBER_SEMI;
		}
	}

	public String getIdentifier() {
		return identifier;
	}

	public void setIdentifier(final String identifier) {
		this.identifier = identifier;
	}

	public void setBaseNote(final int index, final int value) {
		this.baseNote[index] = value;
		updateNoteMuteState(index);
	}

	public void setOffsetNote(final int index, final int value) {
		this.offsetNote[index] = value;
		updateNoteMuteState(index);
	}

	/**
	 * According to mode this returns the current note values set by base note and
	 * offset.
	 *
	 * @param index index of note
	 * @return note value (possibly quantized) normalized between 0 and 1;
	 */
	public double getNoteValue(final int index) {
		if (quantizeMode == QuantizeMode.MUTE) {
			return (Math.min(Math.max(-24, baseNote[index] + offsetNote[index]), 24) + 24.0) / 48.0;
		} else {
			final int sum = Math.min(Math.max(-24, baseNote[index] + offsetNote[index]), 24);
			return (quantizeToNearest(sum) + 24) / 48;
		}
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

	public double getStoredVelocity(final int index) {
		return storedVelocities[index];
	}

	public void storeVelocity(final int index, final double d) {
		storedVelocities[index] = d;
	}

	public double getStoredGate(final int index) {
		return storedGates[index];
	}

	public void storeGate(final int index, final double d) {
		storedGates[index] = d;
	}

	public int getGateMute(final int index) {
		return gateMuteState[index];
	}

	private int toggleGateButtonMuteState(final int index) {
		gateMuteState[index] ^= GATE_BUTTON_MUTE;
		return gateMuteState[index];
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
				final double value = parameterContainer.getGateValue(index);
				if (value > 0) {
					storedGates[index] = value;
				}
				parameterContainer.applyGateValueToParameter(index, 0.0);
			} else if (prevState > 0 && gateMuteState[index] == GATE_NOMUTE) {
				// State was switched, so restore value
				parameterContainer.applyGateValueToParameter(index, storedGates[index]);
			}
		} else {
			if (!inScale(index)) {
				final double qValue = getNoteValue(index);
				parameterContainer.applyNoteValueToParameter(index, qValue);
			}
		}
	}

}
