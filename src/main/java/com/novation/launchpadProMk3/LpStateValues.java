package com.novation.launchpadProMk3;

import com.bitwig.extensions.framework.values.BooleanValueObject;

public class LpStateValues {
    private final BooleanValueObject shiftModeActive = new BooleanValueObject();
    private boolean shiftFunctionInvoked = false;
    private boolean shiftBeingHeld = false;
    private boolean enteredShiftMode = false;

    private final BooleanValueObject clearButtonPressed = new BooleanValueObject();
    private final BooleanValueObject duplicateButtonPressed = new BooleanValueObject();
    private final BooleanValueObject muteButtonPressed = new BooleanValueObject();
    private final BooleanValueObject soloButtonPressed = new BooleanValueObject();
    private final BooleanValueObject noteRepeatActive = new BooleanValueObject();
    private final BooleanValueObject volumeButtonPressed = new BooleanValueObject();

    public BooleanValueObject getClearButtonPressed() {
        return clearButtonPressed;
    }

    public BooleanValueObject getShiftModeActive() {
        return shiftModeActive;
    }

    public boolean isOnlyShiftActive() {
        return shiftModeActive.get() && !clearButtonPressed.get() && !soloButtonPressed.get() //
                && !muteButtonPressed.get() && !duplicateButtonPressed.get();
    }

    public boolean isNoModifiersActive() {
        return !shiftModeActive.get() && !clearButtonPressed.get() && !soloButtonPressed.get() //
                && !muteButtonPressed.get() && !duplicateButtonPressed.get();
    }

    public void handleShiftPressed(final boolean pressed) {
        if (shiftModeActive.get()) {
            if (!pressed) {
                if (shiftFunctionInvoked || !enteredShiftMode) {
                    shiftModeActive.set(false);
                    shiftFunctionInvoked = false;
                }
                enteredShiftMode = false;
            }
        } else {
            if (pressed) {
                shiftModeActive.set(true);
                enteredShiftMode = true;
            } else {
                if (!enteredShiftMode) {
                    shiftModeActive.set(false);
                }
            }
        }
        shiftBeingHeld = pressed;
    }

    public BooleanValueObject getDuplicateButtonPressed() {
        return duplicateButtonPressed;
    }

    public BooleanValueObject getMuteButtonPressed() {
        return muteButtonPressed;
    }

    public BooleanValueObject getSoloButtonPressed() {
        return soloButtonPressed;
    }

    public BooleanValueObject getNoteRepeatActive() {
        return noteRepeatActive;
    }

    public BooleanValueObject getVolumeButtonPressed() {
        return volumeButtonPressed;
    }

    public void notifyShiftFunctionInvoked() {
        if (shiftBeingHeld) {
            shiftFunctionInvoked = true;
        } else {
            shiftModeActive.set(false);
        }
    }

}
