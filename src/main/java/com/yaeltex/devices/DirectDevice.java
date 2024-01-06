package com.yaeltex.devices;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.bitwig.extension.controller.api.ControllerHost;
import com.bitwig.extension.controller.api.DeviceMatcher;

public enum DirectDevice {
    POLY_SYNTH("a9ffacb5-33e9-4fc7-8621-b1af31e410ef", true,
        List.of(new TypePairings(ParameterType.CUT, "F1FREQ"), new TypePairings(ParameterType.RES, "F1RESO"),
            new TypePairings(ParameterType.AMT, "FEGDEPTH"), new TypePairings(ParameterType.MOD, "PRE-FITLER_GAIN"),
            new TypePairings(ParameterType.ENV_A, "A"), new TypePairings(ParameterType.ENV_D, "D"),
            new TypePairings(ParameterType.ENV_S, "S"), new TypePairings(ParameterType.ENV_R, "R"))), //
    PHOSCYON("ABCDEF019182FAEB6431366750685332", false,
        List.of(new TypePairings(ParameterType.CUT, "PID34"), new TypePairings(ParameterType.RES, "PID35"),
            new TypePairings(ParameterType.AMT, "PID36"), new TypePairings(ParameterType.MOD, "PID38"),
            new TypePairings(ParameterType.ENV_A, "PID621"), new TypePairings(ParameterType.ENV_D, "PID37"),
            new TypePairings(ParameterType.ENV_S, "PID39"), new TypePairings(ParameterType.ENV_R, "PID65d")));
    
    private final String id;
    private final UUID uuid;
    private final Map<ParameterType, String> typeToParamName = new HashMap<>();
    private final Map<String, ParameterType> paramNameToType = new HashMap<>();
    
    private record TypePairings(ParameterType type, String paramName) {
        //
    }
    
    DirectDevice(final String id, boolean isBitwig, List<TypePairings> paringsList) {
        this.id = id;
        this.uuid = isBitwig ? UUID.fromString(id) : null;
        for (TypePairings pairing : paringsList) {
            String paramName = "CONTENTS/" + pairing.paramName;
            typeToParamName.put(pairing.type, paramName);
            paramNameToType.put(paramName, pairing.type);
        }
    }
    
    public DeviceMatcher createMatcher(ControllerHost host) {
        if (uuid == null) {
            return host.createVST3DeviceMatcher(id);
        }
        return host.createBitwigDeviceMatcher(uuid);
    }
    
    public String getParamName(final ParameterType type) {
        return typeToParamName.get(type);
    }
    
}
