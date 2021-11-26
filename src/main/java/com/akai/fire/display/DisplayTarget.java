package com.akai.fire.display;

public class DisplayTarget {
	private final OledDisplay oled;
	private int focusIndex = -1;
	private int typeIndex = -1;
	private boolean active;
	private String currentName;
	private String typeName;

	public DisplayTarget(final OledDisplay oled) {
		super();
		this.oled = oled;
	}

	public boolean isActive() {
		return active;
	}

	public void setTypeIndex(final int typeIndex, final String typeName) {
		this.typeIndex = typeIndex;
		this.typeName = typeName;
	}

	public void setFocusIndex(final int focusIndex) {
		this.focusIndex = focusIndex;
	}

	public void update(final int sourceIndex, final int typeIndex, final double rawValue, final String displayValue,
			final boolean bipolar) {
		if (!active || sourceIndex != focusIndex || typeIndex != this.typeIndex) {
			return;
		}
		oled.parameterInfo(currentName, typeName, rawValue, displayValue, bipolar);
	}

	public void activate() {
		active = true;
	}

	public void deactivate() {
		active = false;
		oled.clearScreen();
	}

	public void setName(final String name) {
		this.currentName = name;
	}

	public int getTypeIndex() {
		return typeIndex;
	}

}
