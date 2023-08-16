package com.allenheath.k2.set1;

import com.rhcommons.SpecialVst3Devices;
import com.rhcommons.SpecialVstDevices;

public enum SpecialParam {
    PSP_REPEAT_INF(SpecialVstDevices.LEXICON_PSP, 13),
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
