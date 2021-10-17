package com.novation.launchcontrol.arp;

import com.bitwig.extension.controller.api.CursorTrack;
import com.bitwig.extension.controller.api.Parameter;
import com.bitwig.extension.controller.api.PinnableCursorDevice;
import com.bitwig.extension.controller.api.SpecificBitwigDevice;

public class TimingButtonMode extends ButtonModeLayer {

	private final Parameter arpRate;
	private final Parameter shuffle;
	private final Parameter rateMode;
	private final Parameter retrigger;

	public TimingButtonMode(final LpcArpControlExtension driver, final String name) {
		super(driver, name);
		final SpecificBitwigDevice arpdevice = driver.getArpdevice();
		final PinnableCursorDevice cursorDevice = driver.getCursorDevice();
		cursorDevice.isPinned().markInterested();
		final CursorTrack currentTrack = driver.getCursorTrack();
		currentTrack.isPinned().markInterested();
		rateMode = arpdevice.createParameter("RATE_MODE");
		shuffle = arpdevice.createParameter("SHUFFLE");
		arpRate = arpdevice.createParameter("RATE");
		retrigger = arpdevice.createParameter("RETRIGGER");
		rateMode.markInterested();
		shuffle.markInterested();
		arpRate.markInterested();
		retrigger.markInterested();
		final RedGreenButton[] focusButtons = driver.getFocusButtons();
		bindPressed(focusButtons[0], () -> rateMode.setRaw(0));
		bindPressed(focusButtons[1], () -> rateMode.setRaw(1));
		bindPressed(focusButtons[2], () -> rateMode.setRaw(2));
		bindPressed(focusButtons[4], () -> {
			if (shuffle.getRaw() == 0) {
				shuffle.setRaw(1);
			} else {
				shuffle.setRaw(0);
			}
		});
		bindLightState(() -> rateMode.getRaw() == 0 ? ColorButtonLedState.AMBER_FULL : ColorButtonLedState.AMBER_DIM,
				focusButtons[0]);
		bindLightState(() -> rateMode.getRaw() == 1 ? ColorButtonLedState.AMBER_FULL : ColorButtonLedState.AMBER_DIM,
				focusButtons[1]);
		bindLightState(() -> rateMode.getRaw() == 2 ? ColorButtonLedState.AMBER_FULL : ColorButtonLedState.AMBER_DIM,
				focusButtons[2]);
		bindLightState(() -> shuffle.getRaw() == 0 ? ColorButtonLedState.ORANGE_FULL : ColorButtonLedState.AMBER_DIM,
				focusButtons[4]);
		bindLightState(() -> {
			return cursorDevice.isPinned().get() ? ColorButtonLedState.RED_FULL : ColorButtonLedState.RED_DIM;
		}, focusButtons[7]);

		bindPressed(focusButtons[7], () -> {
			final boolean pinned = cursorDevice.isPinned().get();
			cursorDevice.isPinned().set(!pinned);
			currentTrack.isPinned().set(!pinned);
		});
		bindPressed(focusButtons[1], () -> rateMode.setRaw(1));
		bindPressed(focusButtons[2], () -> rateMode.setRaw(2));

		bindPressed(focusButtons[3], () -> {
		});
		bindLightState(() -> ColorButtonLedState.OFF, focusButtons[3]);
		bindPressed(focusButtons[6], () -> {
		});
		bindLightState(() -> ColorButtonLedState.OFF, focusButtons[6]);

		final RedGreenButton[] controlButtons = driver.getControlButtons();
		for (int i = 0; i < 7; i++) {
			final int index = i;
			bindPressed(controlButtons[i], () -> arpRate.setRaw(index));
			bindLightState(
					() -> arpRate.getRaw() == index ? ColorButtonLedState.GREEN_FULL : ColorButtonLedState.GREEN_DIM,
					controlButtons[i]);
		}
		bindLightState(() -> retrigger.getRaw() == 1 ? ColorButtonLedState.ORANGE_FULL : ColorButtonLedState.AMBER_DIM,
				controlButtons[7]);
		bindPressed(controlButtons[7], () -> {
			if (retrigger.getRaw() == 0) {
				retrigger.setRaw(1);
			} else {
				retrigger.setRaw(0);
			}
		});

	}

	@Override
	public void doActivate() {
		super.doActivate();
		rateMode.subscribe();
		shuffle.subscribe();
		arpRate.subscribe();
		retrigger.subscribe();
	}

	@Override
	public void doDeactivate() {
		super.doDeactivate();
		rateMode.unsubscribe();
		shuffle.unsubscribe();
		arpRate.unsubscribe();
		retrigger.unsubscribe();
	}

}
