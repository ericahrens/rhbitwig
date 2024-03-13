package com.yaeltex.seqarp168mk2.device;

import com.bitwig.extension.controller.api.ControllerHost;
import com.bitwig.extension.controller.api.CursorDeviceFollowMode;
import com.bitwig.extension.controller.api.CursorTrack;
import com.bitwig.extension.controller.api.Device;
import com.bitwig.extension.controller.api.DeviceBank;
import com.bitwig.extension.controller.api.PinnableCursorDevice;
import com.bitwig.extensions.framework.values.BooleanValueObject;
import com.yaeltex.seqarp168mk2.BitwigViewControl;
import com.yaeltex.seqarp168mk2.SeqArp168Extension;

public class FocusDevice {
    
    private final int index;
    private final CursorTrack cursorTrack;
    private final PinnableCursorDevice cursorDevice;
    private final BitwigArpDevice arpDevice;
    private final BooleanValueObject isOnCursorDevice = new BooleanValueObject();
    private DeviceSlotState slotState = DeviceSlotState.EMPTY;
    private final Device focusDevice;
    private final BitwigViewControl viewControl;
    private ArpInstance arpInstance;
    private final ControllerHost host;
    
    
    public FocusDevice(final int index, final ControllerHost host, final BitwigViewControl viewControl) {
        this.index = index;
        this.host = host;
        cursorTrack = host.createCursorTrack("DEVICE_%d_TRACK".formatted(index), //
            "Device %d Track".formatted(index), //
            0, 1, false);
        cursorTrack.name().markInterested();
        
        this.viewControl = viewControl;
        cursorDevice =
            cursorTrack.createCursorDevice("SLOT" + index, "SLOT" + index, 1, CursorDeviceFollowMode.FOLLOW_SELECTION);
        
        final DeviceBank deviceBank = cursorTrack.createDeviceBank(1);
        deviceBank.setDeviceMatcher(viewControl.getArpDeviceMatcher());
        focusDevice = deviceBank.getItemAt(0);
        focusDevice.exists().addValueObserver(this::handleArpDeviceExistChanged);
        focusDevice.presetName().addValueObserver(name -> {
            host.scheduleTask(() -> determinePresetNameChanged(name), 10);
        });
        arpDevice = new BitwigArpDevice(index, focusDevice);
        
        cursorDevice.name().addValueObserver(s -> {
            //            if (!s.equals("Arpeggiator")) {
            //                slotState = DeviceSlotState.EMPTY;
            //                arpInstance = null;
            //            }
        });
        cursorDevice.isPinned().addValueObserver(this::handleIsPinned);
        
        bindCustom();
    }
    
    private void bindCustom() {
        for (int i = 0; i < 16; i++) {
            final int index = i;
            final NoteControlValue noteValue = arpDevice.getNoteValues(i);
            noteValue.addBaseValueListener(value -> updateBaseNote(index, value));
            noteValue.addOffsetValueListener(value -> updateOffsetValue(index, value));
        }
    }
    
    private void updateBaseNote(final int index, final int value) {
        if (arpInstance != null) {
            arpInstance.setBaseNote(index, value);
            arpDevice.getNoteParam(index).set(arpInstance.getNoteValueAct(index));
        }
    }
    
    private void updateOffsetValue(final int index, final int value) {
        if (arpInstance != null) {
            arpInstance.setOffsetNote(index, value);
            arpDevice.getNoteParam(index).set(arpInstance.getNoteValueAct(index));
        }
    }
    
    private void handleIsPinned(final boolean pinned) {
        if (focusDevice.exists().get() && slotState != DeviceSlotState.EMPTY) {
            slotState = pinned ? DeviceSlotState.LOCKED : DeviceSlotState.PRESENT;
        }
    }
    
    public void toggleGate(final int index) {
        if (arpInstance != null) {
            arpInstance.toggleGate(index, arpDevice.getGate(index));
        }
    }
    
    public void toggleVelocity(final int index) {
        if (arpInstance != null) {
            arpInstance.toggleVelocity(index, arpDevice.getVelocity(index));
        }
    }
    
    
    public void toggleSkip(final int index) {
        if (arpInstance != null) {
            arpDevice.toggleStepSkip(index);
        }
    }
    
    public void toggleNoteQuantize(final int noteIndex) {
        if (arpInstance != null) {
            arpInstance.toggleQuantizeNote(noteIndex);
        }
    }
    
    public boolean isQuantizeNoteSet(final int noteIndex) {
        return arpInstance != null && arpInstance.isQuantizeNoteSet(noteIndex);
    }
    
    public boolean isStepMuted(final int index) {
        if (arpInstance != null) {
            return arpInstance.isGateMuted(index);
        }
        return false;
    }
    
    public boolean isVelocityMuted(final int index) {
        if (arpInstance != null) {
            return arpInstance.isVelocityMuted(index);
        }
        return false;
    }
    
    private void determinePresetNameChanged(final String newName) {
        if (arpInstance != null && focusDevice.exists().get()) {
            final String presetName = arpInstance.getPresetName();
            final String cursorTrackName = cursorTrack.name().get();
            if (!presetName.equals(newName)) {
                if (arpInstance.getTrackName().equals(cursorTrackName)) {
                    arpInstance.setPresetName(newName);
                } else {
                    assignArpInstance();
                }
            }
        }
    }
    
    private void determineDeviceSwitch() {
        if (slotState == DeviceSlotState.EMPTY) {
            return;
        }
        final String trackName = cursorTrack.name().get();
        final String presetName = focusDevice.presetName().get();
        if (focusDevice.exists().get()) {
            if (arpInstance == null) {
                assignArpInstance();
            } else if (!arpInstance.matches(trackName, presetName)) {
                assignArpInstance();
            }
        }
    }
    
    private void assignArpInstance() {
        final String trackName = cursorTrack.name().get();
        final String presetName = focusDevice.presetName().get();
        SeqArp168Extension.println("ASSIGN ARP <%d> %s %s", index, presetName, trackName);
        arpInstance = viewControl.getArpInstance(trackName, presetName);
        if (cursorDevice.isPinned().get()) {
            slotState = DeviceSlotState.LOCKED;
        } else {
            slotState = DeviceSlotState.PRESENT;
        }
        arpInstance.applyValues(arpDevice);
    }
    
    public BitwigArpDevice getArpDevice() {
        return arpDevice;
    }
    
    private void handleArpDeviceExistChanged(final boolean exists) {
        SeqArp168Extension.println(" Focus %d  %s", index, exists);
        if (!exists) {
            if (slotState != DeviceSlotState.EMPTY) {
                slotState = DeviceSlotState.EMPTY;
                arpInstance = null;
            }
        }
    }
    
    public PinnableCursorDevice getCursorDevice() {
        return cursorDevice;
    }
    
    public Device getFocusDevice() {
        return focusDevice;
    }
    
    public BooleanValueObject getIsOnCursorDevice() {
        return isOnCursorDevice;
    }
    
    public void link(final CursorTrack track, final Device focusArpDevice) {
        cursorTrack.selectChannel(track);
        cursorDevice.selectDevice(focusArpDevice);
        if (slotState == DeviceSlotState.EMPTY) {
            slotState = cursorDevice.isPinned().get() ? DeviceSlotState.LOCKED : DeviceSlotState.PRESENT;
            host.scheduleTask(this::determineDeviceSwitch, 10);
        }
    }
    
    public DeviceSlotState getSlotState() {
        return slotState;
    }
    
    public void toggleLock() {
        if (slotState == DeviceSlotState.PRESENT) {
            slotState = DeviceSlotState.LOCKED;
            cursorDevice.isPinned().set(true);
            cursorTrack.isPinned().set(true);
        } else if (slotState == DeviceSlotState.LOCKED) {
            slotState = DeviceSlotState.PRESENT;
            cursorDevice.isPinned().set(false);
            cursorTrack.isPinned().set(false);
        }
    }
    
    
    public boolean isPresent() {
        return slotState != DeviceSlotState.EMPTY && focusDevice.exists().get();
    }
    
    
}
