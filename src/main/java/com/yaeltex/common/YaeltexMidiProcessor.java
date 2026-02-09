package com.yaeltex.common;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.IntConsumer;

import com.bitwig.extension.controller.api.ControllerHost;
import com.bitwig.extension.controller.api.MidiIn;
import com.bitwig.extension.controller.api.MidiOut;
import com.bitwig.extension.controller.api.RelativeHardwarControlBindable;
import com.bitwig.extensions.framework.time.TimedEvent;
import com.bitwig.extensions.framework.values.Midi;

public class YaeltexMidiProcessor {
    private final Queue<TimedEvent> timedEvents = new ConcurrentLinkedQueue<>();
    protected final ControllerHost host;
    private int blinkCounter;
    
    public YaeltexMidiProcessor(final ControllerHost host, final int ports) {
        this.host = host;
        final MidiIn midiIn = host.getMidiInPort(0);
        midiIn.setMidiCallback(this::handleMidiIn);
        midiIn.setSysexCallback(this::handleSysEx);
        if (ports == 2) {
            host.getMidiInPort(1).setMidiCallback(this::handleMidiIn2);
        }
    }
    
    public void sendCcValue(final int port, final int channel, final int ccNr, final int value) {
        final MidiOut midiOut = host.getMidiOutPort(port);
        midiOut.sendMidi(Midi.CC | channel, ccNr, value);
    }
    
    public void sendCcValue(final int port, final int ccNr, final int value) {
        final MidiOut midiOut = host.getMidiOutPort(port);
        midiOut.sendMidi(Midi.CC, ccNr, value);
    }
    
    public void sendMidiNote(final int port, final int data1, final int data2) {
        final MidiOut midiOut = host.getMidiOutPort(port);
        midiOut.sendMidi(Midi.NOTE_ON, data1, data1);
    }
    
    public void sendMidi(final int port, final int status, final int data1, final int data2) {
        final MidiOut midiOut = host.getMidiOutPort(port);
        midiOut.sendMidi(status, data1, data1);
    }
    
    public void sendCcColor(final int port, final int ccNr, final int color, final int intensity) {
        final MidiOut midiOut = host.getMidiOutPort(port);
        midiOut.sendMidi(Midi.CC | 0xF, ccNr, color);
        midiOut.sendMidi(Midi.CC | 0xE, ccNr, intensity);
    }
    
    public void sendNoteColor(final int port, final int channel, final int noteNr, final YaeltexButtonLedState color) {
        final MidiOut midiOut = host.getMidiOutPort(port);
        midiOut.sendMidi(Midi.NOTE_ON | channel, noteNr, color.getColorCode());
        midiOut.sendMidi(Midi.NOTE_ON | 0xE, noteNr, color.getIntensity());
    }
    
    public void sendColorOff(final int midiPort, final int channel, final int midiId) {
        final MidiOut midiOut = host.getMidiOutPort(midiPort);
        midiOut.sendMidi(Midi.NOTE_ON | channel, midiId, 0);
    }
    
    public void start() {
        host.scheduleTask(this::handlePing, 50);
    }
    
    private void handlePing() {
        blinkCounter = (blinkCounter + 1) % 16;
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
    
    public MidiIn getMidiIn(final int port) {
        return host.getMidiInPort(port);
    }
    
    protected void handleMidiIn(final int status, final int data1, final int data2) {
        host.println("MIDI 1 => %02X %02X %02X".formatted(status, data1, data2));
    }
    
    private void handleMidiIn2(final int status, final int data1, final int data2) {
        host.println("MIDI 2 => %02X %02X %02X".formatted(status, data1, data2));
    }
    
    protected void handleSysEx(final String sysExString) {
        host.println("SysEx = %s".formatted(sysExString));
    }
    
    public RelativeHardwarControlBindable createIncrementBinder(final IntConsumer consumer) {
        return host.createRelativeHardwareControlStepTarget(//
            host.createAction(() -> consumer.accept(1), () -> "+"),
            host.createAction(() -> consumer.accept(-1), () -> "-"));
    }
    
    public RelativeHardwarControlBindable createAccelIncrementBinder(final IntConsumer consumer, final int resolution) {
        return host.createRelativeHardwareControlAdjustmentTarget(dt -> {
            consumer.accept((int) (dt * resolution));
        });
    }
    
    public YaeltexButtonLedState blinkSlow(final YaeltexButtonLedState color, final YaeltexButtonLedState offColor) {
        return blinkCounter % 16 < 8 ? color : offColor;
    }
    
    public YaeltexButtonLedState blinkSlow(final YaeltexButtonLedState color) {
        return blinkCounter % 16 < 8 ? color : YaeltexButtonLedState.OFF;
    }
    
    public YaeltexButtonLedState blinkMid(final YaeltexButtonLedState color) {
        return blinkCounter % 8 < 4 ? color : YaeltexButtonLedState.OFF;
    }
    
    public YaeltexButtonLedState blinkMid(final YaeltexButtonLedState color, final YaeltexButtonLedState offColor) {
        return blinkCounter % 8 < 4 ? color : offColor;
    }
    
    public YaeltexButtonLedState blinkFast(final YaeltexButtonLedState color) {
        return blinkCounter % 4 < 2 ? color : YaeltexButtonLedState.OFF;
    }
    
    
    public void delayAction(final Runnable action, final int time) {
        host.scheduleTask(action, time);
    }
    
    
}
