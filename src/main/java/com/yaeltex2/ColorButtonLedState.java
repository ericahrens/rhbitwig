package com.yaeltex2;

import com.bitwig.extension.controller.api.HardwareLightVisualState;
import com.bitwig.extension.controller.api.InternalHardwareLightState;

public class ColorButtonLedState extends InternalHardwareLightState {

	public static final ColorButtonLedState OFF = new ColorButtonLedState(YaelTexColors.OFF);
	public static final ColorButtonLedState RED = new ColorButtonLedState(YaelTexColors.RED);
	public static final ColorButtonLedState RED_DIM = new ColorButtonLedState(YaelTexColors.RED, 1);
	public static final ColorButtonLedState BLUE = new ColorButtonLedState(YaelTexColors.BLUE);
	public static final ColorButtonLedState BLUE_ACTIVE = new ColorButtonLedState(YaelTexColors.BLUE, 2);
	public static final ColorButtonLedState AQUA = new ColorButtonLedState(YaelTexColors.AQUA);
	public static final ColorButtonLedState AQUA_DIM = new ColorButtonLedState(YaelTexColors.AQUA, 2);
	public static final ColorButtonLedState PURPLE = new ColorButtonLedState(YaelTexColors.PURPLE);
	public static final ColorButtonLedState ORANGE = new ColorButtonLedState(YaelTexColors.ORANGE);
	public static final ColorButtonLedState ORANGE_DIM = new ColorButtonLedState(YaelTexColors.ORANGE, 1);
	public static final ColorButtonLedState YELLOW = new ColorButtonLedState(YaelTexColors.YELLOW);
	public static final ColorButtonLedState YELLOW_DIM = new ColorButtonLedState(YaelTexColors.BRIGHT_YELLOW, 1);
	public static final ColorButtonLedState GREEN = new ColorButtonLedState(YaelTexColors.GREEN, 0);
	public static final ColorButtonLedState DEEP_GREEN = new ColorButtonLedState(YaelTexColors.DEEP_GREEN, 0);
	public static final ColorButtonLedState WHITE = new ColorButtonLedState(YaelTexColors.WHITE, 0);
	public static final ColorButtonLedState WHITE_DIM = new ColorButtonLedState(YaelTexColors.WHITE, -1);

	private int colorCode = 0;

	private static ColorButtonLedState[] colorMap = new ColorButtonLedState[128];

	public static ColorButtonLedState colorFor(final int index) {
		assert index < 128;
		assert index >= 0;
		ColorButtonLedState color = colorMap[index];
		if (color == null) {
			color = new ColorButtonLedState(index);
			colorMap[index] = color;
		}
		return color;
	}

	public static ColorButtonLedState colorFor(final YaelTexColors ycolor, final int offset) {
		final int index = ycolor.getValue() + offset;
		assert index < 128;
		assert index >= 0;
		ColorButtonLedState color = colorMap[index];
		if (color == null) {
			color = new ColorButtonLedState(index);
			colorMap[index] = color;
		}
		return color;
	}

	public ColorButtonLedState(final YaelTexColors colorCode) {
		super();
		this.colorCode = colorCode.getValue();
	}

	public ColorButtonLedState(final YaelTexColors colorCode, final int offset) {
		super();
		assert offset < 3;
		this.colorCode = colorCode.getValue() + offset;
	}

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
