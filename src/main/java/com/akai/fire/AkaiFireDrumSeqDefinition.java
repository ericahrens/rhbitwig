package com.akai.fire;

import java.util.UUID;

import com.bitwig.extension.api.PlatformType;
import com.bitwig.extension.controller.AutoDetectionMidiPortNamesList;
import com.bitwig.extension.controller.ControllerExtensionDefinition;
import com.bitwig.extension.controller.api.ControllerHost;

public class AkaiFireDrumSeqDefinition extends ControllerExtensionDefinition {
	private static final UUID DRIVER_ID = UUID.fromString("24d0db9d-1951-406f-bdcf-d42c32d3d394");

	public AkaiFireDrumSeqDefinition() {
	}

	@Override
	public String getName() {
		return "Akai Fire Drum Seqencer";
	}

	@Override
	public String getAuthor() {
		return "E.Ahrens/R.Hawtin";
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
		return "Akai";
	}

	@Override
	public String getHardwareModel() {
		return "Akai Fire";
	}

	@Override
	public int getRequiredAPIVersion() {
		return 15;
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
			list.add(new String[] { "FL STUDIO FIRE" }, new String[] { "FL STUDIO FIRE" });
		} else if (platformType == PlatformType.MAC) {
			list.add(new String[] { "FL STUDIO FIRE" }, new String[] { "FL STUDIO FIRE" });
		} else if (platformType == PlatformType.LINUX) {
			list.add(new String[] { "FL STUDIO FIRE" }, new String[] { "FL STUDIO FIRE" });
		}
	}

	@Override
	public AkaiFireDrumSeqExtension createInstance(final ControllerHost host) {
		return new AkaiFireDrumSeqExtension(this, host);
	}
}
