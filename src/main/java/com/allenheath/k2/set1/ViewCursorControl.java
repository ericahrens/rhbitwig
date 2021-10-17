package com.allenheath.k2.set1;

import java.util.List;

import com.bitwig.extension.controller.api.ControllerHost;
import com.bitwig.extension.controller.api.CursorDevice;
import com.bitwig.extension.controller.api.CursorDeviceFollowMode;
import com.bitwig.extension.controller.api.CursorTrack;
import com.bitwig.extension.controller.api.DeviceBank;
import com.bitwig.extension.controller.api.DeviceMatcher;
import com.bitwig.extension.controller.api.DrumPad;
import com.bitwig.extension.controller.api.DrumPadBank;
import com.bitwig.extension.controller.api.PinnableCursorDevice;
import com.bitwig.extension.controller.api.Send;
import com.bitwig.extension.controller.api.Track;
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

	public ViewCursorControl(final ControllerHost host, final List<DirectParameterControl> parameterControls) {
		super();

		this.trackBank = host.createTrackBank(8, 8, 8);
		this.cursorTrack = host.createCursorTrack(8, 8);

		cursorTrack.clipLauncherSlotBank().cursorIndex().addValueObserver(index -> {
			// RemoteConsole.out.println(" => {}", index);
		});

		this.cursorTrack.color().addValueObserver((r, g, b) -> {
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
		for (int i = 0; i < 16; i++) {
			final DrumPad pad = drumPadBank.getItemAt(i);
			for (int j = 0; j < 8; j++) {
				final Send sendItem = pad.sendBank().getItemAt(j);
				sendItem.exists().markInterested();
				sendItem.value().markInterested();
			}
		}
		setUpDevices(host, parameterControls);
	}

	private void setUpDevices(final ControllerHost host, final List<DirectParameterControl> controls) {
		for (int i = 0; i < 8; i++) {
			final Track track = this.trackBank.getItemAt(i);
			final CursorDevice cursorDevice = track.createCursorDevice();
			for (final DirectParameterControl directParameterControl : controls) {
				setUpTrackDevice(i, track, cursorDevice, directParameterControl, host);
			}
		}
	}

	private void setUpTrackDevice(final int index, final Track track, final CursorDevice cursorDevice,
			final DirectParameterControl parameterControl, final ControllerHost host) {

		final DeviceBank db = track.createDeviceBank(1);
		db.setDeviceMatcher(parameterControl.getDeviceType().createDeviceMatcher(host));
		parameterControl.register(db.getItemAt(0), cursorDevice);
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
