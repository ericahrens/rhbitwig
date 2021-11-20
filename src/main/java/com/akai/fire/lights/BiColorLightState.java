package com.akai.fire.lights;

import java.util.Objects;

import com.bitwig.extension.controller.api.HardwareLightVisualState;
import com.bitwig.extension.controller.api.InternalHardwareLightState;

public class BiColorLightState extends InternalHardwareLightState {

	private final byte stateValue;

	public static final BiColorLightState OFF = new BiColorLightState(0);
	public static final BiColorLightState HALF = new BiColorLightState(1);
	public static final BiColorLightState FULL = new BiColorLightState(127);
	public static final BiColorLightState GREEN_HALF = new BiColorLightState(1);
	public static final BiColorLightState RED_HALF = new BiColorLightState(1);
	public static final BiColorLightState AMBER_HALF = new BiColorLightState(2);
	public static final BiColorLightState GREEN_FULL = new BiColorLightState(3);
	public static final BiColorLightState RED_FULL = new BiColorLightState(3);
	public static final BiColorLightState AMBER_FULL = new BiColorLightState(4);
	public static final BiColorLightState MODE_CHANNEL = new BiColorLightState(0);
	public static final BiColorLightState MODE_MIXER = new BiColorLightState(1);
	public static final BiColorLightState MODE_USER1 = new BiColorLightState(2);
	public static final BiColorLightState MODE_USER2 = new BiColorLightState(3);
	public static final BiColorLightState MODE_USER3 = new BiColorLightState(0x13);

	private BiColorLightState(final int stateValue) {
		super();
		this.stateValue = (byte) stateValue;
	}

	public byte getStateValue() {
		return stateValue;
	}

	@Override
	public HardwareLightVisualState getVisualState() {
		return null;
	}

	@Override
	public int hashCode() {
		return Objects.hash(stateValue);
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
		final BiColorLightState other = (BiColorLightState) obj;
		return stateValue == other.stateValue;
	}

}
