package com.akai.fire.sequence;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.akai.fire.AkaiFireDrumSeqExtension;
import com.akai.fire.NoteAssign;
import com.akai.fire.ViewCursorControl;
import com.akai.fire.control.BiColorButton;
import com.akai.fire.control.RgbButton;
import com.akai.fire.display.DisplayInfo;
import com.akai.fire.display.DisplayTarget;
import com.akai.fire.display.OledDisplay.TextJustification;
import com.akai.fire.lights.BiColorLightState;
import com.akai.fire.lights.RgbLigthState;
import com.akai.fire.sequence.NoteAction.Type;
import com.bitwig.extension.controller.api.CursorTrack;
import com.bitwig.extension.controller.api.DrumPadBank;
import com.bitwig.extension.controller.api.NoteInput;
import com.bitwig.extension.controller.api.NoteStep;
import com.bitwig.extension.controller.api.PinnableCursorClip;
import com.bitwig.extension.controller.api.PlayingNote;
import com.bitwig.extensions.framework.Layer;
import com.bitwig.extensions.rh.BooleanValueObject;

public class PadHandler {

	final DrumSequenceMode parent;
	private final PinnableCursorClip cursorClip;
	private final NoteInput noteInput;
	private final DrumPadBank padBank;

	private final NoteRepeatHandler noteRepeatHandler;

	private final List<PadContainer> pads = new ArrayList<>();
	private final Set<Integer> padsHeld = new HashSet<>();

	RgbLigthState currentPadColor = RgbLigthState.PURPLE;
	PadContainer selectedPad;
	int selectedPadIndex = -1;
	private int drumScrollOffset;

	private final BooleanValueObject[] playing = new BooleanValueObject[16];
	private final BooleanValueObject notePlayingActive = new BooleanValueObject();

	private final boolean drumTracker[] = new boolean[16];
	private final Integer[] notesToDrumTable = new Integer[128];
	private final int[] notesToPadsTable = new int[128];
	private final int[] padNotes = new int[16];
	private final DisplayTarget displayTarget;
	private final DisplayInfo padDisplayInfo;

	public PadHandler(final AkaiFireDrumSeqExtension driver, final DrumSequenceMode parent, final Layer mainLayer,
			final Layer muteLayer, final Layer soloLayer) {
		this.parent = parent;
		this.cursorClip = parent.getCursorClip();
		this.noteInput = driver.getNoteInput();
		for (int i = 0; i < padNotes.length; i++) {
			padNotes[i] = 0x36 + i;
		}
		final ViewCursorControl control = driver.getViewControl();

		this.padBank = control.getDrumPadBank();
		padBank.canScrollBackwards().markInterested();
		padBank.canScrollForwards().markInterested();
		padBank.scrollPosition().markInterested();

		setupPlaying(driver.getViewControl());

		displayTarget = new DisplayTarget(parent.getOled());

		final RgbButton[] rgbButtons = driver.getRgbButtons();
		for (int i = 0; i < 16; i++) {
			final int index = i;
			final RgbButton button = rgbButtons[i];
			final PadContainer pad = new PadContainer(this, index, control.getDrumPadBank().getItemAt(index),
					playing[index]);

			bindMain(button, mainLayer, pad);
			button.bindToggle(muteLayer, pad.pad.mute());
			button.bindLight(muteLayer, pad::mutingColors);
			button.bindToggle(soloLayer, pad.pad.solo());
			button.bindLight(soloLayer, pad::soloingColors);
		}

		noteRepeatHandler = new NoteRepeatHandler(driver, parent);
		noteRepeatHandler.getNoteRepeatActive().addValueObserver(this::handleNoteRepeatChanged);

		notePlayingActive.addValueObserver(active -> {
			if (active) {
				applyScale();
			} else if (!notePlayingEnabled()) {
				disableNotePlaying();
			}
		});
		initButtons(mainLayer, driver);
		padDisplayInfo = new DisplayInfo() //
				.addLine("Selected Pad", 1, 0, TextJustification.CENTER) //
				.addLine(() -> selectedPad != null ? selectedPad.getName() : "", 2, 3, TextJustification.CENTER) //
				.create();
	}

	private void bindMain(final RgbButton button, final Layer mainLayer, final PadContainer pad) {
		pads.add(pad);
		button.bindPressed(mainLayer, p -> handlePadSelection(pad, p), pad::getColor);
	}

	private void initButtons(final Layer mainLayer, final AkaiFireDrumSeqExtension driver) {
		final BiColorButton browerNrButton = driver.getButton(NoteAssign.BROWSER);
		browerNrButton.bindPressed(mainLayer, noteRepeatHandler::handlePressed, noteRepeatHandler::getLightState);
		final BiColorButton upNavButon = driver.getButton(NoteAssign.PATTERN_UP);
		upNavButon.markPressedInteressed();
		upNavButon.bindPressed(mainLayer, this::scrollForward, () -> canScrollUp(upNavButon));

		final BiColorButton downNavButon = driver.getButton(NoteAssign.PATTERN_DOWN);
		downNavButon.markPressedInteressed();
		downNavButon.bindPressed(mainLayer, this::scrollBackward, () -> canScrollDown(downNavButon));
	}

	private void handlePadSelection(final PadContainer pad, final boolean pressed) {
		if (!pressed) {
			padsHeld.remove(pad.index);
		} else {
			if (parent.isCopyHeld()) {
				doNotesPadCopy(pad);
			} else if (parent.isDeleteHeld()) {
				if (pad.index == selectedPadIndex) {
					cursorClip.clearStepsAtY(0, 0);
				} else {
					parent.registerPendingAction(new NoteAction(selectedPadIndex, pad.index, Type.CLEAR));
					pad.pad.selectInEditor();
				}
			} else {
				pad.pad.selectInEditor();
				padsHeld.add(pad.index);
			}
		}
	}

	void executeCopy(final List<NoteStep> notes, final boolean copyParams) {
		cursorClip.clearStepsAtY(0, 0);
		for (final NoteStep noteStep : notes) {
			cursorClip.setStep(noteStep.x(), 0, (int) (noteStep.velocity() * 127), noteStep.duration());
			if (copyParams) {
				parent.registerExpectedNoteChange(noteStep.x(), noteStep);
			}
		}
	}

	void executeClear(final int origIndex) {
		cursorClip.clearStepsAtY(0, 0);
		if (origIndex != -1) {
			pads.get(origIndex).pad.selectInEditor();
		}
	}

	/**
	 * The Pad has to be another pad then the currently selected pad. Copies notes
	 * to that destination.
	 *
	 * @param pad destination pad of copy.
	 */
	private void doNotesPadCopy(final PadContainer pad) {
		if (pad.index != selectedPadIndex) {
			final List<NoteStep> notes = parent.getOnNotes();
			parent.registerPendingAction(new NoteAction(selectedPadIndex, pad.index, Type.COPY_PAD, notes));
			cursorClip.scrollToKey(drumScrollOffset + pad.index);
			pad.pad.selectInEditor();
		}
	}

	public void executPadSelection(final PadContainer pad) {
		currentPadColor = pad.getPadColor();
		selectedPad = pad;
		focusOnSelectedPad();
		selectedPadIndex = pad.getIndex();

		displayTarget.setFocusIndex(selectedPadIndex);
		displayTarget.setName(selectedPad.getName());

		parent.getOled().showInfo(padDisplayInfo);

		selectedPad.updateDisplay(displayTarget.getTypeIndex());
		NoteAction pendingAction = parent.getPendingAction();
		if (pendingAction != null && pendingAction.getDestPadIndex() == selectedPadIndex) {
			if (pendingAction.getType() == Type.CLEAR) {
				executeClear(pendingAction.getSrcPadIndex());
			} else if (pendingAction.getType() == Type.COPY_PAD) {
				executeCopy(pendingAction.getCopyNotes(), !parent.isShiftHeld());
			}
			pendingAction = null;
		}
	}

	public void focusOnSelectedPad() {
		final int padIndex = selectedPad != null ? selectedPad.index : 0;
		cursorClip.scrollToKey(drumScrollOffset + padIndex);
	}

	public String getPadInfo() {
		if (selectedPad != null) {
			return selectedPad.getName();
		}
		return "";
	}

	public boolean isPadBeingHeld() {
		return !padsHeld.isEmpty();
	}

	public RgbLigthState getCurrentPadColor() {
		return currentPadColor;
	}

	private void setupPlaying(final ViewCursorControl control) {
		final DrumPadBank drumPadBank = control.getDrumPadBank();
		final CursorTrack cursorTrack = control.getCursorTrack();
		for (int i = 0; i < notesToDrumTable.length; i++) {
			notesToDrumTable[i] = Integer.valueOf(-1);
			notesToPadsTable[i] = -1;
		}
		noteInput.setKeyTranslationTable(notesToDrumTable);
		drumPadBank.scrollPosition().addValueObserver(offset -> {
			drumScrollOffset = offset;
			focusOnSelectedPad();
			applyScale();
		});
		for (int i = 0; i < 16; i++) {
			playing[i] = new BooleanValueObject();
			playing[i].set(false);
		}
		cursorTrack.playingNotes().addValueObserver(this::handleNotes);
	}

	private void handleNotes(final PlayingNote[] notes) {
		if (!parent.isActive()) {
			return;
		}
		for (int i = 0; i < 16; i++) {
			drumTracker[i] = false;
		}
		for (final PlayingNote playingNote : notes) {
			final int padIndex = notesToPadsTable[playingNote.pitch()];
			if (padIndex != -1) {
				playing[padIndex].set(true);
				drumTracker[padIndex] = true;
			}
		}
		for (int i = 0; i < 16; i++) {
			if (!drumTracker[i]) {
				playing[i].set(false);
			}
		}
	}

	boolean notePlayingEnabled() {
		return notePlayingActive.get() || noteRepeatHandler.getNoteRepeatActive().get();
	}

	private void handleNoteRepeatChanged(final boolean nrActive) {
		if (nrActive) {
			noteRepeatHandler.activate();
			applyScale();
		} else {
			noteRepeatHandler.deactivate();
			if (!notePlayingEnabled()) {
				disableNotePlaying();
			}
		}
	}

	void disableNotePlaying() {
		if (!parent.isActive()) {
			return;
		}
		padsHeld.clear();
		for (int i = 0; i < 128; i++) {
			notesToDrumTable[i] = -1;
		}
		noteInput.setKeyTranslationTable(notesToDrumTable);
	}

	void applyScale() {
		if (!parent.isActive()) {
			return;
		}
		for (int i = 0; i < 128; i++) {
			notesToPadsTable[i] = -1;
		}
		for (int i = 0; i < 16; i++) {
			final int padnote = padNotes[i];
			final int noteToPadIndex = drumScrollOffset + i;
			if (noteToPadIndex < 128) {
				notesToDrumTable[padnote] = noteToPadIndex;
				notesToPadsTable[noteToPadIndex] = i;
			}
		}
		if (notePlayingEnabled()) {
			noteInput.setKeyTranslationTable(notesToDrumTable);
		}
	}

	public void handleMainEncoder(final int inc) {
		noteRepeatHandler.handleMainEncoder(inc);
	}

	private BiColorLightState canScrollUp(final BiColorButton button) {
		if (padBank.scrollPosition().get() + (parent.isShiftHeld() ? 16 : 4) < 128) {
			if (button.isPressed()) {
				return BiColorLightState.FULL;
			}
			return BiColorLightState.HALF;
		}
		return BiColorLightState.OFF;
	}

	private BiColorLightState canScrollDown(final BiColorButton button) {
		if (padBank.scrollPosition().get() - (parent.isShiftHeld() ? 16 : 4) >= 0) {
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
		if (parent.isShiftHeld()) {
			padBank.scrollBy(4);
		} else {
			padBank.scrollBy(16);
		}
	}

	private void scrollBackward(final boolean pressed) {
		if (!pressed) {
			return;
		}
		if (parent.isShiftHeld()) {
			padBank.scrollBy(-4);
		} else {
			padBank.scrollBy(-16);
		}
	}

	public DisplayTarget getDiplayTarget() {
		return displayTarget;
	}

	public void activateView(final int typeIndex, final String paramName) {
		displayTarget.setTypeIndex(typeIndex, paramName);
		displayTarget.activate();
	}

	public void deactivateView() {
		displayTarget.deactivate();
	}

	public void modifyValue(final int typeIndex, final int inc) {
		if (selectedPad == null) {
			return;
		}
		selectedPad.modifyValue(typeIndex, inc, parent.isShiftHeld());
	}

	public void bindPadParameters(final Layer layer) {
		for (final PadContainer pad : pads) {
			pad.bindParameters(layer);
		}
	}

	public void updateDisplay(final int index) {
		if (selectedPad != null) {
			selectedPad.updateDisplay(index);
		}

	}

}
