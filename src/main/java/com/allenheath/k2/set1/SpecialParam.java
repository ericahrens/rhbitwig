package com.allenheath.k2.set1;

public enum SpecialParam {
	PSP_REPEAT_INF(SpecialVstDevices.LEXICON_PSP, 13), PSP2_REPEAT_INF(SpecialVst3Devices.LEXICON_PSP2, 25),
	PSP2_DELAY_X2(SpecialVst3Devices.LEXICON_PSP2, 18), PSP_DELAY_X2(SpecialVstDevices.LEXICON_PSP, 4),
	MEGA_VERB_GATE(SpecialVst3Devices.MEGAVERB3, 1569);

	private final int paramId;
	private final SpecialDevice deviceType;

	SpecialParam(final SpecialDevice deviceType, final int paramId) {
		this.paramId = paramId;
		this.deviceType = deviceType;
	}

	public int getParamId() {
		return paramId;
	}

	public SpecialDevice getDeviceType() {
		return deviceType;
	}

}
