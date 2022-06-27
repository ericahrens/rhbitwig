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
    }

    public void setStepLength(final Integer length) {
        if (length > 0 && length <= 16) {
            stepLength.value().set(length - 1, 16);
        }
    }

    public void setNotes(final List<Object> arguments) {
    }

    public void setGateLength(final int sustain) {
        gateLength.set(sustain, 127);
    }
}
