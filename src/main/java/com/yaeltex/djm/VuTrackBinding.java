package com.yaeltex.djm;

import com.bitwig.extension.controller.api.Channel;
import com.bitwig.extensions.framework.Binding;
import com.yaeltex.common.controls.VuMeter;

public class VuTrackBinding extends Binding<Channel, VuMeter> {
    
    private int value;
    
    public VuTrackBinding(final Channel track, final VuMeter target) {
        super(track, track, target);
        track.volume().value().addValueObserver(128, this::handleValueChanged);
        track.addVuMeterObserver(128, -1, true, this::handleVu);
    }
    
    private void handleVu(final int vu) {
        if (isActive()) {
            getTarget().sendVuValue(vu);
        }
    }
    
    private void handleValueChanged(final int value) {
        this.value = value;
        if (isActive()) {
            getTarget().sendEncoderValue(value);
        }
    }
    
    @Override
    protected void deactivate() {
    
    }
    
    @Override
    protected void activate() {
        getTarget().sendEncoderValue(value);
    }
}
