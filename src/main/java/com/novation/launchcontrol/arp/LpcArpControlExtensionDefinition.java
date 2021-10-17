package com.novation.launchcontrol.arp;

import java.util.UUID;

import com.bitwig.extension.api.PlatformType;
import com.bitwig.extension.controller.AutoDetectionMidiPortNamesList;
import com.bitwig.extension.controller.ControllerExtensionDefinition;
import com.bitwig.extension.controller.api.ControllerHost;

public class LpcArpControlExtensionDefinition extends ControllerExtensionDefinition {
	private static final UUID DRIVER_ID = UUID.fromString("41aa8c3d-b499-4cc6-b480-6ef30948f37a");

	public LpcArpControlExtensionDefinition() {
	}

	@Override
	public String getName() {
		return "LpcArpControl";
	}

	@Override
	public String getAuthor() {
		return "R.Hawtin/E.Ahrens";
	}

	@Override
	public String getVersion() {
		return "0.2";
	}

	@Override
	public UUID getId() {
		return DRIVER_ID;
	}

	@Override
	public String getHardwareVendor() {
		return "Novation";
	}

	@Override
	public String getHardwareModel() {
		return "LpcArpControl";
	}

	@Override
	public int getRequiredAPIVersion() {
		return 12;
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
		if (platformType == PlatformType.WINDOWS) {
			list.add(new String[] { "Launch Control XL" }, new String[] { "Launch Control XL" });
		} else if (platformType == PlatformType.MAC) {
			list.add(new String[] { "Launch Control XL" }, new String[] { "Launch Control XL" });
		} else if (platformType == PlatformType.LINUX) {
			list.add(new String[] { "Launch Control XL" }, new String[] { "Launch Control XL" });
		}
	}

	@Override
	public LpcArpControlExtension createInstance(final ControllerHost host) {
		return new LpcArpControlExtension(this, host);
	}
}
