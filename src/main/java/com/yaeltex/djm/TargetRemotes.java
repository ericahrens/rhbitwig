package com.yaeltex.djm;

import java.util.HashMap;
import java.util.Map;

import com.bitwig.extension.controller.api.Track;

public class TargetRemotes<T extends Enum<T> & PageIdent> {
    
    private final Map<T, RemoteFixed> remotes = new HashMap();
    
    public TargetRemotes(final Track sourceTrack, final Class<T> pageEnum) {
        
        final T[] constants = pageEnum.getEnumConstants();
        
        for (int i = 0; i < constants.length; i++) {
            final RemoteFixed fixedRemote = new RemoteFixed(i, constants[i].getName(), sourceTrack);
            this.remotes.put(constants[i], fixedRemote);
        }
    }
    
    public RemoteFixed getRemotes(final T pageConstant) {
        return this.remotes.get(pageConstant);
    }
    
}
