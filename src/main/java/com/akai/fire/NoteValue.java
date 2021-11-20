package com.akai.fire;

import com.bitwig.extension.controller.api.NoteStep;
import com.bitwig.extension.controller.api.NoteStep.State;

public class NoteValue {
	private final int step;
	private int noteValue;
	private double velocityValue; // 0..1
	private double velocitySpread; // 0..1
	private double chance; // 0..1
	private double duration; // 0..d
	private double transpose; // -24..+24
	private boolean repeatEnabled;
	private int repeatCount;
	private double repeatCurve;
	private double timbre; // -1..1
	private int recurrenceLength; // 0..8
	private int recurrenceMask; // bitmask

	private boolean isPresent;
	private NoteStep noteStep;
	private double repeatVelocityCurve;
	private double repeatVelocityEnd;
	private Object pressure;

	@FunctionalInterface
	public interface BoolValueListener {
		void handle(boolean exists);
	}

	@FunctionalInterface
	public interface IntValueListener {
		void handleChange(int noteValue);
	}

	@FunctionalInterface
	public interface DoubleListener {
		void handleChange(double value);
	}

	public NoteValue(final int step, final int initNoteValue) {
		this.step = step;
		this.noteValue = initNoteValue;
	}

	public int getStep() {
		return step;
	}

	public int getNoteValue() {
		return noteValue;
	}

	public boolean isPresent() {
		return isPresent;
	}

	public void setVelocityValue(final double velocityValue) {
		this.velocityValue = velocityValue;
	}

	public void setNoteValue(final int noteValue) {
		this.noteValue = noteValue;
	}

	public double getChance() {
		return chance;
	}

	public void setChance(final double chance) {
		this.chance = chance;
	}

	public double getDuration() {
		return duration;
	}

	public void setDuration(final double duration) {
		this.duration = duration;
	}

	public double getTranspose() {
		return transpose;
	}

	public void setTranspose(final double transpose) {
		this.transpose = transpose;
	}

	public double getVelocityValue() {
		return velocityValue;
	}

	public int getVelocity() {
		return (int) (velocityValue * 127);
	}

	public double getVelocitySpread() {
		return velocitySpread;
	}

	public void setVelocitySpread(final double velocitySpread) {
		this.velocitySpread = velocitySpread;
	}

	public void setPresent(final boolean isPresent) {
		this.isPresent = isPresent;
	}

	public NoteStep getNoteStep() {
		return noteStep;
	}

	public boolean isRepeatEnabled() {
		return repeatEnabled;
	}

	public int getRepeatCount() {
		return repeatCount;
	}

	public double getRepeatCurve() {
		return repeatCurve;
	}

	public double getTimbre() {
		return timbre;
	}

	public int getRecurrenceLength() {
		return recurrenceLength;
	}

	public int getRecurrenceMask() {
		return recurrenceMask;
	}

	public double getRepeatVelocityCurve() {
		return repeatVelocityCurve;
	}

	public double getRepeatVelocityEnd() {
		return repeatVelocityEnd;
	}

	public NoteValue copy() {
		final NoteValue copy = new NoteValue(this.step, this.noteValue);
		copy.chance = this.chance;
		copy.isPresent = this.isPresent;
		copy.timbre = this.timbre;
		copy.transpose = this.transpose;
		copy.pressure = this.pressure;
		copy.duration = this.duration;

		copy.repeatCount = this.repeatCount;
		copy.repeatCurve = this.repeatCurve;
		copy.repeatVelocityCurve = this.repeatVelocityCurve;
		copy.repeatVelocityEnd = this.repeatVelocityEnd;

		copy.recurrenceLength = this.recurrenceLength;
		copy.recurrenceMask = this.recurrenceMask;
		return copy;
	}

	public void apply(final NoteStep notestep) {
		this.noteStep = notestep;
		if (notestep.state() != null) {
			setNoteValue(notestep.y());
			setDuration(notestep.duration());
			setTranspose(notestep.transpose());
			setVelocitySpread(notestep.velocitySpread());
			setPresent(noteStep.state() == State.NoteOn);
			pressure = notestep.pressure();
			recurrenceLength = notestep.recurrenceLength();
			recurrenceMask = notestep.recurrenceMask();
			repeatCount = notestep.repeatCount();
			repeatCurve = notestep.repeatCurve();
			repeatVelocityCurve = notestep.repeatVelocityCurve();
			repeatVelocityEnd = notestep.repeatVelocityEnd();
		} else {
			setPresent(false);
		}
	}
}
