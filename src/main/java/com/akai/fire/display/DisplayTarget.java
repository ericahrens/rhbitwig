package com.akai.fire.display;

public class DisplayTarget {
	private final OledDisplay oled;
	private int focusIndex = -1;
	private int typeIndex = -1;
	private boolean active;
	private String currentName;
	private String typeName;
	
	private int activeEncoderIndex = -1;
	private boolean encoderTouched = false;
	private long lastTouchTime = 0;
	private static final long TOUCH_HOLD_MS = 25;
	
	private double lastRawValue = Double.NaN;
	private String lastDisplayValue = null;
	private long lastUpdateTime = 0;
	private static final long DEBOUNCE_MS = 20;

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
	
	public void setActiveEncoder(int encoderIndex) {
		this.activeEncoderIndex = encoderIndex;
		this.encoderTouched = true;
		this.lastTouchTime = System.currentTimeMillis();
	}
	
	public boolean isEncoderTouched() {
		return encoderTouched;
	}
	
	public void resetTouchState() {
		this.encoderTouched = false;
		this.activeEncoderIndex = -1;
		this.lastTouchTime = 0;
	}

	public void update(final int sourceIndex, final int typeIndex, final double rawValue, final String displayValue,
			final boolean bipolar) {
		if (!active) {
			return;
		}
		
		if (sourceIndex != focusIndex) {
			return;
		}
		
		if (encoderTouched) {
			if (typeIndex != activeEncoderIndex) {
				return;
			}
			if (System.currentTimeMillis() - lastTouchTime < TOUCH_HOLD_MS) {
				return;
			}
		} else {
			return;
		}
		
		if (typeIndex != this.typeIndex) {
			return;
		}
		
		long now = System.currentTimeMillis();
		if (now - lastUpdateTime < DEBOUNCE_MS) {
			return;
		}
		lastUpdateTime = now;
		
		if (lastRawValue == rawValue && displayValue != null && displayValue.equals(lastDisplayValue)) {
			return;
		}
		
		lastRawValue = rawValue;
		lastDisplayValue = displayValue;
		
		oled.parameterInfo(currentName, typeName, rawValue, displayValue, bipolar);
	}

	public void activate() {
		active = true;
		lastRawValue = Double.NaN;
		lastDisplayValue = null;
		lastUpdateTime = 0;
	}

	public void deactivate() {
		active = false;
		encoderTouched = false;
		activeEncoderIndex = -1;
		lastTouchTime = 0;
		oled.clearScreenDelayed();
	}

	public void setName(final String name) {
		this.currentName = name;
	}

	public int getTypeIndex() {
		return typeIndex;
	}

}