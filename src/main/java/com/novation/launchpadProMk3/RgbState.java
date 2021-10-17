package com.novation.launchpadProMk3;

import java.util.Objects;

import com.bitwig.extension.controller.api.HardwareLightVisualState;
import com.bitwig.extension.controller.api.InternalHardwareLightState;

public class RgbState extends InternalHardwareLightState {

	private int colorIndex = 0;

	private LightState state = LightState.NORMAL;

	// public static final RgbState OFF = new RgbState(0, LightState.NORMAL);

	private static RgbState[] registry = new RgbState[128 * 3];

	public RgbState(final int colorIndex, final LightState state) {
		super();
		this.colorIndex = colorIndex;
		this.state = state;
	}

	public static RgbState of(final LpColor color) {
		return of(color.getIndex());
	}

	public static RgbState of(final int colorIndex, final LightState state) {
		final int index = colorIndex + state.getChannel() * 128;
		if (registry[index] == null) {
			registry[index] = new RgbState(colorIndex, state);
		}
		return registry[index];
	}

	public static RgbState of(final int colorIndex) {
		final int index = Math.min(Math.max(0, colorIndex), 127);
		if (registry[index] == null) {
			registry[index] = new RgbState(index, LightState.NORMAL);
		}
		return registry[index];
	}

	@Override
	public HardwareLightVisualState getVisualState() {
		return null;
	}

	public void setColorIndex(final int colorIndex) {
		this.colorIndex = colorIndex;
	}

	public int getColorIndex() {
		return colorIndex;
	}

	public LightState getState() {
		return state;
	}

	public void setState(final LightState state) {
		this.state = state;
	}

	@Override
	public int hashCode() {
		return Objects.hash(colorIndex, state);
	}

	@Override
	public boolean equals(final Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (getClass() != obj.getClass()) {
			return false;
		}
		final RgbState other = (RgbState) obj;
		return colorIndex == other.colorIndex && state == other.state;
	}

	public void off() {
		colorIndex = 0;
		state = LightState.NORMAL;
	}

}
