package com.akai.fire.display;

import java.util.function.Supplier;

import com.akai.fire.display.OledDisplay.TextJustification;

public class Line implements Comparable<Line> {
	private final String text;
	private final int size;
	private final TextJustification justification;
	private final int offset;
	private boolean containsValue = false;
	private Supplier<String> valueSupplier;

	public Line(final String text, final int size, final TextJustification justification, final int offset) {
		super();
		this.text = text;
		this.size = size;
		this.justification = justification;
		this.offset = offset;
		containsValue = false;
	}

	public Line(final Supplier<String> valueSupplier, final int size, final TextJustification justification,
			final int offset) {
		super();
		this.text = null;
		this.valueSupplier = valueSupplier;
		this.size = size;
		this.justification = justification;
		this.offset = offset;
		containsValue = true;
	}

	public String getViewText() {
		if (valueSupplier != null) {
			final String v = valueSupplier.get();
			return v != null ? v : "###";
		}
		return text;
	}

	public String getText() {
		return text;
	}

	public int getSize() {
		return size;
	}

	public int getArea() {
		switch (size) {
		case 0:
			return 1;
		case 1:
			return 1;
		case 2:
			return 2;
		case 3:
			return 3;
		}
		return 0;
	}

	public TextJustification getJustification() {
		return justification;
	}

	public int getOffset() {
		return offset;
	}

	public boolean isContainsValue() {
		return containsValue;
	}

	@Override
	public int compareTo(final Line o) {
		return offset - o.getOffset();
	}

	@Override
	public String toString() {
		final StringBuilder builder = new StringBuilder();
		builder.append("Line [text=").append(text).append(", size=").append(size).append(", justification=")
				.append(justification).append(", offset=").append(offset).append(", containsValue=")
				.append(containsValue).append("]");
		return builder.toString();
	}

}