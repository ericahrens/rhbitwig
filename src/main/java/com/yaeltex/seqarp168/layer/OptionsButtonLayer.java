package com.yaeltex.seqarp168.layer;

import com.bitwig.extension.controller.api.Parameter;
import com.yaeltex.seqarp168.ArpDisplayModeType;
import com.yaeltex.seqarp168.ColorButtonLedState;
import com.yaeltex.seqarp168.RgbButton;
import com.yaeltex.common.YaelTexColors;
import com.yaeltex.seqarp168.YaeltexArpControlExtension;
import com.yaeltex.seqarp168.device.DeviceSlotStateValueObject;
import com.yaeltex.seqarp168.device.FocusDevice;

public class OptionsButtonLayer extends BasicButtonLayer {

	private final YaeltexLayer basicLayer;
	private YaeltexLayer currentSublayer;
	private final YaeltexLayer bottomSecondLayer;

	public OptionsButtonLayer(final YaeltexArpControlExtension driver) {
		super(driver, "BUTTON_OPTION_LAYER");

		basicLayer = new YaeltexLayer(driver, "OPTION_BASIC_SUB_LAYER");
		bottomSecondLayer = new YaeltexLayer(driver, "OPTION_BOTTOM_SUB_LAYER");

		final RgbButton[] topButton = driver.getTopRowButtons();
		final RgbButton[] bottomButton = driver.getBottomRowButtons();

		final ColorButtonLedState activeRate = ColorButtonLedState.colorFor(YaelTexColors.DARK_ORANGE, 0);
		final ColorButtonLedState inactivRate = ColorButtonLedState.colorFor(0);

		bindRateModeButton(topButton[0], 0, activeRate, inactivRate);
		bindRateModeButton(topButton[1], 0.5, activeRate, inactivRate);
		bindRateModeButton(topButton[2], 1.0, activeRate, inactivRate);

		bindEncoderLayoutMode(driver, topButton[3], ArpDisplayModeType.MODE_1X8);
		bindEncoderLayoutMode(driver, topButton[4], ArpDisplayModeType.MODE_2X8);
		bindEncoderLayoutMode(driver, topButton[5], ArpDisplayModeType.MODE_1X16);
		bindEncoderLayoutMode(driver, topButton[6], ArpDisplayModeType.MODE_2X16);

		bindSlotControl(driver, topButton[7], driver.getSlot1State(), 0);
		bindSlotControl(driver, topButton[8], driver.getSlot2State(), 1);

		bindNoteQuantize(this, topButton[9], 1);
		bindNoteQuantize(this, topButton[10], 3);
		bindNoteQuantize(this, topButton[12], 6);
		bindNoteQuantize(this, topButton[13], 8);
		bindNoteQuantize(this, topButton[14], 10);
		bindNoteQuantize(basicLayer, bottomButton[8], 0); // C
		bindNoteQuantize(basicLayer, bottomButton[9], 2); // D
		bindNoteQuantize(basicLayer, bottomButton[10], 4); // E
		bindNoteQuantize(basicLayer, bottomButton[11], 5); // F
		bindNoteQuantize(basicLayer, bottomButton[12], 7); // G
		bindNoteQuantize(basicLayer, bottomButton[13], 9); // A
		bindNoteQuantize(basicLayer, bottomButton[14], 11); // B
		bindNoteQuantize(basicLayer, bottomButton[15], 0); // C
		driver.bindDeviceToggle(this, topButton[15]);

		for (int i = 0; i < 7; i++) {
			driver.bindRateButton(basicLayer, bottomButton[i], i);
		}
		driver.bindShuffeButton(basicLayer, bottomButton[7]);

		for (int i = 0; i < 16; i++) {
			bindModeButton(bottomButton[i], i + 1);
		}
		for (int i = 0; i < 4; i++) {
			bindOctaveButton(topButton[i], i);
		}

		currentSublayer = basicLayer;
	}

	private void bindNoteQuantize(final YaeltexLayer layer, final RgbButton button, final int noteIndex) {
		final ColorButtonLedState active = ColorButtonLedState.colorFor(YaelTexColors.GREEN, 0);
		final ColorButtonLedState inactive = ColorButtonLedState.colorFor(YaelTexColors.GREEN, 2);

		layer.bindPressed(button, () -> {
			final FocusDevice device = getDriver().getFocussedDevice();
			if (!device.isArp()) {
				return;
			}
			device.toggleNoteQuantize(noteIndex);
		});

		layer.bindLightState(() -> {
			final FocusDevice device = getDriver().getFocussedDevice();
			if (!device.isArp()) {
				return ColorButtonLedState.OFF;
			}
			return device.isNoteQuantizeSet(noteIndex) ? active : inactive;
		}, button);
	}

	private void bindRateModeButton(final RgbButton button, final double matchValue,
			final ColorButtonLedState activeColor, final ColorButtonLedState nonActiveColor) {
		bindPressed(button, () -> {
			if (!getDriver().getFocussedDevice().isArp()) {
				return;
			}
			final Parameter rate = getDriver().getFocussedDevice().getRateModeParam();
			rate.set(matchValue);
		});
		bindLightState(() -> {
			if (!getDriver().getFocussedDevice().isArp()) {
				return ColorButtonLedState.OFF;
			}
			final Parameter rate = getDriver().getFocussedDevice().getRateModeParam();
			return rate.get() == matchValue ? activeColor : nonActiveColor;
		}, button);
	}

	private void bindOctaveButton(final RgbButton button, final int matchValue) {
		final ColorButtonLedState activeColor = new ColorButtonLedState(YaelTexColors.ELECTRIC_PURPLE, 0);
		final ColorButtonLedState nonActiveColor = ColorButtonLedState.colorFor(0);
		bottomSecondLayer.bindPressed(button, () -> {
			if (!getDriver().getFocussedDevice().isArp()) {
				return;
			}
			final Parameter mode = getDriver().getFocussedDevice().getOctaveParam();
			mode.set(matchValue / 3.0);
		});
		bottomSecondLayer.bindLightState(() -> {
			if (!getDriver().getFocussedDevice().isArp()) {
				return ColorButtonLedState.OFF;
			}
			final Parameter mode = getDriver().getFocussedDevice().getOctaveParam();
			final int value = (int) (mode.get() * 3);
			return value == matchValue ? activeColor : nonActiveColor;
		}, button);
	}

	private void bindModeButton(final RgbButton button, final int matchValue) {
		final ColorButtonLedState activeColor = new ColorButtonLedState(YaelTexColors.DARK_ORANGE, 0);
		final ColorButtonLedState nonActiveColor = ColorButtonLedState.colorFor(0);
		bottomSecondLayer.bindPressed(button, () -> {
			if (!getDriver().getFocussedDevice().isArp()) {
				return;
			}
			final Parameter mode = getDriver().getFocussedDevice().getModeParam();
			mode.set(matchValue / 16.0);
		});
		bottomSecondLayer.bindLightState(() -> {
			if (!getDriver().getFocussedDevice().isArp()) {
				return ColorButtonLedState.OFF;
			}
			final Parameter mode = getDriver().getFocussedDevice().getModeParam();
			final int value = (int) (mode.get() * 16);
			return value == matchValue ? activeColor : nonActiveColor;
		}, button);
	}

	private void bindSlotControl(final YaeltexArpControlExtension driver, final RgbButton button,
			final DeviceSlotStateValueObject slotvalue, final int slotIndex) {
		bindPressed(button, () -> {
			driver.pinDevice(slotIndex);
		});
		bindReleased(button.getHwButton(), () -> {
			// RemoteConsole.out.println("Released Button ");
		});
		bindLightState(() -> {
			switch (slotvalue.get()) {
			case EMPTY:
				return ColorButtonLedState.colorFor(YaelTexColors.GREEN, 0);
			case FOLLOW:
				return ColorButtonLedState.colorFor(YaelTexColors.DODGER_BLUE, 0);
			case PINNED:
				return ColorButtonLedState.colorFor(YaelTexColors.RED, 0);
			default:
				return ColorButtonLedState.colorFor(YaelTexColors.GREEN, 0);
			}
		}, button);
	}

	private void bindEncoderLayoutMode(final YaeltexArpControlExtension driver, final RgbButton button,
			final ArpDisplayModeType modeAssign) {
		bindPressed(button, () -> {
			driver.selectArpLayoutMode(modeAssign);
		});

		getDriver().getCurrentEncoderLayout().addValueObserver(layout -> {
			if (!isActive()) {
				return;
			}
			button.getLight().state().setValue(modeToLedState(modeAssign));
		});
	}

	private ColorButtonLedState modeToLedState(final ArpDisplayModeType modeAssign) {
		if (getDriver().getCurrentEncoderLayout().get().getType() == modeAssign) {
			return ColorButtonLedState.BLUE;
		}
		return ColorButtonLedState.WHITE;
	}

	@Override
	protected void encoderLayoutChanged() {
		if (isActive()) {

		}
	}

	@Override
	protected void onActivate() {
		super.onActivate();
		currentSublayer.activate();
		final YaeltexArpControlExtension driver = getDriver();
		final RgbButton[] topButton = driver.getTopRowButtons();
		updateModeButtons(topButton);
	}

	private void updateModeButtons(final RgbButton[] topButton) {
		topButton[3].setColor(modeToLedState(ArpDisplayModeType.MODE_1X8));
		topButton[4].setColor(modeToLedState(ArpDisplayModeType.MODE_2X8));
		topButton[5].setColor(modeToLedState(ArpDisplayModeType.MODE_1X16));
		topButton[6].setColor(modeToLedState(ArpDisplayModeType.MODE_2X16));
	}

	@Override
	protected void onDeactivate() {
		super.onDeactivate();
		currentSublayer.deactivate();
	}

	public void switchInternalMode() {
		if (currentSublayer == basicLayer) {
			currentSublayer.deactivate();
			currentSublayer = bottomSecondLayer;
			currentSublayer.activate();
		} else {
			currentSublayer.deactivate();
			currentSublayer = basicLayer;
			currentSublayer.activate();
			updateModeButtons(getDriver().getTopRowButtons());
		}
	}

	public ColorButtonLedState getModeColor() {
		if (isActive()) {
			if (currentSublayer == basicLayer) {
				return ColorButtonLedState.GREEN;
			}
			return ColorButtonLedState.AQUA;
		}
		return ColorButtonLedState.OFF;
	}

}
