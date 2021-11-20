package com.akai.fire.sequence;

import com.akai.fire.AkaiFireDrumSeqExtension;
import com.akai.fire.lights.BiColorLightState;
import com.bitwig.extension.controller.api.Arpeggiator;
import com.bitwig.extension.controller.api.NoteInput;
import com.bitwig.extensions.rh.BooleanValueObject;

public class NoteRepeatHandler {

	private final DrumSequenceMode parent;
	private final BooleanValueObject noteRepeatActive = new BooleanValueObject();
	private boolean repeatButtonHeld = false;
	private double currentArpRate = ARP_RATES[1];
	private static final double[] ARP_RATES = new double[] { 0.125, 0.25, 0.5, 1.0, 1.0 / 12, 1.0 / 6, 1.0 / 3,
			2.0 / 3 };
	private static final String[] GRID_RATES_STR = new String[] { "1/32", "1/16", "1/8", "1/4", //
			"1/32T", "1/16T", "1/8T", "1/4T" };
	private final Arpeggiator arp;
	private final NoteInput noteInput;
	private int selectedArpIndex;

	public NoteRepeatHandler(final AkaiFireDrumSeqExtension driver, final DrumSequenceMode drumSequenceMode) {
		this.parent = drumSequenceMode;
		this.noteInput = driver.getNoteInput();
		this.selectedArpIndex = 1;
		arp = noteInput.arpeggiator();
		arp.usePressureToVelocity().set(true);
		// arp.shuffle().set(true);
		arp.mode().set("all"); // that's the note repeat way
		arp.octaves().set(0);
		arp.humanize().set(0);
		arp.isFreeRunning().set(false);
	}

	BiColorLightState getLightState() {
		return noteRepeatActive.get() ? BiColorLightState.HALF : BiColorLightState.OFF;
	}

	void handlePressed(final boolean pressed) {
		if (pressed) {
			noteRepeatActive.toggle();
			if (noteRepeatActive.get()) {
				parent.getOled().valueInfo("Note Repeat", GRID_RATES_STR[selectedArpIndex]);
			}
		} else {
			parent.getOled().clearScreenDelayed();
		}
		repeatButtonHeld = pressed;
	}

	boolean isHolding() {
		return repeatButtonHeld;
	}

	public BooleanValueObject getNoteRepeatActive() {
		return noteRepeatActive;
	}

	void handleMainEncoder(final int inc) {
		final int newValue = selectedArpIndex + inc;
		if (newValue >= 0 && newValue < ARP_RATES.length) {
			selectedArpIndex = newValue;
			setNoteRateValue(newValue);
		}
	}

	private void setNoteRateValue(final int index) {
		this.selectedArpIndex = index;
		this.currentArpRate = ARP_RATES[index];
		arp.rate().set(currentArpRate);
		parent.getOled().valueInfo("Note Repeat", GRID_RATES_STR[index]);
	}

	public void activate() {
		arp.isEnabled().set(true);
		arp.mode().set("all"); // that's the note repeat way
		arp.octaves().set(0);
		arp.humanize().set(0);
		arp.isFreeRunning().set(false);
		arp.rate().set(currentArpRate);
	}

	public void deactivate() {
		arp.isEnabled().set(false);
	}

}
