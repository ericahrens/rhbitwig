package com.testdebug;

import java.util.UUID;

import com.bitwig.extension.api.PlatformType;
import com.bitwig.extension.controller.AutoDetectionMidiPortNamesList;
import com.bitwig.extension.controller.ControllerExtensionDefinition;
import com.bitwig.extension.controller.api.ControllerHost;

public class TestExtensionDefinition extends ControllerExtensionDefinition {
	private static final UUID DRIVER_ID = UUID.fromString("3c6b9cd4-0ffd-11ec-82a8-0242ac134263");

	public TestExtensionDefinition() {
	}

	@Override
	public String getName() {
		return "Test Extension";
	}

	@Override
	public String getAuthor() {
		return "E.Ahrens";
	}

	@Override
	public String getVersion() {
		return "0.1";
	}

	@Override
	public UUID getId() {
		return DRIVER_ID;
	}

	@Override
	public String getHardwareVendor() {
		return "ADebug";
	}

	@Override
	public String getHardwareModel() {
		return "Test Controller";
	}

	@Override
	public int getRequiredAPIVersion() {
		return 13;
	}

	@Override
	public int getNumMidiInPorts() {
		return 1;
	}

	@Override
	public int getNumMidiOutPorts() {
		return 1;
	}

	@Override
	public void listAutoDetectionMidiPortNames(final AutoDetectionMidiPortNamesList list,
			final PlatformType platformType) {
		final String[] inputNames = new String[1];
		final String[] outputNames = new String[1];

		switch (platformType) {
		case LINUX:
			inputNames[0] = "LPProMK3 MIDI";
			outputNames[0] = "LPProMK3 MIDI";
			break;

		case WINDOWS:
			inputNames[0] = "LPProMK3 MIDI";
			outputNames[0] = "LPProMK3 MIDI";
			break;

		case MAC:
			inputNames[0] = "LPProMK3 MIDI";
			outputNames[0] = "LPProMK3 MIDI";
			break;
		}

		list.add(inputNames, outputNames);
	}

	@Override
	public TestControllerExtension createInstance(final ControllerHost host) {
		return new TestControllerExtension(this, host);
	}
}
