package com.bitwig.extensions.rh;

public enum DawColor {
	BLACK("Black", 0, 0, 0), //
	WHITE("White", 1, 1, 1), //
	OFF("Off", 0.5, 0.5, 0.5), //
	DARK_GRAY("Dark Gray", 0.3294117748737335, 0.3294117748737335, 0.3294117748737335), //
	GRAY("Gray", 0.47843137383461, 0.47843137383461, 0.47843137383461), //
	GRAY_HALF("Gray half", 0.5, 0.5, 0.5), //
	LIGHT_GRAY("Light Gray", 0.7882353067398071, 0.7882353067398071, 0.7882353067398071), //
	SILVER("Silver", 0.5254902243614197, 0.5372549295425415, 0.6745098233222961), //
	DARK_BROWN("Dark Brown", 0.6392157077789307, 0.4745098054409027, 0.26274511218070984), //
	BROWN("Brown", 0.7764706015586853, 0.6235294342041016, 0.43921568989753723), //
	DARK_BLUE("Dark Blue", 0.34117648005485535, 0.3803921639919281, 0.7764706015586853), //
	PURPLE_BLUE("Purplish Blue", 0.5176470875740051, 0.5411764979362488, 0.8784313797950745), //
	PURPLE("Purple", 0.5843137502670288, 0.2862745225429535, 0.7960784435272217), //
	PINK("Pink", 0.8509804010391235, 0.21960784494876862, 0.4431372582912445), //
	RED("Red", 0.8509804010391235, 0.18039216101169586, 0.1411764770746231), //
	ORANGE("Orange", 1, 0.34117648005485535, 0.0235294122248888), //
	LIGHT_ORANGE("Light Orange", 0.8509804010391235, 0.615686297416687, 0.062745101749897), //
	MOSS_GREEN("Moss Green", 0.26274511218070984, 0.8235294222831726, 0.7254902124404907), //
	GREEN("Green", 0.45098039507865906, 0.5960784554481506, 0.0784313753247261), //
	COLD_GREEN("Cold Green", 0, 0.615686297416687, 0.27843138575553894), //
	BLUE("Blue", 0.2666666805744171, 0.7843137383460999, 1), //
	LIGHT_PURPLE("Light Purple", 0.7372549176216125, 0.4627451002597809, 0.9411764740943909), //
	LIGHT_PINK("Light Pink", 0.8823529481887817, 0.4000000059604645, 0.5686274766921997), //
	ROSE("Rose", 0.9254902005195618, 0.3803921639919281, 0.34117648005485535), //
	REDDISH_BROWN("Redish Brown", 1, 0.5137255191802979, 0.24313725531101227), //
	LIGHT_BROWN("Light Brown", 0.8941176533699036, 0.7176470756530762, 0.30588236451148987), //
	LIGHT_GREEN("Light Green", 0.6274510025978088, 0.7529411911964417, 0.2980392277240753), //
	BLUISH_GREEN("Bluish Green", 0, 0.6509804129600525, 0.5803921818733215), //
	GREEN_BLUE("Greenish Blue", 0.24313725531101227, 0.7333333492279053, 0.3843137323856354), //
	LIGHT_BLUE("Light Blue", 0, 0.6000000238418579, 0.8509804010391235);

	private final String name;
	private final int lookupIndex;

	private DawColor(final String name, final double red, final double green, final double blue) {
		this.name = name;
		lookupIndex = toLookuIndex(red, green, blue);
	}

	public static int toLookuIndex(final double red, final double green, final double blue) {
		final int rv = (int) Math.floor(red * 255);
		final int gv = (int) Math.floor(green * 255);
		final int bv = (int) Math.floor(blue * 255);
		return rv << 16 | gv << 8 | bv;
	}

	public int getLookupIndex() {
		return lookupIndex;
	}

	public String getName() {
		return name;
	}

}
