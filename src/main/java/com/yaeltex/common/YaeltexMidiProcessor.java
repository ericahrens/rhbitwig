package com.yaeltex.common;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.IntConsumer;

import com.bitwig.extension.controller.api.ControllerHost;
import com.bitwig.extension.controller.api.MidiIn;
import com.bitwig.extension.controller.api.MidiOut;
import com.bitwig.extension.controller.api.NoteInput;
import com.bitwig.extension.controller.api.RelativeHardwarControlBindable;
import com.bitwig.extensions.framework.time.TimedEvent;
import com.yaeltex.fuse.FuseExtension;

public class YaeltexMidiProcessor {
    private final MidiIn midiIn;
    private final MidiOut midiOut;
    private final Queue<TimedEvent> timedEvents = new ConcurrentLinkedQueue<>();
    private final ControllerHost host;
    
    public YaeltexMidiProcessor(final ControllerHost host) {
        this.host = host;
        this.midiIn = host.getMidiInPort(0);
        this.midiOut = host.getMidiOutPort(0);
        midiIn.setMidiCallback(this::handleMidiIn);
        midiIn.setSysexCallback(this::handleSysEx);
    }
    
    public NoteInput createNoteInput(String name, String... mask) {
        return midiIn.createNoteInput(name, mask);
    }
    
    public void sendMidi(final int status, final int val1, final int val2) {
        midiOut.sendMidi(status, val1, val2);
    }
    
    public void start() {
        host.scheduleTask(this::handlePing, 50);
    }
    
    private void handlePing() {
        if (!timedEvents.isEmpty()) {
            for (final TimedEvent event : timedEvents) {
                event.process();
                if (event.isCompleted()) {
                    timedEvents.remove(event);
                }
            }
        }
        host.scheduleTask(this::handlePing, 50);
    }
    
    public void queueEvent(final TimedEvent event) {
        timedEvents.add(event);
    }
    
    public MidiIn getMidiIn() {
        return midiIn;
    }
    
    private void handleMidiIn(final int status, final int data1, final int data2) {
        FuseExtension.println("MIDI => %02X %02X %02X", status, data1, data2);
    }
    
    protected void handleSysEx(final String sysExString) {
        FuseExtension.println("SysEx = %s", sysExString);
    }
    
    public RelativeHardwarControlBindable createIncrementBinder(final IntConsumer consumer) {
        return host.createRelativeHardwareControlStepTarget(//
            host.createAction(() -> consumer.accept(1), () -> "+"),
            host.createAction(() -> consumer.accept(-1), () -> "-"));
    }
    
    
}
