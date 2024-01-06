package com.yaeltex.devices;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

import com.bitwig.extension.controller.api.ControllerHost;
import com.bitwig.extension.controller.api.Device;
import com.bitwig.extension.controller.api.DeviceBank;
import com.bitwig.extension.controller.api.DeviceMatcher;
import com.bitwig.extension.controller.api.Track;
import com.bitwig.extensions.framework.Layer;
import com.bitwig.extensions.framework.values.SpecialDevices;
import com.yaeltex.fuse.FuseExtension;
import com.yaeltex.fuse.SynthControl1;
import com.yaeltex.fuse.SynthControl2;

public class DirectDeviceControl {
    
    private final ControllerHost host;
    private final Map<SpecialDevices, Object> deviceLookup = new HashMap<>();
    private final Track baseTrack;
    private final int index;
    private Device activeDevice;
    private DirectDevice activeDirectDevice;
    
    public DirectDeviceControl(int index, Track baseTrack, ControllerHost host) {
        this.index = index;
        //cursorDevice = baseTrack.createCursorDevice();
        
        this.baseTrack = baseTrack;
        this.host = host;
    }
    
    public void addSpecificBitwig(DirectDevice type) {
        final DeviceMatcher matcher = type.createMatcher(host);
        final DeviceBank matcherBank = baseTrack.createDeviceBank(1);
        final Device device = matcherBank.getDevice(0);
        matcherBank.setDeviceMatcher(matcher);
        device.exists().addValueObserver(exist -> {
            activeDevice = exist ? device : null;
            activeDirectDevice = exist ? type : null;
            
        });
        device.addDirectParameterIdObserver(ids -> {
            FuseExtension.println(" %d %s => %s", index, type, Arrays.stream(ids).collect(Collectors.joining("\n")));
        });
        device.addDirectParameterNormalizedValueObserver((id, value) -> {
            FuseExtension.println(" %d %s <%s> => %s", index, type, id, value);
        });
    }
    
    public void bindSynth(Layer layer, SynthControl1 control) {
        control.cutoff().value().addValueObserver(v -> applyValue(ParameterType.CUT, v));
        control.resonance().value().addValueObserver(v -> applyValue(ParameterType.RES, v));
        control.mod().value().addValueObserver(v -> applyValue(ParameterType.MOD, v));
        control.amount().value().addValueObserver(v -> applyValue(ParameterType.AMT, v));
        control.adsrKnobs()[0].value().addValueObserver(v -> applyValue(ParameterType.ENV_A, v));
        control.adsrKnobs()[1].value().addValueObserver(v -> applyValue(ParameterType.ENV_D, v));
        control.adsrKnobs()[2].value().addValueObserver(v -> applyValue(ParameterType.ENV_S, v));
        control.adsrKnobs()[3].value().addValueObserver(v -> applyValue(ParameterType.ENV_R, v));
    }
    
    public void bindSynth(Layer layer, SynthControl2 control) {
        control.cutoff().value().addValueObserver(v -> applyValue(ParameterType.CUT, v));
        control.resonance().value().addValueObserver(v -> applyValue(ParameterType.RES, v));
        control.mod().value().addValueObserver(v -> applyValue(ParameterType.MOD, v));
        control.amount().value().addValueObserver(v -> applyValue(ParameterType.AMT, v));
        control.adsrKnobs()[0].value().addValueObserver(v -> applyValue(ParameterType.ENV_A, v));
        control.adsrKnobs()[1].value().addValueObserver(v -> applyValue(ParameterType.ENV_D, v));
        control.adsrKnobs()[2].value().addValueObserver(v -> applyValue(ParameterType.ENV_S, v));
        control.adsrKnobs()[3].value().addValueObserver(v -> applyValue(ParameterType.ENV_R, v));
    }
    
    private void applyValue(ParameterType type, double v) {
        if (activeDirectDevice == null) {
            return;
        }
        final String name = activeDirectDevice.getParamName(type);
        FuseExtension.println(" %s, %s => %f ", type, name, v);
        if (name != null) {
            activeDevice.setDirectParameterValueNormalized(name, v, 1.0);
        }
    }
    
}
