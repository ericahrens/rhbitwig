package com.novation.launchcontrol.arp;

import com.bitwig.extension.controller.api.HardwareLightVisualState;
import com.bitwig.extension.controller.api.InternalHardwareLightState;

public class ColorButtonLedState extends InternalHardwareLightState {

	public static final ColorButtonLedState OFF = new ColorButtonLedState(0x0C);
	public static final ColorButtonLedState RED_FULL = new ColorButtonLedState(0x0F);
	public static final ColorButtonLedState RED_SEMI = new ColorButtonLedState(0x0E);
	public static final ColorButtonLedState RED_DIM = new ColorButtonLedState(0x0D);
	public static final ColorButtonLedState AMBER_FULL = new ColorButtonLedState(0x3F);
	public static final ColorButtonLedState AMBER_SEMI = new ColorButtonLedState(0x2E);
	public static final ColorButtonLedState AMBER_DIM = new ColorButtonLedState(0x1D);

	public static final ColorButtonLedState YELLOW_FULL = new ColorButtonLedState(0x3E);
	public static final ColorButtonLedState GREEN_FULL = new ColorButtonLedState(0x3C);
	public static final ColorButtonLedState GREEN_DIM = new ColorButtonLedState(0x1C);

	public static final ColorButtonLedState EMERALD_DIM = new ColorButtonLedState(0x2D);
	public static final ColorButtonLedState GREEN_SEMI = new ColorButtonLedState(0x2C);
	public static final ColorButtonLedState ORANGE_FULL = new ColorButtonLedState(0x2F);
	public static final ColorButtonLedState ORANGE_DIM = new ColorButtonLedState(0x1E);

	public static final ColorButtonLedState GREEN_FLASH = new ColorButtonLedState(0x38);
	public static final ColorButtonLedState RED_FLASH = new ColorButtonLedState(0x0B);
	public static final ColorButtonLedState YELLOW_FLASH = new ColorButtonLedState(0x3A);
	public static final ColorButtonLedState AMBER_FLASH = new ColorButtonLedState(0x3B);

	private int colorCode = 0;

	public ColorButtonLedState(final int colorCode) {
		super();
		this.colorCode = colorCode;
	}

	public int getColorCode() {
		return colorCode;
	}

	@Override
	public HardwareLightVisualState getVisualState() {
		return null;
	}

	@Override
	public boolean equals(final Object obj) {
		return obj instanceof ColorButtonLedState && equals((ColorButtonLedState) obj);
	}

	public boolean equals(final ColorButtonLedState obj) {
		if (obj == this) {
			return true;
		}

		return colorCode == obj.colorCode;
	}

}
