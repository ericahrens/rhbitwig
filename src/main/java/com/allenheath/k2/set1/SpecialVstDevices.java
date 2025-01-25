package com.allenheath.k2.set1;

import com.bitwig.extension.controller.api.ControllerHost;
import com.bitwig.extension.controller.api.CursorDevice;
import com.bitwig.extension.controller.api.DeviceMatcher;
import com.bitwig.extension.controller.api.SpecificPluginDevice;

public enum SpecialVstDevices implements SpecialDevice {
	//LEXICON_PSP(1347630130); Disabling the old Lexicon_PSP42 otherwise Lexicon_PSP42x 2.0.2 will not work, or the compiling will crash
;
	private final int id;

	private SpecialVstDevices(final int id) {
		this.id = id;
	};

	@Override
	public DeviceMatcher createDeviceMatcher(final ControllerHost host) {
		return host.createVST2DeviceMatcher(id);
	}

	@Override
	public SpecificPluginDevice createDevice(final CursorDevice cursorDevice) {
		return cursorDevice.createSpecificVst2Device(id);
	}

}
