package com.yaeltex.common.devices;

import java.util.UUID;

public enum SpecialDevices {
    EQ_PLUS("e4815188-ba6f-4d14-bcfc-2dcb8f778ccb"), //
    ARPEGGIATOR("4d407a2b-c91b-4e4c-9a89-c53c19fe6251"), //
    DRUM("8ea97e45-0255-40fd-bc7e-94419741e9d1");
    
    private final UUID uuid;
    
    SpecialDevices(final String uuid) {
        this.uuid = UUID.fromString(uuid);
    }
    
    public UUID getUuid() {
        return uuid;
    }
}
