package com.allenheath.k2.set1;

import java.util.UUID;

import com.bitwig.extension.controller.api.ControllerHost;
import com.bitwig.extension.controller.api.Parameter;
import com.bitwig.extension.controller.api.Track;
import com.rhcommons.FollowDevice;

public class ArpFollowDevice extends FollowDevice {
    public static final UUID BITWIG_ARP_DEVICE = UUID.fromString("4d407a2b-c91b-4e4c-9a89-c53c19fe6251");
    private static final int PARAM_IN_VIEW = 8;
    
    private final Parameter length;
    private final Parameter[] noteValues = new Parameter[PARAM_IN_VIEW];
    private final Parameter[] gates = new Parameter[PARAM_IN_VIEW];
    private final Parameter[] velocities = new Parameter[PARAM_IN_VIEW];
    private final boolean[] endSteps = new boolean[PARAM_IN_VIEW];
    private int endStep = 8;
    
    public ArpFollowDevice(
        final int index, final ControllerHost host, final Track track) {
        super(index, host, track, BITWIG_ARP_DEVICE);
        for(int i=0;i<PARAM_IN_VIEW;i++) {
            noteValues[i] = specificDevice.createParameter("STEP_" + (i + 1) + "_TRANSPOSE");
            gates[i] = specificDevice.createParameter("GATE_" + (i + 1));
            velocities[i] = specificDevice.createParameter("STEP_" + (i + 1));
        }
        length = specificDevice.createParameter("STEPS");
    }
    
    public void setValue(final int index, final int pos) {
        if(pos < 6) {
            gates[index].set(0);
            noteValues[index].set(0.5);
            endSteps[index] = false;
        }  else if(pos > 121) {
            gates[index].set(1.0);
            endSteps[index] = true;
        } else {
            double value = (pos-6)/115.0;
            gates[index].set(1.0);
            noteValues[index].set(value);
            endSteps[index] = false;
        }
        
        int newEndStep = 8;
        for(int i=0;i<PARAM_IN_VIEW;i++) {
            if(endSteps[i]) {
                newEndStep = i;
                break;
            }
        }
        if(newEndStep != endStep) {
            endStep = newEndStep;
            length.setRaw(endStep);
        }
    }
}
