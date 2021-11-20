package com.akai.fire.control;

import java.util.function.Consumer;
import java.util.function.Supplier;

import com.akai.fire.AkaiFireDrumSeqExtension;
import com.akai.fire.NoteAssign;
import com.akai.fire.display.DisplayInfo;
import com.akai.fire.display.OledDisplay;
import com.akai.fire.lights.BiColorLightState;
import com.bitwig.extension.controller.api.HardwareButton;
import com.bitwig.extension.controller.api.MidiIn;
import com.bitwig.extension.controller.api.MultiStateHardwareLight;
import com.bitwig.extension.controller.api.SettableBooleanValue;
import com.bitwig.extensions.framework.Layer;

public class BiColorButton {
	private final HardwareButton hwButton;
	private final MultiStateHardwareLight light;
	private final NoteAssign noteAssign;

	public BiColorButton(final NoteAssign assignment, final AkaiFireDrumSeqExtension driver, final int ccValue) {
		final MidiIn midiIn = driver.getMidiIn();
		this.noteAssign = assignment;
		hwButton = driver.getSurface().createHardwareButton("BUTTON_" + assignment.toString());
		hwButton.pressedAction()
				.setPressureActionMatcher(midiIn.createNoteOnVelocityValueMatcher(0, assignment.getNoteValue()));
		hwButton.releasedAction().setActionMatcher(midiIn.createNoteOffActionMatcher(0, assignment.getNoteValue()));
		hwButton.isPressed().markInterested();
		light = driver.getSurface().createMultiStateHardwareLight("BUTTON_LIGHT_" + assignment.toString());
		hwButton.setBackgroundLight(light);
		light.state().onUpdateHardware(state -> {
			if (state instanceof BiColorLightState) {
				driver.sendCC(ccValue, ((BiColorLightState) state).getStateValue());
			} else {
				driver.sendCC(ccValue, 0);
			}
		});
	}

	public BiColorButton(final NoteAssign assignment, final AkaiFireDrumSeqExtension driver) {
		this(assignment, driver, assignment.getNoteValue());
	}

	public void markPressedInteressed() {
		hwButton.isPressed().markInterested();
	}

	public NoteAssign getNoteAssign() {
		return noteAssign;
	}

	public void bindPressed(final Layer layer, final Runnable action, final Supplier<BiColorLightState> lightSource) {
		layer.bind(hwButton, hwButton.pressedAction(), () -> action.run());
		hwButton.isPressed().markInterested();
		layer.bindLightState(() -> lightSource.get(), light);
	}

	public void bindPressed(final Layer layer, final Consumer<Boolean> target,
			final Supplier<BiColorLightState> lightSource) {
		layer.bind(hwButton, hwButton.pressedAction(), () -> target.accept(true));
		layer.bind(hwButton, hwButton.releasedAction(), () -> target.accept(false));
		layer.bindLightState(() -> lightSource.get(), light);
	}

	public void bindPressed(final Layer layer, final Consumer<Boolean> target, final BiColorLightState onColor,
			final BiColorLightState offColor) {
		layer.bind(hwButton, hwButton.pressedAction(), () -> target.accept(true));
		layer.bind(hwButton, hwButton.releasedAction(), () -> target.accept(false));
		hwButton.isPressed().markInterested();
		layer.bindLightState(() -> hwButton.isPressed().get() ? onColor : offColor, light);
	}

	public void bind(final Layer layer, final Runnable action, final BiColorLightState onColor,
			final BiColorLightState offColor) {
		layer.bind(hwButton, hwButton.pressedAction(), () -> action.run());
		hwButton.isPressed().markInterested();
		layer.bindLightState(() -> hwButton.isPressed().get() ? onColor : offColor, light);
	}

	public void bind(final Layer layer, final SettableBooleanValue value, final BiColorLightState onColor,
			final BiColorLightState offColor) {
		layer.bind(hwButton, hwButton.pressedAction(), () -> value.set(true));
		layer.bind(hwButton, hwButton.releasedAction(), () -> value.set(false));
		layer.bindLightState(() -> value.get() ? onColor : offColor, light);
	}

	public void bindToggle(final Layer layer, final SettableBooleanValue value, final BiColorLightState onColor,
			final BiColorLightState offColor) {
		layer.bind(hwButton, hwButton.pressedAction(), () -> value.toggle());
		value.markInterested();
		layer.bindLightState(() -> value.get() ? onColor : offColor, light);
	}

	public void bindPressed(final Layer layer, final Consumer<Boolean> target, final BiColorLightState onColor) {
		layer.bind(hwButton, hwButton.pressedAction(), () -> target.accept(true));
		layer.bind(hwButton, hwButton.releasedAction(), () -> target.accept(false));
		layer.bindLightState(() -> hwButton.isPressed().get() ? onColor : BiColorLightState.OFF, light);
	}

	public boolean isPressed() {
		return hwButton.isPressed().get();
	}

	public void bindToggle(final Layer layer, final SettableBooleanValue value, final BiColorLightState onColor,
			final BiColorLightState offColor, final OledDisplay display, final DisplayInfo info) {
		layer.bind(hwButton, hwButton.pressedAction(), () -> {
			value.toggle();
			display.showInfo(info);
		});
		layer.bind(hwButton, hwButton.releasedAction(), () -> display.clearScreenDelayed());
		value.markInterested();
		layer.bindLightState(() -> value.get() ? onColor : offColor, light);
	}

}
