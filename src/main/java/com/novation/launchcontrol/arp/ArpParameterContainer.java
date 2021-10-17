package com.novation.launchcontrol.arp;

/**
 * Abstraction for accessing basic arp parameters.
 *
 */
public interface ArpParameterContainer {
	/**
	 * Set the value of a gate parameter
	 *
	 * @param index of parameter
	 * @param value the value to be set
	 */
	void applyGateValueToParameter(int index, double value);

	/**
	 * Retrieve value of gate.
	 *
	 * @param index the index of the parameter
	 * @return the value of the gate of the given index
	 */
	double getGateValue(int index);

	/**
	 * Sets the note offset value of a given step.
	 * 
	 * @param index the index of the step
	 * @param value value to be set
	 */
	void applyNoteValueToParameter(int index, double value);
}
