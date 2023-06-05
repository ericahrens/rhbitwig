package com.bitwig.extensions.debug;

import com.bitwig.extension.controller.api.ControllerHost;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class DebugConsole {

    private static final DateTimeFormatter DF = DateTimeFormatter.ofPattern("hh:mm:ss SSS");
    private static ControllerHost host;

    public static void println(final String format, final Object... args) {
        if (host != null) {
            final LocalDateTime now = LocalDateTime.now();
            host.println(now.format(DF) + " > " + String.format(format, args));
        }
    }

    public static void registerHost(final ControllerHost host) {
        DebugConsole.host = host;
    }
}
