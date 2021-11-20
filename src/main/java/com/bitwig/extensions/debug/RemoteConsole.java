package com.bitwig.extensions.debug;

import com.bitwig.extension.controller.api.ControllerHost;

public interface RemoteConsole {
	public static final RemoteConsole out = new RemoteConsoleActive();

	void registerHost(final ControllerHost host);

	void printSysEx(String prefix, byte[] data);

	void println(String format, Object... params);

	String getStackTrace(final int max);
}