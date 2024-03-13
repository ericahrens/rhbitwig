package com.yaeltex.seqarp168mk2.device;

import java.util.ArrayList;
import java.util.List;

import com.bitwig.extension.controller.api.Device;
import com.bitwig.extension.controller.api.IntegerValue;
import com.bitwig.extension.controller.api.Parameter;
import com.bitwig.extension.controller.api.SpecificBitwigDevice;
import com.yaeltex.devices.SpecialDevices;

public class BitwigArpDevice {
    private static final int NUMBER_OF_STEPS = 16;
    private final List<Parameter> arpNoteParams = new ArrayList<>();
    private final List<Parameter> arpGateParams = new ArrayList<>();
    private final List<Parameter> arpVelParams = new ArrayList<>();
    private final List<Parameter> arpSkipParams = new ArrayList<>();
    private final List<NoteControlValue> noteControlValues = new ArrayList<>();
    
    private final Parameter globalVelParam;
    private final Parameter globalGateParam;
    private final Parameter retriggerParam;
    private final Parameter rateParam;
    private final Parameter octaveParam;
    private final Parameter modeParam;
    private final Parameter shuffleParam;
    private final Parameter rateModeParam;
    
    private final Parameter arpStepsParam;
    private final IntegerValue arpStepPositionParam;
    
    private int stepLength;
    
    private int stepPosition = -1;
    private int prevPosition = -1;
    
    private final SpecificBitwigDevice arpDevice;
    private final int id;
    
    public BitwigArpDevice(final int id, final Device device) {
        this.id = id;
        arpDevice = device.createSpecificBitwigDevice(SpecialDevices.ARPEGGIATOR.getUuid());
        
        for (int i = 0; i < 16; i++) {
            final int index = i;
            arpNoteParams.add(createParameter("STEP_" + (i + 1) + "_TRANSPOSE"));
            arpGateParams.add(createParameter("GATE_" + (i + 1)));
            arpVelParams.add(createParameter("STEP_" + (i + 1)));
            arpSkipParams.add(createParameter("SKIP_" + (i + 1)));
            noteControlValues.add(new NoteControlValue());
        }
        
        rateParam = createParameter("RATE");
        octaveParam = createParameter("OCTAVES");
        modeParam = createParameter("MODE");
        shuffleParam = createParameter("SHUFFLE");
        rateModeParam = createParameter("RATE_MODE");
        retriggerParam = createParameter("RETRIGGER");
        globalVelParam = createParameter("GLOBAL_VEL");
        globalGateParam = createParameter("GLOBAL_GATE");
        
        arpStepsParam = createParameter("STEPS");
        arpStepPositionParam = arpDevice.createIntegerOutputValue("STEP");
        arpStepPositionParam.markInterested();
        
        arpStepPositionParam.addValueObserver(v -> {
            prevPosition = stepPosition;
            stepPosition = v;
            //            currentEncoderLayout.handleStepPosition(this, v, prevPosition);
        });
        
        arpStepsParam.value().addValueObserver(16, v -> {
            stepLength = v;
            //            currentEncoderLayout.handleStepLength(this, v, stepLength);
        });
    }
    
    public void applyInstance(final ArpInstance instance) {
    }
    
    private Parameter createParameter(final String id) {
        final Parameter param = arpDevice.createParameter(id);
        param.value().markInterested();
        param.markInterested();
        return param;
    }
    
    public Parameter getNoteParam(final int index) {
        return arpNoteParams.get(index);
    }
    
    public NoteControlValue getNoteValues(final int index) {
        return noteControlValues.get(index);
    }
    
    public Parameter getStepSkip(final int index) {
        return arpSkipParams.get(index);
    }
    
    public Parameter getVelocity(final int index) {
        return arpVelParams.get(index);
    }
    
    public Parameter getGate(final int index) {
        return arpGateParams.get(index);
    }
    
    public boolean isInStepRange(final int index) {
        return index <= stepLength;
    }
    
    public boolean isPlayingStep(final int index) {
        return index == stepPosition;
    }
    
    public boolean isStepSkip(final int index) {
        return arpSkipParams.get(index).getRaw() > 0;
    }
    
    public void toggleStepSkip(final int index) {
        final Parameter stepSkip = arpSkipParams.get(index);
        if (stepSkip.getRaw() > 0) {
            stepSkip.setImmediately(0);
        } else {
            stepSkip.setImmediately(1.0);
        }
    }
    
    public Parameter getRateParam() {
        return rateParam;
    }
    
    public Parameter getRateModeParam() {
        return rateModeParam;
    }
    
    
}
