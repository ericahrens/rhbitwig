package com.yaeltex.djm;

import com.bitwig.extension.controller.api.Channel;
import com.bitwig.extensions.framework.Binding;
import com.yaeltex.common.controls.VuMeter;

public class VuTrackBinding extends Binding<Channel, VuMeter> {
    
    public int lastSentValue = 0;
    
    public VuTrackBinding(final Channel track, final VuMeter target) {
        super(track, track, target);
        track.addVuMeterObserver(128, -1, true, this::handleVu);
    }
    
    private void handleVu(final int vu) {
        if (isActive() && vu != lastSentValue) {
            getTarget().sendVuValue(vu);
            lastSentValue = vu;
        }
    }
    
    @Override
    protected void deactivate() {
    
    }
    
    @Override
    protected void activate() {
        getTarget().sendVuValue(0);
    }
}
