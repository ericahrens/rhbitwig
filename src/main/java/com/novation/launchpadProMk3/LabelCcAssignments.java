package com.novation.launchpadProMk3;

import com.bitwig.extension.controller.api.HardwareActionMatcher;
import com.bitwig.extension.controller.api.MidiIn;

public enum LabelCcAssignments {
    RECORD_ARM_UNDO(1), //
    MUTE_REDO(2), //
    SOLO_CLICK(3), //
    VOLUME(4), //
    PAN(5), //
    SENDS_TAP(6), //
    DEVICE_TEMPO(7), //
    STOP_CLIP_SWING(8), //
    SHIFT(90), //
    TRACK_SEL_1(101, false), //
    TRACK_SEL_2(102, false), //
    TRACK_SEL_3(103, false), //
    TRACK_SEL_4(104, false), //
    TRACK_SEL_5(105, false), //
    TRACK_SEL_6(106, false), //
    TRACK_SEL_7(107, false), //
    TRACK_SEL_8(107, false), //
    R1_PATTERNS(89, false), //
    R2_STEPS(79, false), //
    R3_PAT_SETTINGS(69, false), //
    R4_VELOCITY(59, false), //
    R5_PROBABILITY(49, false), //
    R6_MUTATION(39, false), //
    R7_MICROSTEP(29, false), //
    R8_PRINT_TO_CLIP(19, false), //
    REC(10), //
    PLAY(20), //
    FIXED_LENGTH(30), //
    QUANTIZE(40), //
    DUPLICATE(50),
    CLEAR(60),
    DOWN(70),
    UP(80),
    LEFT(91), //
    RIGHT(92), //
    SESSION(93), //
    NOTE(94), //
    CHORD(95), //
    CUSTOM(96), //
    SEQUENCER(97), //
    PROJECTS(98);

    private final int ccValue;
    private final boolean isDedicated;

    LabelCcAssignments(final int ccValue) {
        this(ccValue, true);

    }

    LabelCcAssignments(final int ccValue, boolean isDedicated) {
        this.ccValue = ccValue;
        this.isDedicated = isDedicated;
    }

    public int getCcValue() {
        return ccValue;
    }

    public boolean isDedicated() {
        return isDedicated;
    }

    public HardwareActionMatcher createMatcher(final MidiIn midiIn, final int matchValue) {
        return midiIn.createCCActionMatcher(0, ccValue, matchValue);
    }
}
