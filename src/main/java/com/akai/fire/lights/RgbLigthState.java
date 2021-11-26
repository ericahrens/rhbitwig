package com.akai.fire.lights;

import java.util.Objects;

import com.bitwig.extension.controller.api.HardwareLightVisualState;
import com.bitwig.extension.controller.api.InternalHardwareLightState;

public class RgbLigthState extends InternalHardwareLightState {

	private static final int ULTRA_DIM_FACTOR = 15;
	private static final int DIM_FACTOR = 4;
	private static final int BRIGHT_FACTOR = 20;
	private static final int MAX_BRIGHT_FACTOR = 50;
	public static final RgbLigthState OFF = new RgbLigthState(0, 0, 0, true);
	public static final RgbLigthState PURPLE = new RgbLigthState(80, 0, 80, true);
	public static final RgbLigthState WHITE = new RgbLigthState(100, 100, 100, true);
	public static final RgbLigthState GRAY_1 = new RgbLigthState(10, 10, 10, true);
	public static final RgbLigthState GRAY_2 = new RgbLigthState(40, 40, 40, true);

	private final byte red;
	private final byte green;
	private final byte blue;

	private RgbLigthState veryDimmed;
	private RgbLigthState dimmed;
	private RgbLigthState brightend;
	private RgbLigthState brightest;

	public RgbLigthState(final int red, final int green, final int blue, final boolean variants) {
		this((byte) red, (byte) green, (byte) blue, variants);
	}

	public RgbLigthState(final byte red, final byte green, final byte blue) {
		this(red, green, blue, true);
	}

	private RgbLigthState(final byte red, final byte green, final byte blue, final boolean variants) {
		super();
		this.red = red;
		this.green = green;
		this.blue = blue;
		if (variants) {
			dimmed = new RgbLigthState(red / DIM_FACTOR, green / DIM_FACTOR, blue / DIM_FACTOR, false);
			veryDimmed = new RgbLigthState(red / ULTRA_DIM_FACTOR, green / ULTRA_DIM_FACTOR, blue / ULTRA_DIM_FACTOR,
					false);
			brightend = new RgbLigthState(Math.min(red + BRIGHT_FACTOR, 127), Math.min(green + BRIGHT_FACTOR, 127),
					Math.min(blue + BRIGHT_FACTOR, 127), false);
			brightest = new RgbLigthState(Math.min(red + MAX_BRIGHT_FACTOR, 127),
					Math.min(green + MAX_BRIGHT_FACTOR, 127), Math.min(blue + MAX_BRIGHT_FACTOR, 127), false);
		}
	}

	@Override
	public HardwareLightVisualState getVisualState() {
		return null;
	}

	public RgbLigthState getVeryDimmed() {
		return veryDimmed != null ? veryDimmed : this;
	}

	public RgbLigthState getDimmed() {
		return dimmed != null ? dimmed : this;
	}

	public RgbLigthState getBrightend() {
		return brightend != null ? brightend : this;
	}

	public RgbLigthState getBrightest() {
		return brightest != null ? brightest : this;
	}

	@Override
	public int hashCode() {
		return Objects.hash(blue, green, red);
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
		final RgbLigthState other = (RgbLigthState) obj;
		return blue == other.blue && green == other.green && red == other.red;
	}

	public byte getRed() {
		return red;
	}

	public byte getBlue() {
		return blue;
	}

	public byte getGreen() {
		return green;
	}

}
