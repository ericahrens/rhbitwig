package com.akai.fire;

import com.bitwig.extension.controller.api.ControllerHost;
import com.bitwig.extension.controller.api.CursorDeviceFollowMode;
import com.bitwig.extension.controller.api.CursorTrack;
import com.bitwig.extension.controller.api.DeviceBank;
import com.bitwig.extension.controller.api.DeviceMatcher;
import com.bitwig.extension.controller.api.DrumPadBank;
import com.bitwig.extension.controller.api.PinnableCursorDevice;
import com.bitwig.extension.controller.api.TrackBank;
import com.bitwig.extensions.rh.SpecialDevices;

public class ViewCursorControl {
	private final CursorTrack cursorTrack;
	private final DeviceBank deviceBank;
	private final PinnableCursorDevice primaryDevice;
	private final DeviceBank drumBank;
	private final DrumPadBank drumPadBank;
	private final TrackBank trackBank;
	// private final Device drumDevice;

	public ViewCursorControl(final ControllerHost host, final int sends) {
		super();

		this.trackBank = host.createTrackBank(8, 8, sends);
		this.cursorTrack = host.createCursorTrack(8, sends);

		cursorTrack.clipLauncherSlotBank().cursorIndex().addValueObserver(index -> {
			// RemoteConsole.out.println(" => {}", index);
		});

		deviceBank = cursorTrack.createDeviceBank(8);
		primaryDevice = cursorTrack.createCursorDevice("drumdetection", "Pad Device", 8,
				CursorDeviceFollowMode.FIRST_INSTRUMENT);
		primaryDevice.hasDrumPads().markInterested();
		primaryDevice.exists().markInterested();
		final DeviceMatcher drumMatcher = host.createBitwigDeviceMatcher(SpecialDevices.DRUM.getUuid());
		drumBank = cursorTrack.createDeviceBank(1);
		drumBank.setDeviceMatcher(drumMatcher);
		// drumDevice = drumBank.getItemAt(0);
		drumPadBank = primaryDevice.createDrumPadBank(16);
	}

	public TrackBank getTrackBank() {
		return trackBank;
	}

	public CursorTrack getCursorTrack() {
		return cursorTrack;
	}

	public DeviceBank getDeviceBank() {
		return deviceBank;
	}

	public PinnableCursorDevice getPrimaryDevice() {
		return primaryDevice;
	}

	public DeviceBank getDrumBank() {
		return drumBank;
	}

	public DrumPadBank getDrumPadBank() {
		return drumPadBank;
	}

}
