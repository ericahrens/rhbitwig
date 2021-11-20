package com.yaeltex2.binding;

import java.util.function.Consumer;

import com.bitwig.extensions.framework.Binding;
import com.yaeltex2.NoteValue;
import com.yaeltex2.NoteValue.DoubleListener;
import com.yaeltex2.RingEncoder;

public class RingDoubleValueBinding extends Binding<NoteValue, RingEncoder> {
	private boolean exists;
	private double value;

	public RingDoubleValueBinding(final NoteValue source, final RingEncoder target,
			final Consumer<DoubleListener> valueBinding) {
		super(target, source, target);
		valueBinding.accept(this::valueChange);
		source.addExistsObserver(this::valueChanged);
	}

	private void valueChanged(final boolean exists) {
		final int sendValue = exists ? (int) (value * 127) : 0;
		this.exists = exists;
		if (isActive()) {
			getTarget().sendValue(sendValue);
			getTarget().setColor(127);
		}
	}

	private void valueChange(final double value) {
		final int sendValue = exists ? (int) (value * 127) : 0;
		this.value = value;
		if (isActive()) {
			getTarget().sendValue(sendValue);
			getTarget().setColor(127);
		}
	}

	@Override
	protected void deactivate() {
	}

	@Override
	protected void activate() {
		final int sendValue = exists ? (int) (value * 127) : 0;
		getTarget().sendValue(sendValue);
		getTarget().setColor(127);
	}

}
