package com.akai.fire.sequence;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import com.akai.fire.AkaiFireDrumSeqExtension;
import com.akai.fire.NoteAssign;
import com.akai.fire.ViewCursorControl;
import com.akai.fire.control.BiColorButton;
import com.akai.fire.control.RgbButton;
import com.akai.fire.control.TouchEncoder;
import com.akai.fire.display.DisplayInfo;
import com.akai.fire.display.OledDisplay;
import com.akai.fire.display.OledDisplay.TextJustification;
import com.akai.fire.lights.BiColorLightState;
import com.akai.fire.lights.RgbLigthState;
import com.bitwig.extension.controller.api.CursorTrack;
import com.bitwig.extension.controller.api.DrumPadBank;
import com.bitwig.extension.controller.api.MultiStateHardwareLight;
import com.bitwig.extension.controller.api.NoteStep;
import com.bitwig.extension.controller.api.NoteStep.State;
import com.bitwig.extension.controller.api.PinnableCursorClip;
import com.bitwig.extensions.framework.Layer;
import com.bitwig.extensions.rh.BooleanValueObject;
import com.bitwig.extensions.rh.StepViewPosition;

public class DrumSequenceMode extends Layer {

	private static final Map<NoteAssign, FunctionInfo> functionDetailInfos = new HashMap<>();
	private static final Map<NoteAssign, FunctionInfo> functionDetailInfos2 = new HashMap<>();

	private static class FunctionInfo {
		private final String name;
		private final String detail;
		private final String shiftFunction;

		public FunctionInfo(final String name, final String detail) {
			this(name, detail, null);
		}

		public FunctionInfo(final String name, final String detail, final String shiftFunction) {
			super();
			this.name = name;
			this.detail = detail;
			this.shiftFunction = shiftFunction;
		}

		public String getName(final boolean shift) {
			if (!shift || shiftFunction == null) {
				return name;
			}
			return shiftFunction;
		}

		public String getDetail() {
			return detail;
		}

	}

	static {
		functionDetailInfos.put(NoteAssign.MUTE_1, new FunctionInfo("Select", "Pad: select Pad\nClip: select clip"));
		functionDetailInfos.put(NoteAssign.MUTE_2, new FunctionInfo("Last Step", "Step: set last step"));
		functionDetailInfos.put(NoteAssign.MUTE_3,
				new FunctionInfo("Copy", "Pad: from selected\nClip: from selected", "Copy -opvals"));
		functionDetailInfos.put(NoteAssign.MUTE_4,
				new FunctionInfo("Delete", "Pad: clear notes\nClip: clear notes\nEncoder: reset value"));
		functionDetailInfos2.put(NoteAssign.MUTE_1, new FunctionInfo("Mute", "Pad: Mute\nNotes: mute notes"));
		functionDetailInfos2.put(NoteAssign.MUTE_2, new FunctionInfo("Solo", "Pad: Solo"));
	}

	private final IntSetValue heldSteps = new IntSetValue();
	private final Set<Integer> addedSteps = new HashSet<>();
	private final Set<Integer> modifiedSteps = new HashSet<>();
	private final HashMap<Integer, NoteStep> expectedNoteChanges = new HashMap<>();

	private final NoteStep[] assignments = new NoteStep[32];

	private final Layer mainLayer;
	private final Layer shiftLayer;
	private Layer currentLayer;

	private final CursorTrack cursorTrack;
	private final PinnableCursorClip cursorClip;

	private final BooleanValueObject muteMode = new BooleanValueObject();
	private final BooleanValueObject soloMode = new BooleanValueObject();
	private final BooleanValueObject selectHeld = new BooleanValueObject();
	private final BooleanValueObject copyHeld = new BooleanValueObject();
	private final BooleanValueObject deleteHeld = new BooleanValueObject();
	private final BooleanValueObject fixedLengthHeld = new BooleanValueObject();
	private final BooleanValueObject shiftActive = new BooleanValueObject();
	private final BooleanValueObject clipLaunchModeQuant = new BooleanValueObject();

	private int playingStep;
	private final double gatePercent = 0.98;
	private final StepViewPosition positionHandler;
	private boolean markIgnoreOrigLen = false;
	private final AccentHandler accentHandler;
	private NoteAction pendingAction;

//	private final BooleanValueObject stopButtonHeld = new BooleanValueObject();

	private final OledDisplay oled;
	private final SequencEncoderHandler encoderLayer;
	private final ResolutionHander resolutionHandler;
	private final SeqClipHandler clipHandler;
	private final DrumPadBank padBank;
	private final RecurrenceEditor recurrenceEditor;
	private final Layer muteLayer;
	private final Layer soloLayer;
	private final PadHandler padHandler;

	public DrumSequenceMode(final AkaiFireDrumSeqExtension driver) {
		super(driver.getLayers(), "DRUM_SEQUENCE_LAYER");
		this.oled = driver.getOled();

		mainLayer = new Layer(this.getLayers(), getName() + "_MAIN");
		shiftLayer = new Layer(this.getLayers(), getName() + "_SHIFT");
		muteLayer = new Layer(this.getLayers(), getName() + "_MUTE");
		soloLayer = new Layer(this.getLayers(), getName() + "_SOLO");

		currentLayer = mainLayer;
		this.accentHandler = new AccentHandler(this);
		this.resolutionHandler = new ResolutionHander(this);

		final ViewCursorControl control = driver.getViewControl();
		cursorTrack = driver.getViewControl().getCursorTrack();
		cursorTrack.name().markInterested();
		cursorClip = cursorTrack.createLauncherCursorClip("SQClip", "SQClip", 32, 1);

		this.padBank = control.getDrumPadBank();
		padBank.canScrollBackwards().markInterested();
		padBank.canScrollForwards().markInterested();
		padBank.scrollPosition().markInterested();

		cursorClip.addNoteStepObserver(this::handleNoteStep);
		cursorClip.playingStep().addValueObserver(this::handlePlayingStep);
		cursorClip.getLoopLength().addValueObserver(clipLength -> {
			if (markIgnoreOrigLen) {
				markIgnoreOrigLen = false;
			}
		});
		cursorClip.isPinned().markInterested();
		this.positionHandler = new StepViewPosition(cursorClip);

		padHandler = new PadHandler(driver, this, mainLayer, muteLayer, soloLayer);
		clipHandler = new SeqClipHandler(driver, this, mainLayer);
		recurrenceEditor = new RecurrenceEditor(driver, this);

		initSequenceSection(driver);
		initModeButtons(driver);
		initButtonBehaviour(driver);
		encoderLayer = new SequencEncoderHandler(this, driver);

		muteMode.addValueObserver(active -> {
			if (active) {
				muteLayer.activate();
			} else {
				muteLayer.deactivate();
			}
		});

		soloMode.addValueObserver(active -> {
			if (active) {
				soloLayer.activate();
			} else {
				soloLayer.deactivate();
			}
		});

		// notePlayingActive.set(true);
		final TouchEncoder mainEncoder = driver.getMainEncoder();
		mainEncoder.setStepSize(0.4);
		mainEncoder.bindEncoder(mainLayer, this::handleMainEncoder);
		mainEncoder.bindTouched(mainLayer, this::handeMainEncoderPress);
	}

	private void initModeButtons(final AkaiFireDrumSeqExtension driver) {
		final MultiStateHardwareLight[] stateLights = driver.getStateLights();
		bindEditButton(driver.getButton(NoteAssign.MUTE_1), "Select", selectHeld, stateLights[0], muteMode);
		bindEditButton(driver.getButton(NoteAssign.MUTE_2), "Last Step", fixedLengthHeld, stateLights[1], soloMode);
		bindEditButton(driver.getButton(NoteAssign.MUTE_3), "Copy", copyHeld, stateLights[2], null);
		bindEditButton(driver.getButton(NoteAssign.MUTE_4), "Delete/Reset", deleteHeld, stateLights[3], null);
		final BiColorButton deleteButton = driver.getButton(NoteAssign.MUTE_4);
		deleteButton.bind(mainLayer, deleteHeld, BiColorLightState.GREEN_FULL, BiColorLightState.OFF);
	}

	private void initButtonBehaviour(final AkaiFireDrumSeqExtension driver) {

		final BiColorButton accentButton = driver.getButton(NoteAssign.STEP_SEQ); // TODO combine with encoder
		accentButton.bindPressed(mainLayer, accentHandler::handlePressed, accentHandler::getLightState);

		final BiColorButton shiftButton = driver.getButton(NoteAssign.SHIFT);
		shiftButton.bind(mainLayer, shiftActive, BiColorLightState.GREEN_HALF, BiColorLightState.OFF);

		final BiColorButton clipLaunchModeButton = driver.getButton(NoteAssign.NOTE);
		clipLaunchModeButton.bindToggle(mainLayer, clipLaunchModeQuant, BiColorLightState.AMBER_FULL,
				BiColorLightState.AMBER_HALF, oled,
				new DisplayInfo().addLine("Clip Legato", 2, 0, TextJustification.CENTER)//
						.addLine(() -> clipLaunchModeQuant.get() ? "with quant" : "immediate", 2, 3,
								TextJustification.CENTER)//
						.create());

		final BiColorButton retrigButton = driver.getButton(NoteAssign.DRUM);
		retrigButton.bind(mainLayer, this::retrigger, BiColorLightState.AMBER_FULL, BiColorLightState.AMBER_HALF);

		final BiColorButton pinButton = driver.getButton(NoteAssign.ALT);
		pinButton.bindPressed(mainLayer, this::handleClipPinning, this::getPinnedState);

		final BiColorButton resolutionButton = driver.getButton(NoteAssign.PERFORM);
		resolutionButton.bindPressed(mainLayer, resolutionHandler::handlePressed, resolutionHandler::getLightState);

		final BiColorButton upNavButon = driver.getButton(NoteAssign.PATTERN_UP);
		upNavButon.markPressedInteressed();
		upNavButon.bindPressed(mainLayer, this::scrollForward, () -> canScrollUp(upNavButon));

		final BiColorButton downNavButon = driver.getButton(NoteAssign.PATTERN_DOWN);
		downNavButon.markPressedInteressed();
		downNavButon.bindPressed(mainLayer, this::scrollBackward, () -> canScrollDown(downNavButon));

		final BiColorButton shiftLeftButton = driver.getButton(NoteAssign.BANK_L);
		shiftLeftButton.bindPressed(mainLayer, p -> movePattern(p, -1), BiColorLightState.HALF, BiColorLightState.OFF);
		final BiColorButton shiftRightButton = driver.getButton(NoteAssign.BANK_R);
		shiftRightButton.bindPressed(mainLayer, p -> movePattern(p, 1), BiColorLightState.HALF, BiColorLightState.OFF);
	}

	private void movePattern(final boolean pressed, final int dir) {
		if (pressed) {
			return;
		}
		final List<NoteStep> notes = getOnNotes();
		final int availableSteps = positionHandler.getAvailableSteps();
		cursorClip.clearStepsAtY(0, 0);

		for (final NoteStep noteStep : notes) {
			int pos = noteStep.x() + dir;
			if (pos < 0) {
				pos = availableSteps - 1;
			} else if (pos >= availableSteps) {
				pos = 0;
			}
			if (!shiftActive.get()) {
				expectedNoteChanges.put(pos, noteStep);
			}
			cursorClip.setStep(pos, 0, (int) Math.round(noteStep.velocity() * 127), noteStep.duration());
		}
	}

	private BiColorLightState canScrollUp(final BiColorButton button) {
		if (padBank.scrollPosition().get() + (isShiftHeld() ? 16 : 4) < 128) {
			if (button.isPressed()) {
				return BiColorLightState.FULL;
			}
			return BiColorLightState.HALF;
		}
		return BiColorLightState.OFF;
	}

	private BiColorLightState canScrollDown(final BiColorButton button) {
		if (padBank.scrollPosition().get() - (isShiftHeld() ? 16 : 4) >= 0) {
			if (button.isPressed()) {
				return BiColorLightState.FULL;
			}
			return BiColorLightState.HALF;
		}
		return BiColorLightState.OFF;
	}

	private void scrollForward(final boolean pressed) {
		if (!pressed) {
			return;
		}
		if (isShiftHeld()) {
			padBank.scrollBy(4);
		} else {
			padBank.scrollBy(16);
		}
	}

	private void scrollBackward(final boolean pressed) {
		if (!pressed) {
			return;
		}
		if (isShiftHeld()) {
			padBank.scrollBy(-4);
		} else {
			padBank.scrollBy(-16);
		}
	}

	private BiColorLightState getPinnedState() {
		return cursorClip.isPinned().get() ? BiColorLightState.HALF : BiColorLightState.OFF;
	}

	private void handleClipPinning(final boolean pressed) {
		if (pressed) {
			cursorClip.isPinned().toggle();
			oled.paramInfo((cursorClip.isPinned().get() ? "UNPIN" : "PIN") + " Clip", "TR:" + cursorTrack.name().get());
		} else {
			oled.clearScreenDelayed();
		}
	}

	private void handleMainEncoder(final int inc) {
		if (accentHandler.isHolding()) {
			accentHandler.handleMainEncoder(inc);
		} else if (resolutionHandler.isHolding()) {
			resolutionHandler.handleMainEncoder(inc);
		} else {
			padHandler.handleMainEncoder(inc);
		}
	}

	private void handeMainEncoderPress(final boolean press) {
		if (accentHandler.isHolding()) {
			accentHandler.handeMainEncoderPress(press);
		} else if (resolutionHandler.isHolding()) {
			resolutionHandler.handeMainEncoderPress(press);
		}
	}

	public BooleanValueObject getShiftActive() {
		return shiftActive;
	}

	public BooleanValueObject getDeleteHeld() {
		return deleteHeld;
	}

	public void notifyBlink(final int blinkTicks) {
		clipHandler.notifyBlink(blinkTicks);
	}

	public OledDisplay getOled() {
		return oled;
	}

	private void bindEditButton(final BiColorButton button, final String name, final BooleanValueObject value,
			final MultiStateHardwareLight stateLight, final BooleanValueObject altValue) {
		if (altValue == null) {
			final FunctionInfo info1 = functionDetailInfos.get(button.getNoteAssign());
			button.bind(mainLayer, value, BiColorLightState.GREEN_FULL, BiColorLightState.OFF);
			mainLayer.bindLightState(() -> BiColorLightState.AMBER_HALF, stateLight);
			value.addValueObserver(active -> handleEditValueChanged(button, active, info1));
			mainLayer.bindLightState(() -> {
				return button.isPressed() ? BiColorLightState.AMBER_FULL : BiColorLightState.AMBER_HALF;
			}, stateLight);
		} else {
			final BooleanValueObject alternateFunctionActive = new BooleanValueObject();
			final FunctionInfo info1 = functionDetailInfos.get(button.getNoteAssign());
			value.addValueObserver(active -> handleEditValueChanged(button, active, info1));
			final FunctionInfo info2 = functionDetailInfos2.get(button.getNoteAssign());
			altValue.addValueObserver(active -> handleEditValueChanged(button, active, info2));
			button.bindPressed(mainLayer, pressed -> {
				if (getShiftActive().get()) {
					if (pressed) {
						alternateFunctionActive.toggle();
						final FunctionInfo info = alternateFunctionActive.get() ? info2 : info1;
						oled.functionInfo(getPadInfo(), info.getName(shiftActive.get()), info.getDetail());
					} else {
						oled.clearScreenDelayed();
					}
				} else {
					if (alternateFunctionActive.get()) {
						altValue.set(pressed);
					} else {
						value.set(pressed);
					}
				}
			}, () -> {
				return button.isPressed() ? BiColorLightState.GREEN_FULL : BiColorLightState.OFF;
			});
			mainLayer.bindLightState(() -> {
				final boolean active = button.isPressed() && !getShiftActive().get();
				if (alternateFunctionActive.get()) {
					return active ? BiColorLightState.RED_FULL : BiColorLightState.RED_HALF;
				}
				return active ? BiColorLightState.AMBER_FULL : BiColorLightState.AMBER_HALF;
			}, stateLight);
		}
	}

	public String getPadInfo() {
		return padHandler.getPadInfo();
	}

	private void handleEditValueChanged(final BiColorButton button, final boolean active, final FunctionInfo info) {
		if (active) {
			if (padHandler.notePlayingEnabled()) {
				padHandler.disableNotePlaying();
			}
			oled.functionInfo(getPadInfo(), info.getName(shiftActive.get()), info.getDetail());
		} else {
			oled.clearScreenDelayed();
			if (padHandler.notePlayingEnabled()) {
				padHandler.applyScale();
			}
		}
	}

	double getGridResolution() {
		return positionHandler.getGridResolution();
	}

	String getDetails(final List<NoteStep> heldNotes) {
		return getPadInfo() + " <" + heldNotes.size() + ">";
	}

	public void registerModifiedSteps(final List<NoteStep> notes) {
		notes.forEach(s -> modifiedSteps.add(s.x()));
	}

	List<NoteStep> getHeldNotes() {
		final List<NoteStep> heldNotes = heldSteps.stream()//
				.map(idx -> assignments[idx])//
				.filter(ns -> ns != null && ns.state() == State.NoteOn) //
				.collect(Collectors.toList());
		return heldNotes;
	}

	List<NoteStep> getOnNotes() {
		return Arrays.stream(assignments).filter(ns -> ns != null && ns.state() == State.NoteOn)
				.collect(Collectors.toList());
	}

	private void initSequenceSection(final AkaiFireDrumSeqExtension driver) {
		final RgbButton[] rgbButtons = driver.getRgbButtons();
		for (int i = 0; i < 32; i++) {
			final RgbButton button = rgbButtons[i + 32];
			final int index = i;
			button.bindPressed(mainLayer, p -> handleSeqSelection(index, p), () -> stepState(index));
		}
	}

	public void registerPendingAction(final NoteAction action) {
		pendingAction = action;
	}

	public NoteAction getPendingAction() {
		return pendingAction;
	}

	private void handleSeqSelection(final int index, final boolean pressed) {
		final NoteStep note = assignments[index];
		if (!pressed) {
			heldSteps.remove(index);
			if (note != null && note.state() == State.NoteOn && !addedSteps.contains(index)) {
				if (!modifiedSteps.contains(index)) {
					cursorClip.toggleStep(index, 0, accentHandler.getCurrenVel());
				} else {
					modifiedSteps.remove(index);
				}
			}
			addedSteps.remove(index);
		} else {
			heldSteps.add(index);
			if (fixedLengthHeld.get()) {
				stepActionFixedLength(index);
			} else {
				if (note == null || note.state() == State.Empty) {
					cursorClip.setStep(index, 0, accentHandler.getCurrenVel(),
							positionHandler.getGridResolution() * gatePercent);
					addedSteps.add(index);
				}
			}
		}
	}

	private void stepActionFixedLength(final int index) {
		final double newLen = positionHandler.lengthWithLastStep(index);
		adjustMode(newLen);
		cursorClip.getLoopLength().set(newLen);
	}

	private void adjustMode(final double clipLength) {
		final int notes = (int) (clipLength / 0.25);
		adjustMode(notes);
	}

	private void adjustMode(final int notes) {
		if (notes % 8 == 0) {
			cursorClip.launchMode().set("play_with_quantization");
		} else if (clipLaunchModeQuant.get()) {
			cursorClip.launchMode().set("continue_with_quantization");
		} else {
			cursorClip.launchMode().set("continue_immediately");
		}
	}

	private RgbLigthState stepState(final int index) {
		final int steps = positionHandler.getAvailableSteps();
		if (index < steps) {
			if (assignments[index] == null || assignments[index].state() != State.NoteOn) {
				if (index == this.playingStep) {
					return RgbLigthState.WHITE;
				}
				if (index / 4 % 2 == 0) {
					return RgbLigthState.GRAY_1;
				} else {
					return RgbLigthState.GRAY_2;
				}
			}

			if (index == this.playingStep) {
				return padHandler.getCurrentPadColor().getBrightend();
			}
			return padHandler.getCurrentPadColor();

		}
		return RgbLigthState.OFF;
	}

	private void handleNoteStep(final NoteStep noteStep) {
		final int newStep = noteStep.x();
		assignments[newStep] = noteStep;
		if (expectedNoteChanges.containsKey(newStep)) {
			final NoteStep previousStep = expectedNoteChanges.get(newStep);
			expectedNoteChanges.remove(newStep);
			applyValues(noteStep, previousStep);
		}
	}

	private void applyValues(final NoteStep dest, final NoteStep src) {
		dest.setChance(src.chance());
		dest.setTimbre(src.timbre());
		dest.setPressure(src.pressure());
		dest.setRepeatCount(src.repeatCount());
		dest.setRepeatVelocityCurve(src.repeatVelocityCurve());
		dest.setPan(src.pan());
		dest.setRepeatVelocityEnd(src.repeatVelocityEnd());
		dest.setRecurrence(src.recurrenceLength(), src.recurrenceMask());
		dest.setOccurrence(src.occurrence());
	}

	private void handlePlayingStep(final int playingStep) {
		if (playingStep == -1) {
			this.playingStep = -1;
		}
		this.playingStep = playingStep - positionHandler.getStepOffset();
	}

	@Override
	protected void onActivate() {
		currentLayer = mainLayer;
		mainLayer.activate();
		encoderLayer.activate();
		padHandler.applyScale();
	}

	@Override
	protected void onDeactivate() {
		currentLayer.deactivate();
		shiftLayer.deactivate();
		encoderLayer.deactivate();
		padHandler.disableNotePlaying();
	}

	public void retrigger() {
		cursorClip.launch();
	}

	public StepViewPosition getPositionHandler() {
		return positionHandler;
	}

	PinnableCursorClip getCursorClip() {
		return cursorClip;
	}

	boolean isShiftHeld() {
		return shiftActive.get();
	}

	boolean isCopyHeld() {
		return copyHeld.get();
	}

	boolean isDeleteHeld() {
		return deleteHeld.get();
	}

	boolean isSelectHeld() {
		return selectHeld.get();
	}

	public void exitRecurrenceEdit() {
		recurrenceEditor.exitRecurrenceEdit();
	}

	public void enterRecurrenceEdit(final List<NoteStep> notes) {
		recurrenceEditor.enterRecurrenceEdit(notes);
	}

	public void updateRecurrencLength(final int length) {
		recurrenceEditor.updateLength(length);
	}

	public IntSetValue getHeldSteps() {
		return heldSteps;
	}

	public boolean isPadBeingHeld() {
		return padHandler.isPadBeingHeld();
	}

	public void registerExpectedNoteChange(final int x, final NoteStep noteStep) {
		expectedNoteChanges.put(noteStep.x(), noteStep);
	}

}
