package com.yaeltex.seqarp168mk2;

import com.bitwig.extension.controller.api.CursorRemoteControlsPage;
import com.bitwig.extension.controller.api.CursorTrack;
import com.bitwig.extension.controller.api.PinnableCursorDevice;
import com.bitwig.extension.controller.api.RemoteControl;
import com.bitwig.extension.controller.api.Track;
import com.bitwig.extensions.framework.Layer;
import com.bitwig.extensions.framework.Layers;
import com.bitwig.extensions.framework.di.Component;
import com.yaeltex.common.YaeltexButtonLedState;
import com.yaeltex.common.controls.RingEncoder;

@Component
public class RemotesLayer extends Layer {
    
    private final BitwigViewControl viewControl;
    private final CursorTrack cursorTrack;
    private RemoteMode mode = RemoteMode.DEVICE;
    private final Layer trackRemoteLayer;
    private Layer currentLayer;
    private final Layer deviceRemoteLayer;
    private final Layer projectRemoteLayer;
    
    private final YaeltexButtonLedState[] PARAM_COLORS = {
        YaeltexButtonLedState.RED,
        YaeltexButtonLedState.ORANGE,
        YaeltexButtonLedState.YELLOW,
        YaeltexButtonLedState.GREEN,
        YaeltexButtonLedState.AQUA,
        YaeltexButtonLedState.BLUE,
        YaeltexButtonLedState.PURPLE,
        YaeltexButtonLedState.WHITE,
    };
    
    enum RemoteMode {
        DEVICE,
        TRACK,
        PROJECT
    }
    
    public RemotesLayer(final Layers layers, final SeqArpHardwareElements hwElements,
        final BitwigViewControl viewControl) {
        super(layers, "REMOTES_LAYER");
        this.viewControl = viewControl;
        cursorTrack = viewControl.getCursorTrack();
        final PinnableCursorDevice cursorDevice = viewControl.getCursorDevice();
        final Track rootTrack = viewControl.getRootTrack();
        final RemotesGroup deviceGroup = new RemotesGroup("DEVICE",
            i -> cursorDevice.createCursorRemoteControlsPage("TRACK_%d".formatted(i + 1), 8, null), 4);
        final RemotesGroup trackGroup = new RemotesGroup("TRACK",
            i -> cursorTrack.createCursorRemoteControlsPage("TRACK_%d".formatted(i + 1), 8, null), 4);
        final RemotesGroup projectGroup = new RemotesGroup("PROJECT",
            i -> rootTrack.createCursorRemoteControlsPage("TRACK_%d".formatted(i + 1), 8, null), 4);
        
        this.trackRemoteLayer = new Layer(layers, "TRACK_REMOTES");
        this.deviceRemoteLayer = new Layer(layers, "DEVICE_REMOTES");
        this.projectRemoteLayer = new Layer(layers, "PROJECT_REMOTES");
        
        this.currentLayer = deviceRemoteLayer;
        assignRemotes(hwElements, trackGroup, trackRemoteLayer);
        assignRemotes(hwElements, deviceGroup, deviceRemoteLayer);
        assignRemotes(hwElements, projectGroup, projectRemoteLayer);
    }
    
    private void assignRemotes(final SeqArpHardwareElements hwElements, final RemotesGroup trackGroup,
        final Layer layer) {
        for (int i = 0; i < 4; i++) {
            final CursorRemoteControlsPage remotes = trackGroup.getRemotes(i);
            for (int j = 0; j < 8; j++) {
                final RingEncoder encoder = hwElements.getEncoder(j + (3 - i) * 8);
                final RemoteControl parameter = remotes.getParameter(j);
                final YaeltexButtonLedState paramColor = PARAM_COLORS[j];
                parameter.exists().markInterested();
                encoder.bind(layer, parameter);
                encoder.bindLight(layer, () -> parameter.exists().get() ? paramColor : YaeltexButtonLedState.OFF);
            }
        }
    }
    
    public RemoteMode getMode() {
        return mode;
    }
    
    public void setMode(final boolean pressed, final RemoteMode mode) {
        if (!pressed || this.mode == mode) {
            return;
        }
        this.mode = mode;
        currentLayer.setIsActive(false);
        currentLayer = switch (mode) {
            case TRACK -> trackRemoteLayer;
            case PROJECT -> projectRemoteLayer;
            case DEVICE -> deviceRemoteLayer;
        };
        currentLayer.setIsActive(true);
    }
    
    @Override
    protected void onActivate() {
        super.onActivate();
        currentLayer.setIsActive(true);
    }
    
    @Override
    protected void onDeactivate() {
        currentLayer.setIsActive(false);
    }
}
