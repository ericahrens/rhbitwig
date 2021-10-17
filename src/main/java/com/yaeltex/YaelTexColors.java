package com.yaeltex;

public enum YaelTexColors {
	OFF(0), RED(1), DARK_ORANGE(10), ORANGE(13), AMBER(16), YELLOW(19), //
	BRIGHT_YELLOW(22), GREEN_YELLOW(25), LIME(28), BRIGHT_GREEN(31), //
	GREEN(34), DEEP_GREEN(37), SPING_GREEN(52), MEDIUM_SPING_GREEN(55), //
	TURQUOISE(58), AQUA(61), SKY_BLUE(64), DEEP_SKY_BLUE(64), DODGER_BLUE(70), //
	NAVY_BLUE(73), BLUE(76), DARK_BLUE(79), PURPLE(94), ELECTRIC_PURPLE(97), //
	MAGENTA(100), PINK(103), DEEP_PINK(106), HOT_PINK(109), VIOLET(112), //
	VIOLET_RED(115), RED_VIOLET(118), RED2(121), WHITE(127);
	private int value;

	private YaelTexColors(final int value) {
		this.value = value;
	}

	public int getValue() {
		return value;
	}
}
