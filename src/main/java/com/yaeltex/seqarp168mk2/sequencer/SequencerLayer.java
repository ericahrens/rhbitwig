package com.yaeltex.seqarp168mk2.sequencer;

import java.util.Arrays;

import com.bitwig.extension.controller.api.ClipLauncherSlot;
import com.bitwig.extension.controller.api.ClipLauncherSlotBank;
import com.bitwig.extension.controller.api.CursorTrack;
import com.bitwig.extension.controller.api.DrumPad;
import com.bitwig.extension.controller.api.DrumPadBank;
import com.bitwig.extension.controller.api.InternalHardwareLightState;
import com.bitwig.extension.controller.api.PlayingNote;
import com.bitwig.extensions.framework.Layer;
import com.bitwig.extensions.framework.Layers;
import com.bitwig.extensions.framework.di.Component;
import com.yaeltex.common.YaeltexButtonLedState;
import com.yaeltex.controls.RingEncoder;
import com.yaeltex.seqarp168mk2.BitwigViewControl;
import com.yaeltex.seqarp168mk2.SeqArp168Extension;
import com.yaeltex.seqarp168mk2.SeqArpHardwareElements;

@Component
public class SequencerLayer extends Layer {
    
    private final BitwigViewControl viewControl;
    private final boolean[] notesPlaying = new boolean[16];
    private final YaeltexButtonLedState[] padColors = new YaeltexButtonLedState[16];
    private final YaeltexButtonLedState[] slotColors = new YaeltexButtonLedState[8];
    private YaeltexButtonLedState trackColor;
    private int noteOffset = 0;
    private int selectedIndex = -1;
    
    
    public SequencerLayer(final Layers layers, final SeqArpHardwareElements hwElements,
        final BitwigViewControl viewControl) {
        super(layers, "SEQUENCER_LAYER");
        this.viewControl = viewControl;
        final DrumPadBank drumPadBank = this.viewControl.getDrumPadBank();
        drumPadBank.scrollPosition().addValueObserver(position -> {
            this.noteOffset = position;
            SeqArp168Extension.println(" POS = %s", position);
        });
        final CursorTrack cursorTrack = this.viewControl.getCursorTrack();
        cursorTrack.color().addValueObserver((r, g, b) -> trackColor = YaeltexButtonLedState.of(r, g, b));
        cursorTrack.playingNotes().addValueObserver(notes -> handleNotesPlaying(notes));
        for (int i = 0; i < drumPadBank.getSizeOfBank(); i++) {
            final int index = i;
            final RingEncoder encoder = hwElements.getEncoder(i);
            final DrumPad drumPad = drumPadBank.getItemAt(i);
            drumPad.color().addValueObserver((r, g, b) -> padColors[index] = YaeltexButtonLedState.of(r, g, b));
            drumPad.addIsSelectedInEditorObserver(selected -> {
                if (selected) {
                    selectedIndex = index;
                }
            });
            encoder.getButton().bindLight(this, () -> getPadColorState(index));
            encoder.getButton().bindPressed(this, () -> {
                drumPad.selectInEditor();
            });
            
            encoder.bindLight(this, () -> getPadColorState(index));
        }
        final ClipLauncherSlotBank slotBank = cursorTrack.clipLauncherSlotBank();
        for (int i = 0; i < slotBank.getSizeOfBank(); i++) {
            final int index = i;
            final ClipLauncherSlot slot = slotBank.getItemAt(i);
            
            prepareSlot(index, slot);
            final RingEncoder encoder = hwElements.getEncoder(i + 16);
            encoder.getButton().bindLight(this, () -> getSlotColor(index, slot));
            encoder.getButton().bindIsPressed(this, pressed -> handleSlot(pressed, index, slot));
        }
    }
    
    private void handleSlot(final boolean pressed, final int index, final ClipLauncherSlot slot) {
        if (pressed && slot.hasContent().get()) {
            slot.launch();
        }
    }
    
    private void prepareSlot(final int index, final ClipLauncherSlot slot) {
        slot.exists().markInterested();
        slot.isPlaying().markInterested();
        slot.hasContent().markInterested();
        slot.isPlaybackQueued().markInterested();
        slot.isStopQueued().markInterested();
        slot.color().addValueObserver((r, g, b) -> slotColors[index] = YaeltexButtonLedState.of(r, g, b));
    }
    
    private InternalHardwareLightState getSlotColor(final int index, final ClipLauncherSlot slot) {
        if (slot.hasContent().get()) {
            return slotColors[index];
        }
        return YaeltexButtonLedState.OFF;
    }
    
    private YaeltexButtonLedState getPadColorState(final int index) {
        if (notesPlaying[index]) {
            if (padColors[index] == YaeltexButtonLedState.OFF) {
                return trackColor;
            }
            return padColors[index];
        }
        if (index == selectedIndex) {
            return YaeltexButtonLedState.WHITE;
        }
        return notesPlaying[index] ? YaeltexButtonLedState.GREEN : YaeltexButtonLedState.OFF;
    }
    
    private void handleNotesPlaying(final PlayingNote[] notes) {
        Arrays.fill(notesPlaying, false);
        for (final PlayingNote note : notes) {
            final int index = note.pitch() - noteOffset;
            if (index >= 0 && index < 16) {
                notesPlaying[index] = true;
            }
        }
    }
}
