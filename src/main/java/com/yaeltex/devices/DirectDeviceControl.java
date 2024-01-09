package com.yaeltex.devices;

import java.util.HashMap;
import java.util.Map;

import com.bitwig.extension.controller.api.ControllerHost;
import com.bitwig.extension.controller.api.CursorDevice;
import com.bitwig.extension.controller.api.CursorRemoteControlsPage;
import com.bitwig.extension.controller.api.Device;
import com.bitwig.extension.controller.api.DeviceBank;
import com.bitwig.extension.controller.api.DeviceMatcher;
import com.bitwig.extension.controller.api.Track;
import com.bitwig.extensions.framework.Layer;
import com.bitwig.extensions.framework.Layers;
import com.bitwig.extensions.framework.values.BooleanValueObject;
import com.bitwig.extensions.framework.values.SpecialDevices;
import com.yaeltex.bindings.DirectParameterBinding;
import com.yaeltex.fuse.SynthControl1;
import com.yaeltex.fuse.SynthControl2;

public class DirectDeviceControl {
    
    private final ControllerHost host;
    private final Map<SpecialDevices, Object> deviceLookup = new HashMap<>();
    private final Track baseTrack;
    private final int index;
    private DeviceState activeDevice;
    private CursorRemoteControlsPage remotes;
    private final BooleanValueObject specDeviceAvailable = new BooleanValueObject();
    private final Layer directLayer;
    private final Layer remoteLayer;
    
    public DirectDeviceControl(int index, Track baseTrack, ControllerHost host, Layers layers) {
        this.index = index;
        
        this.baseTrack = baseTrack;
        this.host = host;
        this.directLayer = new Layer(layers, "DIRECT_%d".formatted(index));
        this.remoteLayer = new Layer(layers, "REMOTES_%d".formatted(index));
        specDeviceAvailable.addValueObserver(available -> {
            directLayer.setIsActive(available);
            remoteLayer.setIsActive(!available);
        });
    }
    
    public void activate() {
        final boolean available = specDeviceAvailable.get();
        directLayer.setIsActive(available);
        remoteLayer.setIsActive(!available);
    }
    
    public void addSpecificBitwig(DirectDevice type) {
        final DeviceMatcher matcher = type.createMatcher(host);
        final DeviceBank matcherBank = baseTrack.createDeviceBank(1);
        final Device device = matcherBank.getDevice(0);
        matcherBank.setDeviceMatcher(matcher);
        host.createInstrumentMatcher();
        final CursorDevice cursorDevice = baseTrack.createCursorDevice();
        final DeviceState deviceState = new DeviceState(type, device);
        
        remotes = cursorDevice.createCursorRemoteControlsPage(8);
        device.exists().addValueObserver(exist -> {
            activeDevice = exist ? deviceState : null;
            specDeviceAvailable.set(exist);
        });
        
        device.addDirectParameterIdObserver(parameters -> {
            // need the observer
        });
        device.addDirectParameterNormalizedValueObserver((id, value) -> {
            type.getParamType(id).ifPresent(parameterType -> {
                deviceState.updateValue(parameterType, value);
            });
        });
    }
    
    public void bindSynth(SynthControl1 control) {
        directLayer.addBinding(new DirectParameterBinding(control.cutoff(), ParameterType.CUT, this));
        directLayer.addBinding(new DirectParameterBinding(control.resonance(), ParameterType.RES, this));
        directLayer.addBinding(new DirectParameterBinding(control.mod(), ParameterType.MOD, this));
        directLayer.addBinding(new DirectParameterBinding(control.amount(), ParameterType.AMT, this));
        directLayer.addBinding(new DirectParameterBinding(control.adsrKnobs()[0], ParameterType.ENV_A, this));
        directLayer.addBinding(new DirectParameterBinding(control.adsrKnobs()[1], ParameterType.ENV_D, this));
        directLayer.addBinding(new DirectParameterBinding(control.adsrKnobs()[2], ParameterType.ENV_S, this));
        directLayer.addBinding(new DirectParameterBinding(control.adsrKnobs()[3], ParameterType.ENV_R, this));
        
        remoteLayer.bind(control.cutoff(), remotes.getParameter(0));
        remoteLayer.bind(control.resonance(), remotes.getParameter(1));
        remoteLayer.bind(control.mod(), remotes.getParameter(2));
        remoteLayer.bind(control.amount(), remotes.getParameter(3));
        for (int i = 0; i < 4; i++) {
            remoteLayer.bind(control.adsrKnobs()[i], remotes.getParameter(4 + i));
        }
    }
    
    public void bindSynth(SynthControl2 control) {
        directLayer.addBinding(new DirectParameterBinding(control.cutoff(), ParameterType.CUT, this));
        directLayer.addBinding(new DirectParameterBinding(control.resonance(), ParameterType.RES, this));
        directLayer.addBinding(new DirectParameterBinding(control.mod(), ParameterType.MOD, this));
        directLayer.addBinding(new DirectParameterBinding(control.amount(), ParameterType.AMT, this));
        directLayer.addBinding(new DirectParameterBinding(control.adsrKnobs()[0], ParameterType.ENV_A, this));
        directLayer.addBinding(new DirectParameterBinding(control.adsrKnobs()[1], ParameterType.ENV_D, this));
        directLayer.addBinding(new DirectParameterBinding(control.adsrKnobs()[2], ParameterType.ENV_S, this));
        directLayer.addBinding(new DirectParameterBinding(control.adsrKnobs()[3], ParameterType.ENV_R, this));
        remoteLayer.bind(control.cutoff(), remotes.getParameter(0));
        remoteLayer.bind(control.resonance(), remotes.getParameter(1));
        remoteLayer.bind(control.mod(), remotes.getParameter(2));
        remoteLayer.bind(control.amount(), remotes.getParameter(3));
        for (int i = 0; i < 4; i++) {
            remoteLayer.bind(control.adsrKnobs()[i], remotes.getParameter(4 + i));
        }
    }
    
    public void applyValue(ParameterType type, double v) {
        if (activeDevice == null) {
            return;
        }
        activeDevice.applyValue(type, v);
    }
    
    public int getIndex() {
        return this.index;
    }
}
