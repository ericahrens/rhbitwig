package com.bitwig.extensions.framework.values;

import java.util.UUID;

import com.bitwig.extension.controller.api.ControllerHost;
import com.bitwig.extension.controller.api.DeviceMatcher;

public enum SpecialDevices {
    POLY_SYNTH("a9ffacb5-33e9-4fc7-8621-b1af31e410ef"), //
    POLYMER("8f58138b-03aa-4e9d-83bd-a038c99a4ed5"), //
    EQ_PLUS("e4815188-ba6f-4d14-bcfc-2dcb8f778ccb"), //
    ARPEGGIATOR("4d407a2b-c91b-4e4c-9a89-c53c19fe6251"), //
    PHOSCYON("ABCDEF019182FAEB6431366750685332", ""), //
    DRUM("8ea97e45-0255-40fd-bc7e-94419741e9d1");
    
    private final UUID uuid;
    private final String id;
    
    SpecialDevices(final String uuid) {
        this(uuid, uuid);
    }
    
    SpecialDevices(String id, final String uuid) {
        if (!uuid.isBlank()) {
            this.uuid = UUID.fromString(uuid);
        } else {
            this.uuid = null;
        }
        this.id = id;
    }
    
    public UUID getUuid() {
        return uuid;
    }
    
    public String getId() {
        return id;
    }
    
    public boolean isVstDevice() {
        return uuid == null;
    }
    
    public DeviceMatcher createMatcher(ControllerHost host) {
        if (uuid == null) {
            return host.createVST3DeviceMatcher(id);
        }
        return host.createBitwigDeviceMatcher(uuid);
    }
}
