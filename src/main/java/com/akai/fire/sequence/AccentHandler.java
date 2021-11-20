package com.akai.fire.sequence;

import com.akai.fire.lights.BiColorLightState;
import com.bitwig.extensions.rh.BooleanValueObject;

public class AccentHandler {
	private static final int ACCENT_OFFSET = 11;
	private int velStandard = 100;
	private int velAccented = 127;
	private final BooleanValueObject accentActive = new BooleanValueObject();
	private boolean accenButtonHeld = false;
	private boolean modified = false;
	private final DrumSequenceMode parent;
	private int velPointer = 0;

	public AccentHandler(final DrumSequenceMode drumSequenceMode) {
		this.parent = drumSequenceMode;
	}

	public int getCurrenVel() {
		return accentActive.get() ? velAccented : velStandard;
	}

	BiColorLightState getLightState() {
		return accentActive.get() ? BiColorLightState.AMBER_FULL : BiColorLightState.AMBER_HALF;
	}

	public boolean isHolding() {
		return accenButtonHeld;
	}

	void handlePressed(final boolean pressed) {
		if (!pressed) {
			if (!modified) {
				accentActive.toggle();
			}
			parent.getOled().clearScreenDelayed();
			modified = false;
		} else {
			displayAccentInfo();
		}
		accenButtonHeld = pressed;
	}

	private void displayAccentInfo() {
		parent.getOled().lineInfo("Accents", //
				String.format("%sNormal: %d\n%sAccent: %d", velPointer == 0 ? ">" : " ", velStandard, //
						velPointer == 1 ? ">" : " ", velAccented));
	}

	void handleMainEncoder(final int inc) {
		if (!accenButtonHeld) {
			return;
		}
		if (velPointer == 0) {
			final int newValue = velStandard + inc;
			if (newValue > 0 && newValue < velAccented - ACCENT_OFFSET) {
				velStandard = newValue;
				displayAccentInfo();
			}
		} else if (velPointer == 1) {
			final int newValue = velAccented + inc;
			if (newValue > velStandard + ACCENT_OFFSET && newValue < 128) {
				velAccented = newValue;
				displayAccentInfo();
			}
		}
		modified = true;
	}

	void handeMainEncoderPress(final boolean pressed) {
		if (!accenButtonHeld || !pressed) {
			return;
		}
		velPointer = (velPointer + 1) % 2;
		displayAccentInfo();
	}

}
