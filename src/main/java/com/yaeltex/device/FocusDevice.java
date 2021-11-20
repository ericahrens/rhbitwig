package com.yaeltex.device;

import java.util.ArrayList;
import java.util.UUID;

import com.bitwig.extension.controller.api.CursorTrack;
import com.bitwig.extension.controller.api.IntegerValue;
import com.bitwig.extension.controller.api.Parameter;
import com.bitwig.extension.controller.api.PinnableCursorDevice;
import com.bitwig.extension.controller.api.SpecificBitwigDevice;
import com.bitwig.extensions.debug.RemoteConsole;
import com.yaeltex.YaeltexArpControlExtension;
import com.yaeltex.encoders.EncoderLayout;
import com.yaeltex.layer.MuteState;
import com.yaeltex.value.BooleanValueObject;

public class FocusDevice {

	private static final int NUMBER_OF_STEPS = 16;

	private final CursorTrack cursorTrack;

	private final PinnableCursorDevice cursorDevice;
	private final SpecificBitwigDevice arpdevice;
	private final int which;
	private final BooleanValueObject arpSelected = new BooleanValueObject();

	private final ArrayList<Parameter> arpNoteParams;
	private final ArrayList<Parameter> arpGateParams;
	private final ArrayList<Parameter> arpVelParams;
	private final ArrayList<Parameter> arpSkipParams;

	private final int[] noteValues = new int[NUMBER_OF_STEPS];
	private final int[] velocityValues = new int[NUMBER_OF_STEPS];
	private final int[] gateValues = new int[NUMBER_OF_STEPS];
	private final int[] stepSkipValues = new int[NUMBER_OF_STEPS];

	private int stepLength;

	private int stepPosition = -1;
	private int prevPosition = -1;

	private boolean isOnCursorDevice;
	private final Parameter arpStepsParam;
	private final IntegerValue arpStepPositionParam;

	private EncoderLayout currentEncoderLayout;

	private ArpInstance assignedArpInstance = null;

	private boolean initalized = false;

	private final YaeltexArpControlExtension driver;

	private final Parameter globalVelParam;

	private final Parameter globalGateParam;

	private final Parameter retriggerParam;

	private final Parameter rateParam;

	private final Parameter octaveParam;

	private final Parameter modeParam;

	private final Parameter shuffleParam;

	private final Parameter rateModeParam;

	public FocusDevice(final int which, final boolean isFocus, final YaeltexArpControlExtension driver) {
		super();
		this.which = which;
		this.driver = driver;
		arpSelected.setValue(false);
		cursorTrack = driver.getHost().createCursorTrack(
				"DEVICE" + (isFocus ? "_CURSOR" : Integer.toString(which)) + "_TRACK", //
				isFocus ? "Focus Track" : "Device" + which + "Track", //
				0, 1, isFocus);
		cursorTrack.name().markInterested();
		cursorDevice = cursorTrack.createCursorDevice();
		arpdevice = cursorDevice.createSpecificBitwigDevice(UUID.fromString("4d407a2b-c91b-4e4c-9a89-c53c19fe6251"));
		currentEncoderLayout = driver.getCurrentEncoderLayout().get();
		driver.getCurrentEncoderLayout().addValueObserver(layout -> {
			this.currentEncoderLayout = layout;
		});
		cursorTrack.name().addValueObserver(newTrackName -> {
			if (assignedArpInstance != null) {
				assignedArpInstance.setTrackName(newTrackName);
			}
		});

		arpNoteParams = new ArrayList<>();
		arpGateParams = new ArrayList<>();
		arpVelParams = new ArrayList<>();
		arpSkipParams = new ArrayList<>();
		for (int i = 0; i < 16; i++) {
			final int index = i;
			final Parameter gate = arpdevice.createParameter("GATE_" + (i + 1));
			final Parameter velocity = arpdevice.createParameter("STEP_" + (i + 1));
			final Parameter note = arpdevice.createParameter("STEP_" + (i + 1) + "_TRANSPOSE");
			final Parameter skip = arpdevice.createParameter("SKIP_" + (i + 1));
			gate.markInterested();
			velocity.markInterested();
			note.markInterested();
			skip.markInterested();
			note.value().markInterested();
			arpNoteParams.add(note);
			arpGateParams.add(gate);
			arpVelParams.add(velocity);
			arpSkipParams.add(skip);

			note.value().addValueObserver(49, v -> handleNoteUpdate(index, v));
			gate.value().addValueObserver(128, v -> handleGateUpdate(index, v));
			velocity.value().addValueObserver(128, v -> handleVelocityUpdate(index, v));
			skip.value().addValueObserver(2, v -> {
				stepSkipValues[index] = v;
				currentEncoderLayout.handleSkipStepUpdate(this, index, v);
			});
		}

		cursorDevice.name().addValueObserver(s -> {
			if (s.equals("Arpeggiator")) {
				arpSelected.setValue(true);
				driver.notifyPinned(this, cursorDevice.isPinned().get());
				if (cursorDevice.presetName().get().length() > 0) {
					assignedArpInstance = driver.getArpInstance(cursorTrack.name().get(),
							cursorDevice.presetName().get());
					currentEncoderLayout.refresh();
				}
			} else /* if (currentArp != null) */ {
				arpSelected.setValue(false);
				driver.notifyPinned(this, cursorDevice.isPinned().get());
				currentEncoderLayout.refresh();
			}
		});
		cursorDevice.presetName().addValueObserver(pn -> {
			if (isArp()) {
				if (assignedArpInstance == null) {
					assignedArpInstance = driver.getArpInstance(cursorTrack.name().get(),
							cursorDevice.presetName().get());
					currentEncoderLayout.refresh();
				} else {
					assignedArpInstance.setPresetName(pn);
				}
			}
		});

		cursorDevice.isPinned().addValueObserver(v -> {
			driver.notifyPinned(this, v);
		});

		rateParam = arpdevice.createParameter("RATE");
		rateParam.markInterested();
		octaveParam = arpdevice.createParameter("OCTAVES");
		octaveParam.markInterested();
		modeParam = arpdevice.createParameter("MODE");
		modeParam.markInterested();
		shuffleParam = arpdevice.createParameter("SHUFFLE");
		shuffleParam.markInterested();

		rateModeParam = arpdevice.createParameter("RATE_MODE");
		rateModeParam.markInterested();
		retriggerParam = arpdevice.createParameter("RETRIGGER");
		retriggerParam.markInterested();

		globalVelParam = arpdevice.createParameter("GLOBAL_VEL");
		globalVelParam.markInterested();

		globalGateParam = arpdevice.createParameter("GLOBAL_GATE");
		globalGateParam.markInterested();

		arpStepsParam = arpdevice.createParameter("STEPS");
		arpStepPositionParam = arpdevice.createIntegerOutputValue("STEP");
		arpStepPositionParam.markInterested();
		arpStepsParam.markInterested();

		arpStepPositionParam.addValueObserver(v -> {
			prevPosition = stepPosition;
			stepPosition = v;
			currentEncoderLayout.handleStepPosition(this, v, prevPosition);
		});

		arpStepsParam.value().addValueObserver(16, v -> {
			stepLength = v;
			currentEncoderLayout.handleStepLength(this, v, stepLength);
		});
	}

	public ArpInstance getAssignedArpInstance() {
		return assignedArpInstance;
	}

	public Parameter getRateModeParam() {
		return rateModeParam;
	}

	public Parameter getRateParam() {
		return rateParam;
	}

	public Parameter getModeParam() {
		return modeParam;
	}

	public Parameter getOctaveParam() {
		return octaveParam;
	}

	public Parameter getShuffleParam() {
		return shuffleParam;
	}

	public void handleNoteUpdate(final int index, final int value) {
		noteValues[index] = value;
		currentEncoderLayout.handleNoteUpdate(this, index, value);
	}

	public void handleGateUpdate(final int index, final int v) {
		gateValues[index] = v;
		currentEncoderLayout.handleGateUpdate(this, index, v);
	}

	public void handleVelocityUpdate(final int index, final int v) {
		velocityValues[index] = v;
		currentEncoderLayout.handleVelocityUpdate(this, index, v);
	}

	public void notifyTrackIndex(final int newTrackIndex) {
//		if (arpCreationPending) {
//			assignedArpInstance = driver.getArpInstance(driver.getCurrentTrackIndex(), cursorDevice.presetName().get());
//			currentEncoderLayout.refresh();
//			arpCreationPending = false;
//		} else if (isArp() && !isPinned()) {
//			if (assignedArpInstance.getTrackIndex() != newTrackIndex) {
//				assignedArpInstance = driver.getArpInstance(newTrackIndex, cursorDevice.presetName().get());
//				currentEncoderLayout.refresh();
//			}
//		}
	}

	public void setGlobalGate(final double value) {
		globalGateParam.value().setImmediately(value);
	}

	public double getGlobalGateLength() {
		return globalGateParam.value().get();
	}

	public void setGlobalVelocity(final double value) {
		globalVelParam.value().setImmediately(value);
	}

	public double getGlobalVelocityLength() {
		return globalVelParam.value().get();
	}

	public Parameter getRetriggerParam() {
		return retriggerParam;
	}

	public void setStepLength(final int stepLength) {
		this.stepLength = stepLength;
		arpStepsParam.set(stepLength, 16);
	}

	public void toggleStepSkipValue(final int index) {
		final Parameter param = arpSkipParams.get(index);
		if (param.get() == 0) {
			param.set(1);
		} else {
			param.set(0);
		}
	}

	public void toggleGateMute(final int index) {
		if (assignedArpInstance != null) {
			assignedArpInstance.toggleGate(index, gateValues[index], arpGateParams.get(index));
		}
	}

	public void toggleVelMute(final int index) {
		// RemoteConsole.out.println(" TGL VEL MUTE i={} inst={}", index,
		// assignedArpInstance != null);
		if (assignedArpInstance != null) {
			assignedArpInstance.toggleVelocity(index, velocityValues[index], arpVelParams.get(index));
		}
	}

	public MuteState getGateMuteState(final int index) {
		if (assignedArpInstance != null) {
			return assignedArpInstance.getGateMute(index) == -1 ? MuteState.ACTIVE : MuteState.MUTED;
		}
		return MuteState.UNDEFINED;
	}

	public MuteState getVelMuteState(final int index) {
		if (assignedArpInstance != null) {
			return assignedArpInstance.getVelMute(index) == -1 ? MuteState.ACTIVE : MuteState.MUTED;
		}
		return MuteState.UNDEFINED;
	}

	public void changeNoteValue(final int index, final int offset) {
		noteValues[index] = Math.min(48, Math.max(0, noteValues[index] + offset));
		if (assignedArpInstance != null) {
			assignedArpInstance.changeBaseNote(index, offset);
			arpNoteParams.get(index).set(assignedArpInstance.getNoteValueAct(index));
		} else {
			arpNoteParams.get(index).value().set(noteValues[index], 49);
		}
	}

	public void changeNoteOffsetValue(final int index, final int offset) {
		if (assignedArpInstance == null) {
			return;
		}
		assignedArpInstance.changeNoteOffset(index, offset);
		arpNoteParams.get(index).set(assignedArpInstance.getNoteValueAct(index));
		currentEncoderLayout.handleNoteOffsetUpdate(this, index, assignedArpInstance.getNoteOffset(index));
	}

	public void resetNoteOffset(final int index) {
		if (assignedArpInstance == null) {
			return;
		}
		assignedArpInstance.resetNoteOffset(index);
		arpNoteParams.get(index).set(assignedArpInstance.getNoteValueAct(index));
		currentEncoderLayout.handleNoteOffsetUpdate(this, index, assignedArpInstance.getNoteOffset(index));
	}

	public void changeVelocityValue(final int index, final int offset) {
		velocityValues[index] = Math.min(127, Math.max(0, velocityValues[index] + offset));
		if (assignedArpInstance != null) {
			assignedArpInstance.updateVelocityValue(index, velocityValues[index]);
			if (!assignedArpInstance.isVelocityMuted(index)) {
				arpVelParams.get(index).value().set(velocityValues[index], 128);
			}
		} else {
			arpVelParams.get(index).value().set(velocityValues[index], 128);
		}
	}

	public void changeGateValue(final int index, final int offset) {
		gateValues[index] = Math.min(127, Math.max(0, gateValues[index] + offset));
		if (assignedArpInstance != null) {
			if (!assignedArpInstance.isGateMuted(index)) {
				arpGateParams.get(index).value().set(gateValues[index], 128);
			}
			assignedArpInstance.updateGateValue(index, gateValues[index]);
		} else {
			arpGateParams.get(index).value().set(gateValues[index], 128);
		}
	}

	public void updateGlobals(final int index) {
		driver.getStepLengthValues().set(index, stepLength);
	}

	public int getStepLength() {
		return stepLength;
	}

	public int getStepPostion() {
		return stepPosition;
	}

	public Parameter getNoteParam(final int index) {
		return arpNoteParams.get(index);
	}

	public Parameter getVelocityParam(final int index) {
		return arpVelParams.get(index);
	}

	public Parameter getGateParam(final int index) {
		return arpGateParams.get(index);
	}

	public Parameter getSkipParam(final int index) {
		return arpSkipParams.get(index);
	}

	public boolean isInitalized() {
		return initalized;
	}

	public void setInitialized() {
		initalized = true;
	}

	public boolean isPinned() {
		return cursorDevice.isPinned().get();
	}

	public boolean isArp() {
		return arpSelected.get();
	}

	public BooleanValueObject getArpSelected() {
		return arpSelected;
	}

	public int getWhich() {
		return which;
	}

	public void pin(final boolean pinned) {
		cursorTrack.isPinned().set(pinned);
		cursorDevice.isPinned().set(pinned);
	}

	public void set(final FocusDevice other, final boolean pinned) {
		RemoteConsole.out.println("PIN {}", pinned);
		cursorTrack.selectChannel(other.getCursorTrack());
		cursorDevice.selectDevice(other.getCursorDevice());
		cursorTrack.isPinned().set(pinned);
		cursorDevice.isPinned().set(pinned);
	}

	public CursorTrack getCursorTrack() {
		return cursorTrack;
	}

	public PinnableCursorDevice getCursorDevice() {
		return cursorDevice;
	}

	public void setIsOnCursorDevice(final boolean v) {
		isOnCursorDevice = v;
	}

	public boolean isOnCursorDevice() {
		return isOnCursorDevice;
	}

	public int getNoteOffsetVal(final int index) {
		if (assignedArpInstance == null) {
			return 0;
		}
		return assignedArpInstance.getNoteOffset(index);
	}

	public int getNoteValue(final int index) {
		return noteValues[index];
	}

	public int getGateValue(final int index) {
		return gateValues[index];
	}

	public int getVelocityValue(final int index) {
		return velocityValues[index];
	}

	public int getStepSkipValue(final int index) {
		return stepSkipValues[index];
	}

	public void toggleNoteQuantize(final int noteIndex) {
		if (assignedArpInstance != null) {
			assignedArpInstance.toggleQuantizeNote(noteIndex);
		}
	}

	public boolean isNoteQuantizeSet(final int noteIndex) {
		if (assignedArpInstance != null) {
			return assignedArpInstance.isQuantizeNoteSet(noteIndex);
		}
		return false;
	}

}
