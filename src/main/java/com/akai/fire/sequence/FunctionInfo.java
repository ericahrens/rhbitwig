package com.akai.fire.sequence;

import java.util.HashMap;
import java.util.Map;

import com.akai.fire.NoteAssign;
import com.akai.fire.display.DisplayInfo;
import com.akai.fire.display.OledDisplay.TextJustification;

public class FunctionInfo {
	public static final Map<NoteAssign, FunctionInfo> INFO1 = new HashMap<>();
	public static final Map<NoteAssign, FunctionInfo> INFO2 = new HashMap<>();

	static {
		INFO1.put(NoteAssign.MUTE_1, new FunctionInfo("Select", "Pad: select Pad\nClip: select clip"));
		INFO1.put(NoteAssign.MUTE_2, new FunctionInfo("Last Step", "Step: set last step"));
		INFO1.put(NoteAssign.MUTE_3,
				new FunctionInfo("Copy", "Pad: from selected\nClip: from selected\nStep: Copy steps", "Copy -opvals"));
		INFO1.put(NoteAssign.MUTE_4,
				new FunctionInfo("Delete", "Pad: clear notes\nClip: clear notes\nEncoder: reset value"));
		INFO2.put(NoteAssign.MUTE_1, new FunctionInfo("Mute", "Pad: Mute\nNotes: mute notes"));
		INFO2.put(NoteAssign.MUTE_2, new FunctionInfo("Solo", "Pad: Solo"));
	}

	private final String name;
	private final String detail;
	private final String shiftFunction;

	public FunctionInfo(final String name, final String detail) {
		this(name, detail, null);
	}

	public FunctionInfo(final String name, final String detail, final String shiftFunction) {
		super();
		this.name = name;
		this.detail = detail;
		this.shiftFunction = shiftFunction;
	}

	public String getName(final boolean shift) {
		if (!shift || shiftFunction == null) {
			return name;
		}
		return shiftFunction;
	}

	public String getDetail() {
		return detail;
	}

	public DisplayInfo toDisplayInfo(final String context, final boolean shift) {
		return new DisplayInfo()
			.addLine(context, 0, 0, TextJustification.CENTER)
			.addLine(getName(shift), 2, 1, TextJustification.CENTER)
			.addLine("", 0, 3, TextJustification.LEFT)
			.addLine(detail, 0, 4, TextJustification.LEFT)
			.create();
	}

}
