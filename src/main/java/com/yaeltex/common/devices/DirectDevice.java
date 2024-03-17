package com.yaeltex.common.devices;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
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
            new TypePairings(ParameterType.ENV_S, "PID39"), new TypePairings(ParameterType.ENV_R, "PID65d"))), //
    ACID_V("41727475415649535433303350726F63", false,
        List.of(new TypePairings(ParameterType.CUT, "PID4"), new TypePairings(ParameterType.RES, "PID5"),
            new TypePairings(ParameterType.AMT, "PID6"), new TypePairings(ParameterType.MOD, "PID8"),
            new TypePairings(ParameterType.ENV_A, "PID9"), new TypePairings(ParameterType.ENV_D, "PID7"),
            new TypePairings(ParameterType.ENV_S, "PIDb"), new TypePairings(ParameterType.ENV_R, "PID10"))), //
    PIGMENTS("41727475415649534B61743150726F63", false,
        List.of(new TypePairings(ParameterType.CUT, "PIDa5"), new TypePairings(ParameterType.RES, "PIDa6"),
            new TypePairings(ParameterType.AMT, "PID3"), new TypePairings(ParameterType.MOD, "PID4"),
            new TypePairings(ParameterType.ENV_A, "PID1"), new TypePairings(ParameterType.ENV_D, "PID2"),
            new TypePairings(ParameterType.ENV_S, "PID3"), new TypePairings(ParameterType.ENV_R, "PID4")));
    
    private final String id;
    private final UUID uuid;
    private final Map<ParameterType, String> typeToParamName = new HashMap<>();
    private final Map<String, ParameterType> paramNameToType = new HashMap<>();
    
    private record TypePairings(ParameterType type, String paramName) {
        //
    }
    
    DirectDevice(final String id, final boolean isBitwig, final List<TypePairings> paringsList) {
        this.id = id;
        this.uuid = isBitwig ? UUID.fromString(id) : null;
        for (final TypePairings pairing : paringsList) {
            final String paramName = "CONTENTS/" + pairing.paramName;
            typeToParamName.put(pairing.type, paramName);
            paramNameToType.put(paramName, pairing.type);
        }
    }
    
    public DeviceMatcher createMatcher(final ControllerHost host) {
        if (uuid == null) {
            return host.createVST3DeviceMatcher(id);
        }
        return host.createBitwigDeviceMatcher(uuid);
    }
    
    public String getParamName(final ParameterType type) {
        return typeToParamName.get(type);
    }
    
    public Optional<ParameterType> getParamType(final String parameterName) {
        return Optional.ofNullable(paramNameToType.get(parameterName));
    }
    
}
