package com.novation.launchcontrol.arp;

public interface ParameterContainer {
	void applyGateValueToParameter(int index, double value);

	double getGateValue(int index);

	void applyNoteValueToParameter(int index, double qValue);
}
