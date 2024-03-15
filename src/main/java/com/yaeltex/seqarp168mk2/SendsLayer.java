package com.yaeltex.seqarp168mk2;

import com.bitwig.extension.controller.api.Send;
import com.bitwig.extension.controller.api.Track;
import com.bitwig.extension.controller.api.TrackBank;
import com.bitwig.extensions.framework.Layer;
import com.bitwig.extensions.framework.Layers;
import com.bitwig.extensions.framework.di.Component;
import com.yaeltex.common.YaeltexButtonLedState;
import com.yaeltex.controls.RgbButton;
import com.yaeltex.controls.RingEncoder;
import com.yaeltex.fuse.SendMode;

@Component
public class SendsLayer extends Layer {
    
    private final YaeltexButtonLedState[] trackColors = new YaeltexButtonLedState[8];
    private final SendMode[][] modeStates = new SendMode[8][4];
    private final boolean[][] preFaderStates = new boolean[8][4];
    private final boolean[][] sendExists = new boolean[8][4];
    private final boolean[] trackExists = new boolean[8];
    
    public SendsLayer(final Layers layers, final SeqArpHardwareElements hwElements,
        final BitwigViewControl viewControl) {
        super(layers, "SENDS_LAYER");
        final TrackBank trackBank = viewControl.getTrackBank();
        for (int i = 0; i < 8; i++) {
            final int trackIndex = i;
            final Track track = trackBank.getItemAt(i);
            track.color().addValueObserver((r, g, b) -> trackColors[trackIndex] = YaeltexButtonLedState.of(r, g, b));
            track.exists().addValueObserver(exists -> trackExists[trackIndex] = exists);
            for (int j = 0; j < 4; j++) {
                final int sendIndex = j;
                final Send send = track.sendBank().getItemAt(sendIndex);
                final RingEncoder encoder = hwElements.getEncoder(sendIndex * 8 + trackIndex);
                bindControl(trackIndex, sendIndex, track, send, encoder);
            }
        }
    }
    
    private void bindControl(final int trackIndex, final int sendIndex, final Track track, final Send send,
        final RingEncoder encoder) {
        send.sendMode().addValueObserver(enumRaw -> modeStates[trackIndex][sendIndex] = SendMode.toMode(enumRaw));
        send.isPreFader().addValueObserver(preFader -> preFaderStates[trackIndex][sendIndex] = preFader);
        send.exists().addValueObserver(exists -> sendExists[trackIndex][sendIndex] = exists);
        encoder.bindLight(this, () -> sendExists[trackIndex][sendIndex] && trackExists[trackIndex]
            ? trackColors[trackIndex]
            : YaeltexButtonLedState.OFF);
        encoder.bindValue(this, send.value(), track.exists(), 100);
        final RgbButton button = encoder.getButton();
        button.bindLight(this, () -> getSendSate(trackIndex, sendIndex));
        button.bindPressed(this, () -> changeSendState(send, trackIndex, sendIndex));
    }
    
    private void changeSendState(final Send send, final int trackIndex, final int sendIndex) {
        final SendMode newState = modeStates[trackIndex][sendIndex].toggle();
        send.sendMode().set(newState.getEnumRaw());
    }
    
    private YaeltexButtonLedState getSendSate(final int trackIndex, final int sendIndex) {
        if (!sendExists[trackIndex][sendIndex] || !trackExists[trackIndex]) {
            return YaeltexButtonLedState.OFF;
        }
        return preFaderStates[trackIndex][sendIndex] ? YaeltexButtonLedState.AQUA : YaeltexButtonLedState.YELLOW;
    }
    
}
