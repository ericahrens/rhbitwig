package com.yaeltex2.binding;

import com.bitwig.extensions.framework.Binding;
import com.yaeltex2.NoteValue;
import com.yaeltex2.RingEncoder;

public class NoteRingValueBinding extends Binding<NoteValue, RingEncoder> {
	public static final int[] NOTE_ENCODER_MAPPING = new int[] { //
			// -23,-22,-21,-20,-19,-18,-17,-16,-15,-14,-13
			10, 18, 18, 18, 18, 18, 18, 18, 18, 18, 18, 18, //
			// -11,-10,-09,-08,-07,-06,-05,-04,-03,-02,-01
			30, 36, 36, 36, 36, 46, 50, 58, 58, 58, 58, 58, //
			// 01, 02, 03, 04, 05, 06, 07, 08, 09, 10, 11
			64, 73, 73, 73, 73, 73, 73, 81, 90, 90, 90, 90, //
			// 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23, 24
			100, 108, 108, 108, 108, 108, 108, 108, 108, 108, 108, 108, 127 //
	};
	private boolean exists = false;
	private int rawValue = 0;
	private static final int[] segments = { 10, 18, 30, 36, 46, 50, 58, 64, 73, 81, 90, 100, 108, 127 };
	private static final int[] octColors = { 49, 37, 35, 32, 30, 127, 126, 125, 124, 118, 115, 103, 97 };

	public NoteRingValueBinding(final NoteValue source, final RingEncoder target) {
		super(target, source, target);
		source.addValueObserver(this::valueChange);
		source.addExistsObserver(this::valueChanged);
	}

	private void valueChanged(final boolean exists) {
		int positionValue = 0;
		int colorValue = 127;
		if (exists) {
			final int idx = rawValue % 12;
			positionValue = segments[idx];
			colorValue = octColors[Math.max(0, rawValue / 12)];
		}
		this.exists = exists;
		if (isActive()) {
			getTarget().sendValue(positionValue);
			getTarget().setColor(colorValue);
		}
	}

	private void valueChange(final int value) {
		int positionValue = 0;
		int colorValue = 127;
		if (exists) {
			final int idx = value % 12;
			positionValue = segments[idx];
			colorValue = octColors[Math.max(0, value / 12)];
		}
		rawValue = value;
		if (isActive()) {
			getTarget().sendValue(positionValue);
			getTarget().setColor(colorValue);
		}
	}

	@Override
	protected void deactivate() {
	}

	@Override
	protected void activate() {
		int positionValue = 0;
		int colorValue = 127;
		if (exists) {
			final int idx = rawValue % 12;
			positionValue = segments[idx];
			colorValue = octColors[Math.max(0, rawValue / 12)];
		}
		getTarget().sendValue(positionValue);
		getTarget().setColor(colorValue);
	}

}
