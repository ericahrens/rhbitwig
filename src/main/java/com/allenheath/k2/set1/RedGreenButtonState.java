package com.allenheath.k2.set1;

import java.util.Map;
import java.util.Objects;

import com.bitwig.extension.api.Color;
import com.bitwig.extension.controller.api.HardwareLightVisualState;
import com.bitwig.extension.controller.api.InternalHardwareLightState;

public class RedGreenButtonState extends InternalHardwareLightState {
	
	public static final RedGreenButtonState OFF = new RedGreenButtonState(RedGreenColor.OFF);
	public static final RedGreenButtonState RED = new RedGreenButtonState(RedGreenColor.RED);
	public static final RedGreenButtonState GREEN = new RedGreenButtonState(RedGreenColor.GREEN);
	public static final RedGreenButtonState YELLOW = new RedGreenButtonState(RedGreenColor.YELLOW);
	private static final Map<Color, RedGreenButtonState> TO_STATE_COLOR = Map.of(RedGreenColor.OFF.getColor(), OFF, //
		RedGreenColor.GREEN.getColor(), GREEN,//
		RedGreenColor.RED.getColor(), RED, //
		RedGreenColor.YELLOW.getColor(), YELLOW);

	private final RedGreenColor color;
		private RedGreenButtonState(final RedGreenColor color) {
		super();
		this.color = color;
	}

	public static RedGreenButtonState toState(Color color) {
 		return TO_STATE_COLOR.getOrDefault(color, OFF);
	}
	
	@Override
	public HardwareLightVisualState getVisualState() {
		return HardwareLightVisualState.createForColor(color.getColor());
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
	
	@Override
	public String toString() {
		return "Color %s".formatted(color);
	}
}
