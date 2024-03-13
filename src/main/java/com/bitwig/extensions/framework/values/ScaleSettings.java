package com.bitwig.extensions.framework.values;

public class ScaleSettings {
    private final static String[] NOTES = {"  C", " C#", "  D", " D#", "  E", "  F", " F#", "  G", " G#", "  A", " A#", "  B"};

    private final ValueObject<Scale> scale = new ValueObject<>(Scale.CHROMATIC, ScaleSettings::increment,
            ScaleSettings::convert);
    private final ValueObject<KeyboardLayoutType> layoutType = new ValueObject<>(KeyboardLayoutType.ISOMORPHIC,
            ScaleSettings::incrementKeyboardType, ScaleSettings::convert);
    private final IntValueObject baseNote = new IntValueObject(0, 0, 11, v -> NOTES[v]);
    private final IntValueObject octaveOffset = new IntValueObject(4, 0, 6);
    private final IntValueObject layoutOffset = new IntValueObject(4, 3, 5);
    private final IntValueObject velocity = new IntValueObject(100, 0, 127);

    public ScaleSettings() {
        super();
    }

    private static Scale increment(final Scale current, final int amount) {
        final int ord = current.ordinal();
        final Scale[] values = Scale.values();
        final int newOrd = ord + amount;
        if (newOrd < 0) {
            return values[0];
        }
        if (newOrd >= values.length) {
            return values[values.length - 1];
        }
        return values[newOrd];
    }

    private static KeyboardLayoutType incrementKeyboardType(final KeyboardLayoutType current, final int amount) {
        final int ord = current.ordinal();
        final KeyboardLayoutType[] values = KeyboardLayoutType.values();
        final int newOrd = ord + amount;
        if (newOrd < 0) {
            return values[values.length - 1];
        }
        if (newOrd >= values.length) {
            return values[0];
        }
        return values[newOrd];
    }

    private static String convert(final KeyboardLayoutType type) {
        switch (type) {
            case ISOMORPHIC:
                return "Isomorphic";
            case IMITATE_KEYS:
                return "Piano Layout";
        }
        return "---";
    }


    private static String convert(final Scale scale) {
        return scale.getName();
    }

    public ValueObject<Scale> getScale() {
        return scale;
    }

    public IntValueObject getBaseNote() {
        return baseNote;
    }

    public String getBaseNoteString() {
        return NOTES[baseNote.get()];
    }

    public IntValueObject getOctaveOffset() {
        return octaveOffset;
    }

    public IntValueObject getLayoutOffset() {
        return layoutOffset;
    }

    public IntValueObject getVelocity() {
        return velocity;
    }

    public void modifyOctave(final int direction) {
        if (direction > 0) {
            if (octaveOffset.get() < 6) {
                octaveOffset.increment(1);
            }
        } else {
            if (octaveOffset.get() > 0) {
                octaveOffset.increment(-1);
            }
        }
    }

    public ValueObject<KeyboardLayoutType> getLayoutType() {
        return layoutType;
    }

}
