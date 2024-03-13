package com.yaeltex.seqarp168mk2.device;

import java.util.Arrays;

import com.bitwig.extension.controller.api.Parameter;
import com.yaeltex.seqarp168.EncoderUtil;
import com.yaeltex.seqarp168.QuantizeMode;

/**
 * Represents an single instance of an arpeggiator device. Contains and manages
 * states that are not in the device itself. These being
 * <ul>
 * <li>stored gate values
 * <li>stored velocity values
 * <li>separate offset note value and base note value as set by knob/slider
 * <li>quantize layout
 * <li>gate mute state (button and/or due to quantization)
 * </ul>
 */
public class ArpInstance {
    
    private static final int NUMBER_OF_STEPS = 16;
    public static int GATE_NOMUTE = 0;
    public static int GATE_BUTTON_MUTE = 1;
    public static int GATE_QUANTIZE_MUTE = 2;
    public static int GATE_BUTTON_QUANTIZE_MUTE = 3;
    private static final int BASE_NOTE_RANGE = 24;
    
    private final int[] gateMuteState = new int[NUMBER_OF_STEPS];
    private final int[] velocityMuteState = new int[NUMBER_OF_STEPS];
    
    private final int[] baseNote = new int[NUMBER_OF_STEPS]; // Internal base note
    private final int[] offsetNote = new int[NUMBER_OF_STEPS]; // Internal offset note
    
    private String presetName;
    private String trackName;
    
    private final boolean[] quanitze = new boolean[12];
    // private final ArpParameterContainer parameterContainer;
    private QuantizeMode quantizeMode = QuantizeMode.NEAREST_VALUE;
    private boolean isInitialized = true;
    
    public ArpInstance(final String trackName, final String presetName, final QuantizeMode mode) {
        this.presetName = presetName;
        this.trackName = trackName;
        
        Arrays.fill(gateMuteState, -1);
        Arrays.fill(velocityMuteState, -1);
        Arrays.fill(quanitze, true);
        this.quantizeMode = mode;
    }
    
    public int getNoteOffset(final int index) {
        return offsetNote[index];
    }
    
    public void setBaseNote(final int index, final int value) {
        baseNote[index] = value;
    }
    
    public void changeBaseNote(final int index, final int offset) {
        baseNote[index] = Math.min(BASE_NOTE_RANGE, Math.max(baseNote[index] + offset, -BASE_NOTE_RANGE));
    }
    
    public void changeNoteOffset(final int index, final int offset) {
        offsetNote[index] = Math.min(EncoderUtil.OFFSET_NOTE_RANGE,
            Math.max(offsetNote[index] + offset, -EncoderUtil.OFFSET_NOTE_RANGE));
    }
    
    public void resetNoteOffset(final int index) {
        offsetNote[index] = 0;
    }
    
    /**
     * According to mode this returns the current note values set by base note and
     * offset.
     *
     * @param index index of note
     * @return note value (possibly quantized) normalized between 0 and 1;
     */
    public double getNoteValueAct(final int index) {
        if (quantizeMode == QuantizeMode.MUTE) {
            return (Math.min(Math.max(-24, baseNote[index] + offsetNote[index]), 24) + 24.0) / 48.0;
        } else {
            final int sum = Math.min(Math.max(-24, baseNote[index] + offsetNote[index]), 24);
            return (quantizeToNearest(sum) + 24) / 48;
        }
    }
    
    protected void toggleGate(final int index, final Parameter parameter) {
        final int actualValue = (int) (parameter.getRaw() * 100);
        final int prevState = gateMuteState[index];
        if (prevState == -1) { // NOT Muted
            gateMuteState[index] = actualValue;
            parameter.value().set(0);
        } else { // IS Muted
            final double v = gateMuteState[index] / 100.0;
            parameter.value().set(Math.max(0, v));
            gateMuteState[index] = -1;
        }
    }
    
    protected void toggleVelocity(final int index, final Parameter parameter) {
        final int actualValue = (int) (parameter.getRaw() * 100);
        final int prevState = velocityMuteState[index];
        if (prevState == -1) { // NOT Muted
            velocityMuteState[index] = actualValue;
            parameter.value().set(0);
        } else { // IS Muted
            final double v = velocityMuteState[index] / 100.0;
            parameter.value().set(v);
            velocityMuteState[index] = -1;
        }
    }
    
    public void updateVelocityValue(final int index, final int newValue) {
        if (velocityMuteState[index] != -1) {
            velocityMuteState[index] = newValue;
        }
    }
    
    public void updateGateValue(final int index, final int newValue) {
        if (gateMuteState[index] != -1) {
            gateMuteState[index] = newValue;
        }
    }
    
    public boolean matches(final String trackName, final String presetName) {
        return trackName.equals(this.trackName) && presetName.equals(this.presetName);
    }
    
    public String getPresetName() {
        return presetName;
    }
    
    public void setPresetName(final String name) {
        this.presetName = name;
    }
    
    public String getTrackName() {
        return trackName;
    }
    
    public void setTrackName(final String trackName) {
        this.trackName = trackName;
    }
    
    public void setOffsetNote(final int index, final int value) {
        this.offsetNote[index] = value;
        //updateNoteMuteState(index);
    }
    
    private boolean inScale(final int index) {
        final int value = Math.min(Math.max(-24, baseNote[index] + offsetNote[index]), 24);
        return quanitze[(value + 24) % 12];
    }
    
    private double quantizeToNearest(final int value) {
        final int nv = (value + 24) % 12;
        if (quanitze[nv]) {
            return value;
        }
        int vu = value;
        while (vu < 24) {
            if (quanitze[(vu + 24) % 12]) {
                return vu;
            }
            vu++;
        }
        vu = value;
        while (vu > -25) {
            if (quanitze[(vu + 24) % 12]) {
                return vu;
            }
            vu--;
        }
        return value;
    }
    
    public boolean isGateMuted(final int index) {
        return gateMuteState[index] != -1;
    }
    
    public boolean isVelocityMuted(final int index) {
        return velocityMuteState[index] != -1;
    }
    
    public int getGateMute(final int index) {
        return gateMuteState[index];
    }
    
    public int getVelMute(final int index) {
        return velocityMuteState[index];
    }
    
    public boolean isQuantizeNoteSet(final int note) {
        return quanitze[note];
    }
    
    public void toggleQuantizeNote(final int note) {
        quanitze[note] = !quanitze[note];
        for (int i = 0; i < NUMBER_OF_STEPS; i++) {
            updateNoteMuteState(i);
        }
    }
    
    private void updateNoteMuteState(final int index) {
        final int prevState = gateMuteState[index];
        if (inScale(index)) {
            //gateMuteState[index] &= ~GATE_QUANTIZE_MUTE;
        } else {
            //gateMuteState[index] |= GATE_QUANTIZE_MUTE;
        }
    }
    
    public void applyValues(final BitwigArpDevice arpDevice) {
        if (isInitialized) {
            for (int i = 0; i < 16; i++) {
                final int noteValue = (int) arpDevice.getNoteParam(i).getRaw();
                final NoteControlValue noteOver = arpDevice.getNoteValues(i);
                baseNote[i] = noteValue;
                noteOver.setBaseValue(noteValue);
                noteOver.setOffsetValue(0);
            }
            isInitialized = false;
        } else {
            for (int i = 0; i < 16; i++) {
                final NoteControlValue noteValue = arpDevice.getNoteValues(i);
                noteValue.setBaseValue(baseNote[i]);
                noteValue.setOffsetValue(offsetNote[i]);
            }
        }
    }
    
}
