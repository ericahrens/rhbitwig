package com.allenheath.k2.set1;

import com.bitwig.extension.controller.api.ControllerHost;
import com.bitwig.extension.controller.api.CursorDevice;
import com.bitwig.extension.controller.api.DeviceMatcher;
import com.bitwig.extension.controller.api.SpecificPluginDevice;

public enum SpecialVst3Devices implements SpecialDevice {
	MEAGAVERB3("5653544D56335867687A206D65676176"), FIX_DOUBLER("048D51804B2049B0BFF8E66CA70F51EC ");

	private final String id;

	private SpecialVst3Devices(final String id) {
		this.id = id;
	};

	@Override
	public DeviceMatcher createDeviceMatcher(final ControllerHost host) {
		return host.createVST3DeviceMatcher(id);
	}

	@Override
	public SpecificPluginDevice createDevice(final CursorDevice cursorDevice) {
		return cursorDevice.createSpecificVst3Device(id);
	}

}
