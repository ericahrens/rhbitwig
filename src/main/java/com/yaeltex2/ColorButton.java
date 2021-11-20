package com.yaeltex2;

import java.util.function.Consumer;
import java.util.function.Supplier;

import com.bitwig.extension.controller.api.HardwareButton;
import com.bitwig.extension.controller.api.MidiIn;
import com.bitwig.extension.controller.api.MidiOut;
import com.bitwig.extension.controller.api.MultiStateHardwareLight;
import com.bitwig.extension.controller.api.SettableBooleanValue;
import com.bitwig.extensions.framework.Layer;
import com.bitwig.extensions.rh.Midi;

public class ColorButton {

	private final HardwareButton hwButton;
	private final int index;
	private final MultiStateHardwareLight light;
	private final int notevalue;

	public ColorButton(final ExtensionDriver driver, final String group, final int index, final int notvalue,
			final int channel) {
		this.index = index;
		this.notevalue = notvalue;
		final MidiIn midiIn = driver.getMidiIn();
		final MidiOut midiOut = driver.getMidiOut();
		hwButton = driver.getSurface().createHardwareButton(group + "_" + index);
		hwButton.pressedAction().setPressureActionMatcher(midiIn.createNoteOnVelocityValueMatcher(0, notevalue));
		hwButton.releasedAction().setActionMatcher(midiIn.createNoteOffActionMatcher(0, notevalue));
		hwButton.isPressed().markInterested();
		light = driver.getSurface().createMultiStateHardwareLight(group + "_LIGHT_" + index);
		hwButton.setBackgroundLight(light);

		light.state().onUpdateHardware(state -> {
			if (state instanceof ColorButtonLedState) {
				midiOut.sendMidi(Midi.NOTE_ON, notevalue, ((ColorButtonLedState) state).getColorCode());
			} else {
				midiOut.sendMidi(Midi.NOTE_ON, notevalue, 0);
			}
		});
	}

	public int getIndex() {
		return index;
	}

	public void bindPressed(final Layer layer, final Consumer<Boolean> target,
			final Supplier<ColorButtonLedState> colorProvider) {
		layer.bind(hwButton, hwButton.pressedAction(), () -> target.accept(true));
		layer.bind(hwButton, hwButton.releasedAction(), () -> target.accept(false));
		layer.bindLightState(() -> colorProvider.get(), light);
	}

	public void bindPressed(final Layer layer, final SettableBooleanValue value, final ColorButtonLedState onColor,
			final ColorButtonLedState offColor) {
		layer.bind(hwButton, hwButton.pressedAction(), () -> value.set(true));
		layer.bind(hwButton, hwButton.releasedAction(), () -> value.set(false));
		layer.bindLightState(() -> value.get() ? onColor : offColor, light);
	}

	public void bindToggle(final Layer layer, final SettableBooleanValue value, final ColorButtonLedState onColor,
			final ColorButtonLedState offColor) {
		layer.bind(hwButton, hwButton.pressedAction(), () -> value.toggle());
		layer.bindLightState(() -> value.get() ? onColor : offColor, light);
	}

}
