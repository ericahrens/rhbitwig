package com.yaeltex.common;

import com.bitwig.extension.controller.api.HardwareLightVisualState;
import com.bitwig.extension.controller.api.InternalHardwareLightState;

public class YaeltexButtonLedState extends InternalHardwareLightState {

	public static final YaeltexButtonLedState OFF = new YaeltexButtonLedState(YaelTexColors.OFF);
	public static final YaeltexButtonLedState RED = new YaeltexButtonLedState(YaelTexColors.RED);
	public static final YaeltexButtonLedState RED_DIM = new YaeltexButtonLedState(YaelTexColors.RED, 2);
	public static final YaeltexButtonLedState BLUE = new YaeltexButtonLedState(YaelTexColors.BLUE);
	public static final YaeltexButtonLedState BLUE_ACTIVE = new YaeltexButtonLedState(YaelTexColors.BLUE, 2);
	public static final YaeltexButtonLedState AQUA = new YaeltexButtonLedState(YaelTexColors.AQUA);
	public static final YaeltexButtonLedState PURPLE = new YaeltexButtonLedState(YaelTexColors.PURPLE);
	public static final YaeltexButtonLedState ORANGE = new YaeltexButtonLedState(YaelTexColors.ORANGE);
	public static final YaeltexButtonLedState ORANGE_DIM = new YaeltexButtonLedState(YaelTexColors.ORANGE, 1);
	public static final YaeltexButtonLedState YELLOW = new YaeltexButtonLedState(YaelTexColors.YELLOW);
	public static final YaeltexButtonLedState YELLOW_DIM = new YaeltexButtonLedState(YaelTexColors.BRIGHT_YELLOW, 1);
	public static final YaeltexButtonLedState GREEN = new YaeltexButtonLedState(YaelTexColors.GREEN, 0);
	public static final YaeltexButtonLedState DEEP_GREEN = new YaeltexButtonLedState(YaelTexColors.DEEP_GREEN, 0);
	public static final YaeltexButtonLedState WHITE = new YaeltexButtonLedState(YaelTexColors.WHITE, 0);

	private int colorCode = 0;

	private static final YaeltexButtonLedState[] colorMap = new YaeltexButtonLedState[128];

	public static YaeltexButtonLedState of(final int index) {
		assert index < 128;
		assert index >= 0;
		YaeltexButtonLedState color = colorMap[index];
		if (color == null) {
			color = new YaeltexButtonLedState(index);
			colorMap[index] = color;
		}
		return color;
	}

	public static YaeltexButtonLedState of(final YaelTexColors colorType, final int offset) {
		final int index = colorType.getValue() + offset;
		assert index < 128;
		assert index >= 0;
		YaeltexButtonLedState color = colorMap[index];
		if (color == null) {
			color = new YaeltexButtonLedState(index);
			colorMap[index] = color;
		}
		return color;
	}

	private YaeltexButtonLedState(final YaelTexColors colorCode) {
		super();
		this.colorCode = colorCode.getValue();
	}
	
	private YaeltexButtonLedState(final YaelTexColors colorCode, final int offset) {
		super();
		assert offset < 3;
		this.colorCode = colorCode.getValue() + offset;
	}
	
	private YaeltexButtonLedState(final int colorCode) {
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
		return obj instanceof YaeltexButtonLedState && equals((YaeltexButtonLedState) obj);
	}

	public boolean equals(final YaeltexButtonLedState obj) {
		if (obj == this) {
			return true;
		}

		return colorCode == obj.colorCode;
	}

}
