package com.bitwig.extensions.debug;

import com.bitwig.extension.controller.api.ControllerHost;

public class DisabledRemoteConsole implements RemoteConsole {

	@Override
	public void printSysEx(final String prefix, final byte[] data) {
	}

	@Override
	public void println(final String format, final Object... params) {
	}

	@Override
	public String getStackTrace(final int max) {
		return "";
	}

	@Override
	public void registerHost(final ControllerHost host) {
	}

}
