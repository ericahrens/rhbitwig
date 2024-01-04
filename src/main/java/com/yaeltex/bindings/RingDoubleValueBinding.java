package com.yaeltex.bindings;

import java.util.function.Consumer;

import com.bitwig.extension.controller.api.Parameter;
import com.bitwig.extensions.framework.Binding;
import com.yaeltex.controls.RingEncoder;

public class RingDoubleValueBinding extends Binding<Parameter, RingEncoder> {
	private boolean exists;
	private double value;

	public RingDoubleValueBinding(final Parameter source, final RingEncoder target) {
		super(target, source, target);
		source.addValueObserver(this::valueChange);
		source.exists().addValueObserver(this::valueChanged);
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
