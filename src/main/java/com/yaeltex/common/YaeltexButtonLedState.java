package com.yaeltex.common;

import com.bitwig.extension.api.Color;
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

	private final int colorCode;
	private final Color color;

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

	private YaeltexButtonLedState(final YaelTexColors color) {
		this(color,0);
	}
	
	private YaeltexButtonLedState(final YaelTexColors color, final int offset) {
		super();
		assert offset < 3;
		this.colorCode = color.getValue() + offset;
		this.color = ColorUtil.getColor(this.colorCode);
	}
	
	private YaeltexButtonLedState(final int colorCode) {
		super();
		this.colorCode = colorCode;
		color = ColorUtil.getColor(this.colorCode);
	}

	public int getColorCode() {
		return colorCode;
	}

	@Override
	public HardwareLightVisualState getVisualState() {
		return  HardwareLightVisualState.createForColor(color);
	}
	
	public Color getColor() {
		return color;
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
