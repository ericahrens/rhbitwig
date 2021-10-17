package com.novation.launchcontrol.arp;

import java.util.function.DoubleConsumer;
import java.util.function.Supplier;

import com.bitwig.extension.controller.api.BooleanValue;
import com.bitwig.extension.controller.api.HardwareActionBindable;
import com.bitwig.extension.controller.api.IntegerValue;
import com.bitwig.extension.controller.api.InternalHardwareLightState;
import com.bitwig.extension.controller.api.Parameter;
import com.bitwig.extension.controller.api.SettableBooleanValue;
import com.bitwig.extensions.framework.Binding;
import com.bitwig.extensions.framework.Layer;

public abstract class ButtonModeLayer extends Layer {

	private final LpcArpControlExtension driver;
	private final LayerActiveValue active = new LayerActiveValue();

	public ButtonModeLayer(final LpcArpControlExtension driver, final String name) {
		super(driver.getLayers(), name);
		this.driver = driver;
	}

	public LpcArpControlExtension getDriver() {
		return driver;
	}

	protected void assignStepControl(final RedGreenButton[] buttons, final Parameter steps,
			final IntegerValue stepPosition) {
		for (int i = 0; i < 8; i++) {
			final int index = i + 1;
			final int stepIndex = i;
			bindPressed(buttons[i], () -> steps.setRaw(index));
			bindLightState(() -> {
				if (steps.getRaw() >= index) {
					if (stepPosition.get() == stepIndex) {
						return ColorButtonLedState.GREEN_FULL;
					} else {
						return ColorButtonLedState.GREEN_DIM;
					}
				} else {
					return ColorButtonLedState.OFF;
				}
			}, buttons[i]);
		}
	}

	protected ColorButtonLedState gateValueToLed(final double value, final int index) {
		final ArpInstance arp = getDriver().getCurrentArp();
		if (arp != null) {
			return arp.gateValueToLed(value, index);
		}
		return ColorButtonLedState.OFF;
	}

	protected ColorButtonLedState skipValueToLed(final double value, final int index) {
		final ArpInstance arp = getDriver().getCurrentArp();
		if (arp != null) {
			if (value == 0) {
				return ColorButtonLedState.AMBER_DIM;
			}
		}
		return ColorButtonLedState.RED_FULL;
	}

	void bindPressed(final RedGreenButton bt, final Runnable runnable) {
		bindPressed(bt.getHwButton(), runnable);
	}

	void bindPressed(final RedGreenButton bt, final DoubleConsumer pressedPressureConsumer) {
		bindPressed(bt.getHwButton(), pressedPressureConsumer);
	}

	void bindReleased(final RedGreenButton bt, final Runnable runnable) {
		bindReleased(bt.getHwButton(), runnable);
	}

	void bindPressed(final RedGreenButton bt, final HardwareActionBindable bindable) {
		bindPressed(bt.getHwButton(), bindable);
	}

	void bindReleased(final ModeButton bt, final Runnable runnable) {
		bindReleased(bt.getHwButton(), runnable);
	}

	void bindPressed(final ModeButton bt, final HardwareActionBindable bindable) {
		bindPressed(bt.getHwButton(), bindable);
	}

	private void bindReleased(final RedGreenButton bt, final HardwareActionBindable bindable) {
		bindReleased(bt.getHwButton(), bindable);
	}

	private void bindReleased(final ModeButton bt, final HardwareActionBindable bindable) {
		bindReleased(bt.getHwButton(), bindable);
	}

	void bindModeRadio(final ModeButton bt, final ButtonModeLayer mode) {
		assert mode != null;
		bindPressed(bt.getHwButton(), () -> driver.setMode(mode));
		mode.getActive().addValueObserver(v -> bt.getLed().isOn().setValue(v));
	}

	void bindModeToggle(final ModeButton button, final ButtonModeLayer mode) {
		assert mode != null;
		button.getHwButton().isPressed().addValueObserver(pressed -> {
			if (pressed) {
				driver.toggleMode(mode);
			}
		});
//		bindToggle(bt.getHwButton(), mode);
		mode.getActive().addValueObserver(v -> button.getLed().isOn().setValue(v));
	}

	void bindModeMomentary(final ModeButton bt, final ButtonModeLayer mode) {
		assert mode != null;
		bindPressed(bt, mode.getActivateAction());
		bindReleased(bt, mode.getDeactivateAction());
		mode.getActive().addValueObserver(v -> bt.getLed().isOn().setValue(v));
	}

	void bindLayer(final RedGreenButton button, final Layer layer) {
		bindPressed(button, layer.getActivateAction());
		bindReleased(button, layer.getDeactivateAction());
	}

	@SuppressWarnings("rawtypes")
	public Binding bindLightState(final Supplier<InternalHardwareLightState> supplier, final RedGreenButton button) {
		return bindLightState(supplier, button.getLight());
	}

	@SuppressWarnings("rawtypes")
	public Binding bindLightState(final ColorButtonLedState state, final RedGreenButton button) {
		return bindLightState(() -> state, button);
	}

	public void bindToggle(final ModeButton button, final SettableBooleanValue target) {
		bindToggle(button.getHwButton(), target);
	}

	@Override
	final protected void onActivate() {
		doActivate();
		active.setValue(true);
//
//		final String modeDescription = getModeDescription();
//		if (modeDescription != null)
//			mDriver.getHost().showPopupNotification(modeDescription);
	}

	protected void doActivate() {
		/* for subclasses */
	}

	@Override
	final protected void onDeactivate() {
		doDeactivate();
		active.setValue(false);
	}

	protected void doDeactivate() {
	}

	public BooleanValue getActive() {
		return active;
	}

}
