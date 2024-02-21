package com.yaeltex.devices;

import com.bitwig.extension.controller.api.CursorRemoteControlsPage;
import com.bitwig.extension.controller.api.Track;
import com.bitwig.extensions.framework.Layer;
import com.bitwig.extensions.framework.Layers;
import com.yaeltex.common.YaeltexButtonLedState;
import com.yaeltex.controls.RgbButton;
import com.yaeltex.fuse.StripControl;
import com.yaeltex.fuse.SynthControl1;
import com.yaeltex.fuse.SynthControl2;

public class DirectDeviceControl {
    
    private final Track baseTrack;
    private final int index;
    private CursorRemoteControlsPage remotes;
    private CursorRemoteControlsPage remotes2;
    private final Layer remoteLayer;
    
    public DirectDeviceControl(final int index, final Track baseTrack, final Layers layers) {
        this.index = index;
        this.baseTrack = baseTrack;
        this.remoteLayer = new Layer(layers, "REMOTES_%d".formatted(index));
    }
    
    public void activate() {
        remoteLayer.setIsActive(true);
        if (remotes2 != null && remotes2.pageCount().get() > 1) {
            remotes2.selectedPageIndex().set(1);
        }
    }
    
    public void addSpecificBitwig(final DirectDevice type) {
        remotes = baseTrack.createCursorRemoteControlsPage("track-remotes", 8, null);
        remotes.selectedPageIndex().markInterested();
        remotes.pageCount().markInterested();
        remotes2 = baseTrack.createCursorRemoteControlsPage("track-remotes2", 8, null);
        remotes2.selectedPageIndex().markInterested();
        remotes2.pageCount().addValueObserver(pages -> {
            if (pages > 1) {
                remotes2.selectedPageIndex().set(1);
            }
        });
    }
    
    public void bindSynth(final SynthControl1 control, final StripControl stripControl) {
        remoteLayer.bind(control.cutoff(), remotes.getParameter(0));
        remoteLayer.bind(control.resonance(), remotes.getParameter(1));
        remoteLayer.bind(control.mod(), remotes.getParameter(2));
        remoteLayer.bind(control.amount(), remotes.getParameter(3));
        for (int i = 0; i < 4; i++) {
            remoteLayer.bind(control.adsrKnobs()[i], remotes.getParameter(4 + i));
        }
        remoteLayer.bind(stripControl.plusKnob(), remotes2.getParameter(0));
        remoteLayer.bind(stripControl.multKnob(), remotes2.getParameter(1));
        
        final YaeltexButtonLedState color = index == 0 ? YaeltexButtonLedState.BLUE : YaeltexButtonLedState.YELLOW;
        for (int i = 0; i < 4; i++) {
            final RgbButton button = control.buttons()[i];
            button.bindToggleValue(remoteLayer, remotes2.getParameter(4 + i), color);
        }
    }
    
    public void bindSynth(final SynthControl2 control, final StripControl stripControl) {
        remoteLayer.bind(control.cutoff(), remotes.getParameter(0));
        remoteLayer.bind(control.resonance(), remotes.getParameter(1));
        remoteLayer.bind(control.mod(), remotes.getParameter(2));
        remoteLayer.bind(control.amount(), remotes.getParameter(3));
        for (int i = 0; i < 4; i++) {
            remoteLayer.bind(control.adsrKnobs()[i], remotes.getParameter(4 + i));
        }
        remoteLayer.bind(stripControl.plusKnob(), remotes2.getParameter(0));
        remoteLayer.bind(stripControl.multKnob(), remotes2.getParameter(1));
    }
    
    public void applyValue(final ParameterType type, final double v) {
    }
    
    public int getIndex() {
        return this.index;
    }
}
