package com.akai.fire.sequence;

import java.util.List;

import com.akai.fire.sequence.ClipboardManager.ClippedNote;
import com.bitwig.extension.controller.api.NoteStep;

public class NoteAction {
	public enum Type {
		CLEAR, COPY_PAD, PASTE_CLIPBOARD;
	}

	private final int destPadIndex;
	private final int srcPadIndex;
	private final Type type;
	private final List<NoteStep> copyNotes;
	private final List<ClippedNote> clippedNotes;

	NoteAction(final int srcPadIndex, final int destPadIndex, final Type type) {
		this(srcPadIndex, destPadIndex, type, null, null);
	}

	NoteAction(final int srcPadIndex, final int destPadIndex, final Type type, final List<NoteStep> copyNotes) {
		this(srcPadIndex, destPadIndex, type, copyNotes, null);
	}
	
	NoteAction(final int srcPadIndex, final int destPadIndex, final Type type, final List<NoteStep> copyNotes,
			final List<ClippedNote> clippedNotes) {
		this.destPadIndex = destPadIndex;
		this.srcPadIndex = srcPadIndex;
		this.type = type;
		this.copyNotes = copyNotes;
		this.clippedNotes = clippedNotes;
	}

	public Type getType() {
		return type;
	}

	public int getSrcPadIndex() {
		return srcPadIndex;
	}

	public int getDestPadIndex() {
		return destPadIndex;
	}

	public List<NoteStep> getCopyNotes() {
		return copyNotes;
	}
	
	public List<ClippedNote> getClippedNotes() {
		return clippedNotes;
	}
}