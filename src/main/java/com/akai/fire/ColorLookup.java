package com.akai.fire;

import java.util.HashMap;
import java.util.Map;

import com.akai.fire.lights.RgbLigthState;
import com.bitwig.extension.api.Color;
import com.bitwig.extensions.rh.DawColor;

public class ColorLookup {
	private static Map<Integer, DawColor> lookupMap = new HashMap<>();
	private static Map<Integer, RgbLigthState> dc = new HashMap<>();

	static {
		final DawColor[] colors = DawColor.values();
		for (final DawColor dawColor : colors) {
			lookupMap.put(dawColor.getLookupIndex(), dawColor);
		}
//		register(DawColor.GRAY, LpColor.GREY_MD);
//		register(DawColor.GRAY_HALF, LpColor.GREY_HALF);
//		register(DawColor.LIGHT_GRAY, LpColor.GREY_MD);
//		register(DawColor.DARK_BROWN, LpColor.AMBER);
//		register(DawColor.BROWN, LpColor.AMBER);
//		register(DawColor.ROSE, LpColor.ROSE);
//		register(DawColor.PURPLE_BLUE, LpColor.BLUE_ORCHID);
		register(DawColor.RED, new RgbLigthState(70, 0, 0, true));
		register(DawColor.BLUE, new RgbLigthState(0, 0, 127, true));
		register(DawColor.LIGHT_BLUE, new RgbLigthState(0, 30, 100, true));
		register(DawColor.BLUISH_GREEN, new RgbLigthState(0, 60, 100, true));
		register(DawColor.GREEN, new RgbLigthState(5, 68, 20, true));
		register(DawColor.COLD_GREEN, new RgbLigthState(0, 50, 5, true));
		register(DawColor.ORANGE, new RgbLigthState(100, 15, 0, true));
		register(DawColor.GREEN_BLUE, new RgbLigthState(0, 100, 20, true));
		register(DawColor.MOSS_GREEN, new RgbLigthState(0, 90, 40, true));
		register(DawColor.LIGHT_BROWN, new RgbLigthState(70, 70, 0, true));
		register(DawColor.DARK_BLUE, new RgbLigthState(30, 10, 90, true));
		register(DawColor.PINK, new RgbLigthState(100, 10, 90, true));
		register(DawColor.LIGHT_PINK, new RgbLigthState(90, 30, 90, true));
		register(DawColor.LIGHT_PURPLE, new RgbLigthState(70, 10, 100, true));
		register(DawColor.PURPLE, new RgbLigthState(80, 0, 80, true));
		register(DawColor.DARK_GRAY, new RgbLigthState(80, 80, 100, true));
		register(DawColor.LIGHT_GRAY, new RgbLigthState(90, 90, 110, true));
		register(DawColor.SILVER, new RgbLigthState(70, 70, 120, true));
	}

	private static void register(final DawColor dawColor, final RgbLigthState lpColor) {
		dc.put(dawColor.getLookupIndex(), lpColor);
	}

	public static RgbLigthState getColor(final DawColor dawColor) {
		return dc.getOrDefault(dawColor.getLookupIndex(), RgbLigthState.OFF);
	}

	public static DawColor getDawColor(final double red, final double green, final double blue) {
		final int rv = (int) Math.floor(red * 255);
		final int gv = (int) Math.floor(green * 255);
		final int bv = (int) Math.floor(blue * 255);
		return lookupMap.get(rv << 16 | gv << 8 | bv);
	}

	public static final RgbLigthState getColor(final double red, final double green, final double blue) {
		final int rv = (int) Math.floor(red * 255);
		final int gv = (int) Math.floor(green * 255);
		final int bv = (int) Math.floor(blue * 255);
		return dc.computeIfAbsent(rv << 16 | gv << 8 | bv, index -> calcRgbState(red, green, blue));
	}

	public static RgbLigthState getColor(final Color color) {
		final int rv = (int) Math.floor(color.getRed255());
		final int gv = (int) Math.floor(color.getGreen255());
		final int bv = (int) Math.floor(color.getBlue255());
		return dc.computeIfAbsent(rv << 16 | gv << 8 | bv, index -> calcRgbState(rv, gv, bv));
	}

	private static RgbLigthState calcRgbState(final double red, final double green, final double blue) {
		int rfactor = 60;
		int gfactor = 60;
		int bfactor = 60;
		if (red == 1.0) {
			rfactor = 127;
		}
		if (green == 1.0) {
			gfactor = 127;
		}
		if (blue == 1.0) {
			bfactor = 127;
		}
		final byte r = (byte) Math.floor(red * rfactor);
		final byte g = (byte) Math.floor(green * gfactor);
		final byte b = (byte) Math.floor(blue * bfactor);

		return new RgbLigthState(r, g, b);
	}

}
