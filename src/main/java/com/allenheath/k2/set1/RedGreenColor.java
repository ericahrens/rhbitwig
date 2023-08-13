package com.allenheath.k2.set1;

public enum RedGreenColor {
    OFF(0),
    RED(0),
    YELLOW(36),
    GREEN(72);

    private final int offset;

    RedGreenColor(final int offset) {
        this.offset = offset;
    }

    public int getOffset() {
        return offset;
    }

}
