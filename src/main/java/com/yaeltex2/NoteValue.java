package com.yaeltex2;

import java.util.ArrayList;
import java.util.List;

import com.bitwig.extension.controller.api.CursorClip;
import com.bitwig.extension.controller.api.NoteStep;
import com.bitwig.extension.controller.api.NoteStep.State;
import com.bitwig.extensions.debug.RemoteConsole;

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
	private int changeNote = -1;

	private boolean isPresent;
	private final List<BoolValueListener> existsListener = new ArrayList<>();
	private final List<IntValueListener> noteListener = new ArrayList<>();
	private final List<DoubleListener> vellisteners = new ArrayList<>();
	private final List<DoubleListener> chanceListeners = new ArrayList<>();
	private final List<DoubleListener> velocitySpreadListeners = new ArrayList<>();
	private final List<DoubleListener> transposeListeners = new ArrayList<>();
	private final List<DoubleListener> durationListeners = new ArrayList<>();
	private final List<BoolValueListener> repeatListener = new ArrayList<>();
	private final List<IntValueListener> repeatCountListeners = new ArrayList<>();
	private final List<DoubleListener> repeatCurveListeners = new ArrayList<>();
	private final List<DoubleListener> timbreListeners = new ArrayList<>();
	private NoteStep noteStep;

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
		vellisteners.forEach(l -> l.handleChange(velocityValue));
	}

	public void setNoteValue(final int noteValue) {
		this.noteValue = noteValue;
		noteListener.forEach(l -> l.handleChange(noteValue));
	}

	public double getChance() {
		return chance;
	}

	public void setChance(final double chance) {
		this.chance = chance;
		chanceListeners.forEach(l -> l.handleChange(chance));
	}

	public double getDuration() {
		return duration;
	}

	public void setDuration(final double duration) {
		this.duration = duration;
		durationListeners.forEach(l -> l.handleChange(duration));
	}

	public double getTranspose() {
		return transpose;
	}

	public void setTranspose(final double transpose) {
		this.transpose = transpose;
		transposeListeners.forEach(l -> l.handleChange(transpose));
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
		velocitySpreadListeners.forEach(l -> l.handleChange(velocitySpread));
	}

	public void setPresent(final boolean isPresent) {
		this.isPresent = isPresent;
		noteListener.forEach(l -> l.handleChange(noteValue));
		vellisteners.forEach(l -> l.handleChange(velocityValue));
		existsListener.forEach(l -> l.handle(isPresent));
	}

	public void addValueObserver(final IntValueListener listener) {
		this.noteListener.add(listener);
	}

	public void addVelValueObserver(final DoubleListener listener) {
		this.vellisteners.add(listener);
	}

	public void addChanceValueObserver(final DoubleListener listener) {
		this.chanceListeners.add(listener);
	}

	public void addVelSpreadValueObserver(final DoubleListener listener) {
		this.velocitySpreadListeners.add(listener);
	}

	public void addDurationObserver(final DoubleListener listener) {
		this.durationListeners.add(listener);
	}

	public void addTransposeObserver(final DoubleListener listener) {
		this.transposeListeners.add(listener);
	}

	public void addExistsObserver(final BoolValueListener listener) {
		this.existsListener.add(listener);
	}

	public NoteStep getNoteStep() {
		return noteStep;
	}

	public void notifyChange(final int noteExpected) {
		changeNote = noteExpected;
	}

	public void takeover() {
		if (this.noteStep == null) {
			return;
		}
		noteStep.setChance(chance);
		noteStep.setVelocity(velocityValue);
	}

	public void handleRemove(final CursorClip cursorClip) {
		if (changeNote != -1) {
			cursorClip.setStep(step, changeNote, getNoteValue(), getDuration());
		}
	}

	public void apply(final NoteStep notestep) {
		this.noteStep = notestep;
		if (notestep.state() == State.NoteOn) {
			if (changeNote >= 0) {
//				takeover();
				RemoteConsole.out.println("  -TO-> <{}> {} y={}", step, changeNote, notestep.y());
			} else {
				RemoteConsole.out.println("  -NW-> <{}> {} y={}", step, changeNote, notestep.y());
				setChance(notestep.chance());
				setVelocityValue(notestep.velocity());
			}
			setNoteValue(notestep.y());
			setDuration(notestep.duration());
			setTranspose(notestep.transpose());
			setVelocitySpread(notestep.velocitySpread());
			setPresent(true);
		}
		changeNote = -1;
	}
}
