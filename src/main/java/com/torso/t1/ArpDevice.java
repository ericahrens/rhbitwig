package com.torso.t1;

import com.bitwig.extension.controller.api.ControllerHost;
import com.bitwig.extension.controller.api.Parameter;
import com.bitwig.extension.controller.api.Track;

import java.util.List;
import java.util.UUID;

public class ArpDevice extends FollowDevice {
    public static final UUID BITWIG_ARP_DEVICE = UUID.fromString("4d407a2b-c91b-4e4c-9a89-c53c19fe6251");

    private final Parameter[] notes = new Parameter[16];
    private final Parameter[] gates = new Parameter[16];
    private final Parameter[] skips = new Parameter[16];
    private final Parameter[] velocities = new Parameter[16];
    private final Parameter stepLength;
    private final Parameter gateLength;
    private final Parameter rate;
    private final Parameter rateMode;

    public ArpDevice(final int trackIndex, final ControllerHost host, final Track track) {
        super(trackIndex, host, track, BITWIG_ARP_DEVICE);
        for (int i = 0; i < 16; i++) {
            gates[i] = createParameter("GATE_" + (i + 1));
            velocities[i] = createParameter("STEP_" + (i + 1));
            notes[i] = createParameter("STEP_" + (i + 1) + "_TRANSPOSE");
            skips[i] = createParameter("SKIP_" + (i + 1));
        }
        stepLength = createParameter("STEPS");
        gateLength = createParameter("GLOBAL_GATE");
        rate = createParameter("RATE");
        rateMode = createParameter("RATE_MODE");
    }

    public void setStepLength(final Integer length) {
        if (length > 0 && length <= 16) {
            stepLength.value().set(length - 1, 16);
        }
    }

    public void setOffsets(final List<Integer> offsets, final int numberOfNotes, final int steps) {
        if (offsets.size() > 0) {
            for (int i = 0; i < steps; i++) {
                final int offIndex = i % offsets.size();
                host.println(String.format("i=%d ni=%d no=%d", i, offIndex, offsets.get(offIndex)));
                notes[i].set(24 + offsets.get(offIndex), 49);
            }
        }
    }

    public void setPulseLocations(final List<Integer> arguments) {
        final boolean[] slots = new boolean[16];
        for (final Integer loc : arguments) {
            if (loc >= 1 && loc < 17) {
                slots[loc - 1] = true;
            }
        }
        for (int i = 0; i < slots.length; i++) {
            gates[i].set(slots[i] ? 1.0 : 0);
        }
    }

    public void setRate(final int rate) {
        host.println(" RATE " + rate);
        if (rate < 7) {
            this.rate.set(rate, 7);
            rateMode.set(0, 3);
        } else {
            this.rate.set(rate - 6, 7);
            rateMode.set(2, 3);
        }
    }

    public void setGateLength(final int sustain) {
        if (sustain <= 64) {
            final double value = sustain * 0.25 / 64.0;
            gateLength.set(value);
        } else if (sustain <= 96) {
            final double value = 0.25 + (sustain - 64) * 0.25 / 32.0;
            gateLength.set(value);
        } else if (sustain <= 116) {
            final double value = 0.50 + (sustain - 96) * 0.25 / 20.0;
            gateLength.set(value);
        } else {
            final double value = 0.75 + (sustain - 116) * 0.25 / 11.0;
            gateLength.set(value);
        }
    }
}
