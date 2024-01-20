package com.yaeltex.fuse;

import com.yaeltex.common.YaeltexButtonLedState;

import java.util.Arrays;

public enum SendMode {
    AUTO("AUTO", YaeltexButtonLedState.OFF),
    PRE("PRE", YaeltexButtonLedState.WHITE),
    POST("POST", YaeltexButtonLedState.AQUA);
    private final String enumRaw;
    private final YaeltexButtonLedState color;

    SendMode(final String enumRaw, final YaeltexButtonLedState color) {
        this.enumRaw = enumRaw;
        this.color = color;
    }

    public YaeltexButtonLedState getColor() {
        return color;
    }

    public String getEnumRaw() {
        return enumRaw;
    }

    public static SendMode toMode(final String enumRaw) {
        return Arrays.stream(SendMode.values())
                .filter(mode -> mode.enumRaw.equals(enumRaw))
                .findFirst()
                .orElse(SendMode.AUTO);
    }

    public SendMode toggle() {
        return switch (this) {
            case AUTO, POST -> PRE;
            case PRE -> POST;
        };
    }


}
