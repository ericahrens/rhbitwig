package com.allenheath.k2.set1;

public enum SpecialParam {
    PSP_REPEAT_INF(SpecialVstDevices.LEXICON_PSP, 25), // Parameter ID from 13 to 25 - required from the updated plugin to work (Lexicon PSP42x 2.0.2)
    MEGA_VERB_GATE(SpecialVst3Devices.MEAGAVERB3, 1569);

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
