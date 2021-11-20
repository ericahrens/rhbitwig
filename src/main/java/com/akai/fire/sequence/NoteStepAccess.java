package com.akai.fire.sequence;

import java.util.List;

import com.akai.fire.sequence.SequencEncoderHandler.NoteDoubleGetter;
import com.akai.fire.sequence.SequencEncoderHandler.NoteDoubleSetter;
import com.akai.fire.sequence.SequencEncoderHandler.NoteIntGetter;
import com.akai.fire.sequence.SequencEncoderHandler.NoteIntSetter;
import com.bitwig.extension.controller.api.NoteStep;

public enum NoteStepAccess implements EncoderAccess {

	VELOCITY("Velocity", ns -> ns.velocity(), //
			(final NoteStep ns, final double v) -> ns.setVelocity(v), //
			NoteValueUnit.MIDI, 0, 127, 1, 0.25), //
	CHANCE("Chance", ns -> ns.chance(), //
			(final NoteStep ns, final double v) -> ns.setChance(v), //
			NoteValueUnit.PERCENT, 0, 1, 0.05, 1, 1.0),
	REPEATS("Repeats", ns -> ns.repeatCount(), //
			(final NoteStep ns, final int v) -> ns.setRepeatCount(v), //
			NoteValueUnit.NONE, 0, 16, 1, 2, 0.0),
	TIMBRE("Timbre", ns -> ns.timbre(), //
			(final NoteStep ns, final double v) -> ns.setTimbre(v), //
			NoteValueUnit.PERCENT, -1, 1, 0.01, 0.25, 0.0),
	VELOCITY_SPREAD("Vel.Spread", ns -> ns.velocitySpread(), //
			(final NoteStep ns, final double v) -> ns.setVelocitySpread(v), //
			NoteValueUnit.PERCENT, 0, 1, 0.01, 0.25, 0.0),
	PRESSURE("Pressure", ns -> ns.pressure(), //
			(final NoteStep ns, final double v) -> ns.setPressure(v), //
			NoteValueUnit.PERCENT, 0, 1, 0.01, 0.25),
	DURATION("N.Length", ns -> ns.duration(), //
			(final NoteStep ns, final double v) -> ns.setDuration(v), //
			NoteValueUnit.NOTE_LEN, 0, 1, 0.01, 0.5),
	OCCURENCE("IF:", NoteValueUnit.OCCURENCE, 1, 4), //
	RECURRENCE("Recurr.len", ns -> ns.recurrenceLength(), //
			(final NoteStep ns, final int v) -> ns.setRecurrence(v, ns.recurrenceMask()), //
			NoteValueUnit.RECURRENCE, 1, 8, 1, 2, 0.0),
	REPEATCURVE("Repeat Curve", ns -> ns.repeatCurve(), //
			(final NoteStep ns, final double v) -> ns.setRepeatCurve(v), //
			NoteValueUnit.PERCENT, -1, 1, 0.01, 0.25, 0.0),
	REPEAT_VEL_CRV("Rpt.Vel.Crv", ns -> ns.repeatVelocityCurve(), //
			(final NoteStep ns, final double v) -> ns.setRepeatVelocityCurve(v), //
			NoteValueUnit.PERCENT, -1, 1, 0.01, 0.25, 0.0),
	REPEAT_VEL_END("Rpt.Vel.End", ns -> ns.repeatVelocityEnd(), //
			(final NoteStep ns, final double v) -> ns.setRepeatVelocityEnd(v), //
			NoteValueUnit.PERCENT, -1, 1, 0.01, 0.25, 0.0);

//	TRANSPOSE("Transpose", ns -> ns.transpose(), //
//			(final NoteStep ns, final double v) -> ns.setTranspose(v), //
//			NoteValueUnit.SEMI, -24, 24, 1.0, 1.0, 0.0)

	private final String name;
	private final NoteDoubleGetter doubleGetter;
	private final NoteIntGetter intGetter;
	private final NoteDoubleSetter doubleSetter;
	private final NoteIntSetter intSetter;
	private final NoteValueUnit unit;
	private final double min;
	private final double max;
	private final double incStep;
	private final double resolution;
	private final Double resetValue;

	NoteStepAccess(final String name, final NoteValueUnit unit, final double increment,
			final double encoderResolution) {
		this(name, null, null, null, null, unit, 0.0, 1.0, increment, encoderResolution, null);
	}

	NoteStepAccess(final String name, final NoteDoubleGetter getterDoubl, final NoteDoubleSetter setterDoubl,
			final NoteValueUnit unit, final double min, final double max, final double increment,
			final double encoderResolution) {
		this(name, getterDoubl, setterDoubl, unit, min, max, increment, encoderResolution, null);
	}

	NoteStepAccess(final String name, final NoteDoubleGetter getterDoubl, final NoteDoubleSetter setterDoubl,
			final NoteValueUnit unit, final double min, final double max, final double increment,
			final double encoderResolution, final Double resetValue) {
		this(name, getterDoubl, setterDoubl, null, null, unit, min, max, increment, encoderResolution, resetValue);
	}

	NoteStepAccess(final String name, final NoteIntGetter intGetter, final NoteIntSetter intSetter,
			final NoteValueUnit unit, final double min, final double max, final double increment,
			final double encoderResolution, final Double resetValue) {
		this(name, null, null, intGetter, intSetter, unit, min, max, increment, encoderResolution, resetValue);
	}

	NoteStepAccess(final String name, final NoteDoubleGetter getterDoubl, final NoteDoubleSetter setterDoubl,
			final NoteIntGetter intGetter, final NoteIntSetter intSetter, final NoteValueUnit unit, final double min,
			final double max, final double increment, final double encoderResolution, final Double resetValue) {
		this.name = name;
		this.doubleGetter = getterDoubl;
		this.doubleSetter = setterDoubl;
		this.intGetter = intGetter;
		this.intSetter = intSetter;
		this.unit = unit;
		this.min = min;
		this.max = max;
		this.incStep = increment;
		this.resolution = encoderResolution;
		this.resetValue = resetValue;
	}

	public String getName() {
		return name;
	}

	public NoteValueUnit getUnit() {
		return unit;
	}

	@Override
	public double getResolution() {
		return resolution;
	}

	public double getDouble(final NoteStep step) {
		return doubleGetter.get(step);
	}

	public int getInt(final NoteStep step) {
		if (intGetter != null) {
			return intGetter.get(step);
		}
		return (int) (doubleGetter.get(step) * max);
	}

	public Double applyDoubleIncrement(final int inc, final NoteStep step) {
		final double value = getDouble(step);
		final double newValue = Math.min(Math.max(min, value + inc * this.incStep), max);
		if (newValue != value) {
			doubleSetter.set(step, newValue);
			return newValue;
		}
		return null;
	}

	public Integer applyIntIncrement(final int inc, final NoteStep step) {
		final int value = getInt(step);
		final int newValue = Math.min(Math.max(getMinInt(), value + inc * (int) incStep), getMaxInt());
		if (newValue != value) {
			if (intSetter != null) {
				intSetter.set(step, newValue);
			} else {
				doubleSetter.set(step, newValue / max);
			}
			return newValue;
		}
		return null;
	}

	public int getMaxInt() {
		return (int) max;
	}

	public int getMinInt() {
		return (int) min;
	}

	public double getMax() {
		return max;
	}

	public double getMin() {
		return min;
	}

	boolean canReset() {
		return resetValue != null;
	}

	void applyReset(final List<NoteStep> onNotes) {
		if (resetValue == null) {
			return;
		}
		if (intSetter != null) {
			final int rv = resetValue.intValue();
			for (final NoteStep noteStep : onNotes) {
				intSetter.set(noteStep, rv);
			}
		} else {
			for (final NoteStep noteStep : onNotes) {
				doubleSetter.set(noteStep, resetValue.doubleValue());
			}
		}
	}
}