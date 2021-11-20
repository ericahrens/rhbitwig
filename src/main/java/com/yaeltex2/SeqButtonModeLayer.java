package com.yaeltex2;

import com.bitwig.extensions.framework.Layer;

public class SeqButtonModeLayer extends Layer {

	private static final String LAYER_NAME = "SEQ_BUTTON_MODE_LAYER";

	private final ValueObject<SeqButtonEncoderMode> encoderMode = new ValueObject<>(SeqButtonEncoderMode.NOTE);

	public SeqButtonModeLayer(final ExtensionDriver driver) {
		super(driver.getLayers(), LAYER_NAME);
		final ColorButton[] buttons = driver.getRowButtons();

		for (final SeqButtonEncoderMode mode : SeqButtonEncoderMode.values()) {
			buttons[mode.getIndex()].bindPressed(this, pressed -> setMode(pressed, mode), () -> modeColor(mode));
		}

	}

	private void setMode(final boolean pressed, final SeqButtonEncoderMode mode) {
		if (!pressed) {
			return;
		}
		encoderMode.set(mode);
	}

	private ColorButtonLedState modeColor(final SeqButtonEncoderMode mode) {
		if (encoderMode.get() == mode) {
			return ColorButtonLedState.AQUA;
		}
		return ColorButtonLedState.WHITE;
	}

	public ValueObject<SeqButtonEncoderMode> getEncoderMode() {
		return encoderMode;
	}

}
