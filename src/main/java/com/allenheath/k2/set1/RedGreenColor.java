package com.allenheath.k2.set1;

import com.bitwig.extension.api.Color;

public enum RedGreenColor {
    OFF(0, Color.fromHex("#111")),
    RED(0, Color.fromHex("#f11")),
    YELLOW(36, Color.fromHex("#ff1")),
    GREEN(72, Color.fromHex("#1f1"));

    private final int offset;
    private final Color color;

    RedGreenColor(final int offset, Color color) {
        this.offset = offset;
        this.color = color;
    }

    public int getOffset() {
        return offset;
    }
    
    public Color getColor() {
        return color;
    }
}
