package com.yaeltex2;

import java.util.UUID;

import com.bitwig.extension.api.PlatformType;
import com.bitwig.extension.controller.AutoDetectionMidiPortNamesList;
import com.bitwig.extension.controller.ControllerExtensionDefinition;
import com.bitwig.extension.controller.api.ControllerHost;

public class YaeltexSeqArp168Definition extends ControllerExtensionDefinition {
	private static final UUID DRIVER_ID = UUID.fromString("7e675975-44be-4396-8814-14521805c242");

	public YaeltexSeqArp168Definition() {
	}

	@Override
	public String getName() {
		return "SEQ ARP 168 Test";
	}

	@Override
	public String getAuthor() {
		return "E.Ahrens";
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
		return "Yaeltex";
	}

	@Override
	public String getHardwareModel() {
		return "SEQARP168";
	}

	@Override
	public int getRequiredAPIVersion() {
		return 14;
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
			list.add(new String[] { "SEQARP168" }, new String[] { "SEQARP168" });
		} else if (platformType == PlatformType.MAC) {
			list.add(new String[] { "SEQARP168" }, new String[] { "SEQARP168" });
		} else if (platformType == PlatformType.LINUX) {
			list.add(new String[] { "SEQARP168" }, new String[] { "SEQARP168" });
		}
	}

	@Override
	public YaeltexSeqArp168Extension createInstance(final ControllerHost host) {
		return new YaeltexSeqArp168Extension(this, host);
	}
}
