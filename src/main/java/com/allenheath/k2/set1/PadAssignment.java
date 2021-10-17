package com.allenheath.k2.set1;

import java.util.ArrayList;
import java.util.List;

public class PadAssignment {

	private final List<PadContainer> pads = new ArrayList<>();
	private final int index;

	public PadAssignment(final int index) {
		this.index = index;
	}

	public int getIndex() {
		return index;
	}

	public RedGreenButtonState sendStatusColor(final int sendSlot) {
		if (pads.isEmpty()) {
			return RedGreenButtonState.OFF;
		}
		return pads.get(0).sendStatusColor(sendSlot);
	}

	public void assign(final String assignValues, final List<PadContainer> drumPadsList) {
		if (assignValues == null || assignValues.length() == 0) {
			return;
		}
		pads.clear();
		final String[] vs = assignValues.split(",");
		for (final String indexString : vs) {
			try {
				final int indexValue = Integer.parseInt(indexString.trim()) - 1;
				if (indexValue >= 0 && indexValue < drumPadsList.size()) {
					pads.add(drumPadsList.get(indexValue));
				}
			} catch (final NumberFormatException e) {
			}
		}
	}

	public void toggleSendValue(final int sendSlot) {
		if (!pads.isEmpty()) {
			final int newValue = pads.get(0).toggleSendValue(sendSlot);
			for (int i = 1; i < pads.size(); i++) {
				pads.get(i).setValue(sendSlot, newValue);
			}
		}
	}

}
