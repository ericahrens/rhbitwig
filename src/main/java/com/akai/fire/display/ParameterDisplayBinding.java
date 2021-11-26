package com.akai.fire.display;

import com.bitwig.extension.controller.api.Parameter;
import com.bitwig.extension.controller.api.SettableRangedValue;
import com.bitwig.extensions.framework.Binding;

public class ParameterDisplayBinding extends Binding<Parameter, DisplayTarget> {

	private double rawValue;
	private String displayValue;
	private final int index;
	private final int typeIndex;
	private final boolean bipolar;

	public ParameterDisplayBinding(final int typeIndex, final int index, final Parameter source,
			final DisplayTarget target, final boolean bipolar) {
		super(target, source, target);
		this.index = index;
		this.typeIndex = typeIndex;
		this.bipolar = bipolar;
		source.value().addValueObserver(this::handleRawValue);
		source.displayedValue().addValueObserver(this::handleDisplayValue);
	}

	private void handleRawValue(final double rawValue) {
		this.rawValue = rawValue;
		if (isActive()) {
			getTarget().update(index, typeIndex, rawValue, displayValue, bipolar);
		}
	}

	private void handleDisplayValue(final String displayValue) {
		this.displayValue = displayValue;
		if (isActive()) {
			getTarget().update(index, typeIndex, rawValue, displayValue, bipolar);
		}
	}

	@Override
	protected void deactivate() {
	}

	@Override
	protected void activate() {
		update();
	}

	public void modify(final double inc) {
		final SettableRangedValue value = getSource().value();
		final double preValue = value.get();
		final double newValue = Math.min(1, Math.max(0, preValue + inc));
		if (preValue != newValue) {
			value.setImmediately(newValue);
		}
	}

	public void update() {
		getTarget().update(index, typeIndex, rawValue, displayValue, bipolar);
	}

}
