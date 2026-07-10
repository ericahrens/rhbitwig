package com.akai.fire.display;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Supplier;

import com.akai.fire.display.OledDisplay.TextJustification;

public class DisplayInfo {

	private final List<Line> lines = new ArrayList<>();
	private String controllerId; // Добавен ID на контролера

	public DisplayInfo() {

	}
	
	public DisplayInfo(String controllerId) {
		this.controllerId = controllerId;
	}

	public List<Line> getLines() {
		return lines;
	}
	
	public String getControllerId() {
		return controllerId;
	}
	
	public void setControllerId(String controllerId) {
		this.controllerId = controllerId;
	}

	public DisplayInfo addLine(final Supplier<String> text, final int size, final int offset,
			final TextJustification justfication) {
		lines.add(new Line(text, size, justfication, offset));
		return this;
	}

	public DisplayInfo addLine(final String text, final int size, final int offset,
			final TextJustification justfication) {
		lines.add(new Line(text, size, justfication, offset));
		return this;
	}

	public DisplayInfo create() {
		Collections.sort(lines);
		int bottom = 0;
		final List<Line> result = new ArrayList<>();
		for (final Line line : lines) {
			final int dist = line.getOffset() - bottom;
			if (dist > 0) {
				result.add(new Line("", dist, TextJustification.CENTER, bottom));
				bottom += dist;
			}
			result.add(line);
			bottom += line.getArea();
		}
		if (bottom < 8) {
			final int fill = 8 - bottom;
			result.add(new Line("", fill, TextJustification.CENTER, bottom));
		}
		lines.clear();
		lines.addAll(result);
		return this;
	}
}