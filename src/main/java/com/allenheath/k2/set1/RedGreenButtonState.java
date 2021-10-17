package com.allenheath.k2.set1;

import java.util.Objects;

import com.bitwig.extension.controller.api.HardwareLightVisualState;
import com.bitwig.extension.controller.api.InternalHardwareLightState;

public class RedGreenButtonState extends InternalHardwareLightState {

	private RedGreenColor color = RedGreenColor.OFF;
	public static final RedGreenButtonState OFF = new RedGreenButtonState(RedGreenColor.OFF);
	public static final RedGreenButtonState RED = new RedGreenButtonState(RedGreenColor.RED);
	public static final RedGreenButtonState GREEN = new RedGreenButtonState(RedGreenColor.GREEN);
	public static final RedGreenButtonState YELLOW = new RedGreenButtonState(RedGreenColor.YELLOW);

	private RedGreenButtonState(final RedGreenColor color) {
		super();
		this.color = color;
	}

	@Override
	public HardwareLightVisualState getVisualState() {
		return null;
	}

	public RedGreenColor getColor() {
		return color;
	}

	@Override
	public int hashCode() {
		return Objects.hash(color);
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
		final RedGreenButtonState other = (RedGreenButtonState) obj;
		return color == other.color;
	}

}
