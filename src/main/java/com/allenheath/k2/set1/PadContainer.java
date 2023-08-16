package com.allenheath.k2.set1;

import com.bitwig.extension.controller.api.DrumPad;
import com.bitwig.extension.controller.api.Send;

public class PadContainer {
    private final DrumPad pad;
    private final int index;
    private boolean exists = true;

    public PadContainer(final int index, final DrumPad pad) {
        super();
        this.pad = pad;
        this.index = index;
        this.pad.name().markInterested();
        pad.exists().addValueObserver(exists -> {
            this.exists = exists;
        });
    }

    @Override
    public String toString() {
        return "pad=%d".formatted(index);
    }

    public int getIndex() {
        return index;
    }

    public RedGreenButtonState sendStatusColor(final int sendSlot) {
        if (!exists) {
            return RedGreenButtonState.OFF;
        }
        final Send send = pad.sendBank().getItemAt(sendSlot);
        final double value = send.get();
        if (value == 0) {
            return RedGreenButtonState.OFF;
        } else if (value == 1.0) {
            return RedGreenButtonState.GREEN;
        }
        return RedGreenButtonState.YELLOW;
    }

    public int toggleSendValue(final int sendSlot) {
        final Send send = pad.sendBank().getItemAt(sendSlot);
        final double value = send.value().get();
        if (value > 0) {
            send.value().setImmediately(0.0);
            return 0;
        }
        send.value().setImmediately(1.0);
        return 1;
    }

    public void setValue(final int sendSlot, final int value) {
        final Send send = pad.sendBank().getItemAt(sendSlot);
        send.value().setImmediately(value);
    }

}
