package com.allenheath.k2.set1;

import com.bitwig.extension.controller.api.ControllerHost;
import com.bitwig.extension.controller.api.CursorDevice;
import com.bitwig.extension.controller.api.DeviceMatcher;
import com.bitwig.extension.controller.api.SpecificPluginDevice;

public interface SpecialDevice {
	DeviceMatcher createDeviceMatcher(final ControllerHost host);

	SpecificPluginDevice createDevice(final CursorDevice cursorDevice);

}
