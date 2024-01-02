package com.novation.launchpadProMk3;

import java.util.HashMap;
import java.util.Map;

import com.bitwig.extension.api.Color;
import com.bitwig.extensions.framework.values.DawColor;

public class ColorLookup {
	private static Map<Integer, DawColor> lookupMap = new HashMap<>();
	private static Map<Integer, LpColor> dc = new HashMap<>();

	static {
		final DawColor[] colors = DawColor.values();
		for (final DawColor dawColor : colors) {
			lookupMap.put(dawColor.getLookupIndex(), dawColor);
		}
		register(DawColor.BLACK, LpColor.BLACK);
		register(DawColor.WHITE, LpColor.WHITE);
		register(DawColor.DARK_GRAY, LpColor.GREY_HALF);
		register(DawColor.GRAY, LpColor.GREY_MD);
		register(DawColor.GRAY_HALF, LpColor.GREY_HALF);
		register(DawColor.LIGHT_GRAY, LpColor.GREY_MD);
		register(DawColor.SILVER, LpColor.WHITE);
		register(DawColor.DARK_BROWN, LpColor.AMBER);
		register(DawColor.BROWN, LpColor.AMBER);
		register(DawColor.ROSE, LpColor.ROSE);
		register(DawColor.DARK_BLUE, LpColor.OCEAN);
		register(DawColor.PURPLE_BLUE, LpColor.BLUE_ORCHID);
		register(DawColor.PURPLE, LpColor.PURPLE);
		register(DawColor.PINK, LpColor.PINK);
		register(DawColor.RED, LpColor.RED);
		register(DawColor.ORANGE, LpColor.ORANGE);
		register(DawColor.LIGHT_ORANGE, LpColor.ORANGE);
		register(DawColor.MOSS_GREEN, LpColor.LIME);
		register(DawColor.GREEN, LpColor.GREEN);
		register(DawColor.COLD_GREEN, LpColor.TURQUOISE);
		register(DawColor.LIGHT_PURPLE, LpColor.MAGENTA_PINK);
		register(DawColor.LIGHT_PINK, LpColor.ORCHID_MAGENTA);
		register(DawColor.REDDISH_BROWN, LpColor.RED_AMBER);
		register(DawColor.LIGHT_BROWN, LpColor.AMBER);
		register(DawColor.LIGHT_GREEN, LpColor.LIME);
		register(DawColor.BLUISH_GREEN, LpColor.TURQUOISE);
		register(DawColor.GREEN_BLUE, LpColor.BLUE);
		register(DawColor.LIGHT_BLUE, LpColor.SKY);
		register(DawColor.BLUE, LpColor.BLUE);
	}

	private static void register(final DawColor dawColor, final LpColor lpColor) {
		dc.put(dawColor.getLookupIndex(), lpColor);
	}

	public static LpColor getColor(Color color, LpColor black) {
		final int rv = (int) Math.floor(color.getRed255());
		final int gv = (int) Math.floor(color.getGreen255());
		final int bv = (int) Math.floor(color.getBlue255());
		return dc.get(rv << 16 | gv << 8 | bv);
	}

	static LpColor getColor(final double red, final double green, final double blue) {
		final int rv = (int) Math.floor(red * 255);
		final int gv = (int) Math.floor(green * 255);
		final int bv = (int) Math.floor(blue * 255);
		return dc.get(rv << 16 | gv << 8 | bv);
	}

	static LpColor getColor(final double red, final double green, final double blue, final LpColor defaultColor) {
		final int rv = (int) Math.floor(red * 255);
		final int gv = (int) Math.floor(green * 255);
		final int bv = (int) Math.floor(blue * 255);
		final LpColor color = dc.get(rv << 16 | gv << 8 | bv);
		return color != null ? color : defaultColor;
	}

	static DawColor get(final int index) {
		return lookupMap.get(index);
	}

}