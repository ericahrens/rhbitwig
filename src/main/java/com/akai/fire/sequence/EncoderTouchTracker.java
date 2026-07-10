package com.akai.fire.sequence;

import java.util.LinkedHashMap;

public class EncoderTouchTracker {
    private final LinkedHashMap<Integer, String> activeEncoders = new LinkedHashMap<>();
    private int currentActiveEncoder = -1;
    private String currentActiveParameterName;

    public boolean beginTouch(final int encoderIndex, final String parameterName) {
        activeEncoders.remove(encoderIndex);
        activeEncoders.put(encoderIndex, parameterName);
        currentActiveEncoder = encoderIndex;
        currentActiveParameterName = parameterName;
        return true;
    }

    public boolean endTouch(final int encoderIndex) {
        if (!activeEncoders.containsKey(encoderIndex)) {
            return false;
        }
        activeEncoders.remove(encoderIndex);
        if (currentActiveEncoder == encoderIndex) {
            currentActiveEncoder = -1;
            currentActiveParameterName = null;
        }
        return currentActiveEncoder != -1;
    }

    public boolean isAnyTouchActive() {
        return !activeEncoders.isEmpty();
    }

    public int getCurrentActiveEncoder() {
        return currentActiveEncoder;
    }

    public String getCurrentActiveParameterName() {
        return currentActiveParameterName;
    }

    public void reset() {
        activeEncoders.clear();
        currentActiveEncoder = -1;
        currentActiveParameterName = null;
    }
}
