package com.yaeltex.fuse;

public enum FuseAssignments {
    MIX_BASE(0x14);
    private final int code;
    
    FuseAssignments(final int code) {
        this.code = code;
    }
}
