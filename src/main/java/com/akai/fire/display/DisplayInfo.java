package com.akai.fire.display;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Supplier;

import com.akai.fire.display.OledDisplay.TextJustification;
import com.bitwig.extensions.debug.RemoteConsole;

public class DisplayInfo {

	private final List<Line> lines = new ArrayList<>();

	public DisplayInfo() {

	}

	public List<Line> getLines() {
		return lines;
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
//		RemoteConsole.out.println("=============");
		Collections.sort(lines);
		int bottom = 0;
		final List<Line> result = new ArrayList<>();
		for (final Line line : lines) {
			RemoteConsole.out.println("==> {}", line);
			final int dist = line.getOffset() - bottom;
			if (dist > 0) {
				RemoteConsole.out.println(" FILL {}", dist);
				result.add(new Line("", dist, TextJustification.CENTER, bottom));
				bottom += dist;
			}
			result.add(line);
			bottom += line.getArea();
		}
//		RemoteConsole.out.println("Bottom {}", bottom);
		if (bottom < 8) {
			final int fill = 8 - bottom;
			result.add(new Line("", fill, TextJustification.CENTER, bottom));
		}
		lines.clear();
		lines.addAll(result);
//		RemoteConsole.out.println("=============");
//		for (final Line line : lines) {
//			RemoteConsole.out.println("==> {}", line);
//		}
//		RemoteConsole.out.println("=============");
		return this;
	}
}
