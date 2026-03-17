package com.yaeltex.djm;

import com.bitwig.extension.controller.api.Channel;
import com.bitwig.extensions.framework.Binding;
import com.yaeltex.common.controls.VuMeter;

public class VuStereoTrackBinding extends Binding<Channel, VuMeter> {
    final VuMeter targetLeft;
    final VuMeter targetRight;
    public VuStereoTrackBinding(final Channel track, final VuMeter targetLeft, final VuMeter targetRight) {
        super(track, track, targetLeft);
        this.targetLeft = targetLeft;
        this.targetRight = targetRight;
        track.addVuMeterObserver(128, 0, true, this::handleVuLeft);
        track.addVuMeterObserver(128, 1, true, this::handleVuRight);
    }
    
    private void handleVuLeft(final int vu) {
        if (isActive()) {
            targetLeft.sendVuValue(vu);
        }
    }
    
    private void handleVuRight(final int vu) {
        if (isActive()) {
            targetRight.sendVuValue(vu);
        }
    }
    
    @Override
    protected void deactivate() {
    
    }
    
    @Override
    protected void activate() {
        targetLeft.sendVuValue(0);
        targetRight.sendVuValue(0);
    }
}
