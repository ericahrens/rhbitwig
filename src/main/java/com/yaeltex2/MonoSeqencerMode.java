package com.yaeltex2;

import com.bitwig.extension.controller.api.ControllerHost;
import com.bitwig.extension.controller.api.CursorTrack;
import com.bitwig.extension.controller.api.NoteStep;
import com.bitwig.extension.controller.api.NoteStep.State;
import com.bitwig.extension.controller.api.PinnableCursorClip;
import com.bitwig.extensions.framework.Layer;
import com.bitwig.extensions.rh.BooleanValueObject;
import com.yaeltex2.binding.NoteRingValueBinding;
import com.yaeltex2.binding.RingDoubleValueBinding;

public class MonoSeqencerMode extends Layer {
	private static final String LAYER_NAME = "DRUM_SEQUENCE_LAYER";
	private final CursorTrack cursorTrack;
	private final PinnableCursorClip cursorClip;
	private final NoteValue[] notes = new NoteValue[32];

	private final Layer encoderSemiTransposeLayer;
	private final Layer mainButtonLayer;
	private final Layer encoderProbLayer;

	private Layer currentEncoderLayer;
	private final Layer buttonRowLayer;

	private final SeqButtonModeLayer seqModeButtonLayer;

	private final ControllerHost host;
	private final StepViewPosition position;
	private int playingStep;
	private final double gatePercent = 0.98;
	private final ScaleSetting scale;

	private final BooleanValueObject stepMode = new BooleanValueObject();
	private final BooleanValueObject octaveMode = new BooleanValueObject();
	private final BooleanValueObject velMode = new BooleanValueObject(); // TODO needs to be enum
	private final Layer encoderVelLayer;

	public MonoSeqencerMode(final ExtensionDriver driver) {
		super(driver.getLayers(), LAYER_NAME);
		encoderSemiTransposeLayer = new Layer(driver.getLayers(), LAYER_NAME + "_ENC_SEMI");
		encoderVelLayer = new Layer(driver.getLayers(), LAYER_NAME + "_ENC_VEL");
		encoderProbLayer = new Layer(driver.getLayers(), LAYER_NAME + "_ENC_PROB");

		mainButtonLayer = new Layer(driver.getLayers(), LAYER_NAME + "_MAIN_BUTTON");
		seqModeButtonLayer = new SeqButtonModeLayer(driver);

		buttonRowLayer = seqModeButtonLayer;
		currentEncoderLayer = encoderSemiTransposeLayer;

		cursorTrack = driver.getViewControl().getCursorTrack();
		cursorClip = cursorTrack.createLauncherCursorClip("MonoClip", "MonoClip", 32, 128);
		cursorClip.addNoteStepObserver(this::handleNoteStep);
		cursorClip.playingStep().addValueObserver(this::handlePlayingStep);
		position = new StepViewPosition(cursorClip);
		host = driver.getHost();
		this.scale = driver.getScaleSetting();
		final RingEncoder[] encoders = driver.getEncoders();

		for (int i = 0; i < encoders.length; i++) {
			final int index = i;
			notes[index] = new NoteValue(index, 60);
			final RingEncoder encoder = encoders[i];
			final ColorButton button = encoder.getButton();

			encoder.bind(encoderSemiTransposeLayer, inc -> handleNoteChange(index, inc));
			final NoteValue noteValue = notes[index];
			encoderSemiTransposeLayer.addBinding(new NoteRingValueBinding(noteValue, encoder));
			button.bindPressed(encoderSemiTransposeLayer, p -> handleStepPress(p, index), () -> stepColor(index));

			encoder.bind(encoderVelLayer, inc -> handleVelChange(index, inc));
			encoderVelLayer.addBinding(new RingDoubleValueBinding(noteValue, encoder, noteValue::addVelValueObserver));

			encoderProbLayer
					.addBinding(new RingDoubleValueBinding(noteValue, encoder, noteValue::addChanceValueObserver));
			encoder.bind(encoderProbLayer, inc -> handleChanceChange(index, inc));
		}
		initMainButtons(driver.getModeButtons());
		seqModeButtonLayer.getEncoderMode().addValueObserver(this::handleModeChange);
		buttonRowLayer.activate();
	}

	private void initMainButtons(final ColorButton[] colorButtons) {
		colorButtons[0].bindPressed(mainButtonLayer, stepMode, ColorButtonLedState.AQUA, ColorButtonLedState.WHITE);
		colorButtons[7].bindPressed(mainButtonLayer, octaveMode, ColorButtonLedState.AQUA, ColorButtonLedState.WHITE);
	}

	private void handleModeChange(final SeqButtonEncoderMode oldValue, final SeqButtonEncoderMode newMode) {
		if (newMode == oldValue) {
			return;
		}
		Layer deactivateLayer = null;
		Layer newLayer = null;
		if (oldValue != SeqButtonEncoderMode.NOTE) {
			deactivateLayer = currentEncoderLayer;
		}
		switch (newMode) {
		case VELOCITY:
			newLayer = encoderVelLayer;
			break;
		case CHANCE:
			newLayer = encoderProbLayer;
			break;
		default:
			break;
		}
		if (deactivateLayer != null) {
			deactivateLayer.deactivate();
		}
		if (newLayer != null) {
			currentEncoderLayer = newLayer;
			currentEncoderLayer.activate();
		}
	}

	private void handleStepPress(final boolean pressed, final int index) {
		if (!pressed) {
			return;
		}
		if (stepMode.get()) {
			final double newLen = position.lengthWithLastStep(index);
			// adjustMode(newLen);
			cursorClip.getLoopLength().set(newLen);
		} else {
			if (notes[index] != null && notes[index].isPresent()) {
				cursorClip.toggleStep(index, notes[index].getNoteValue(), notes[index].getVelocity());
			} else {
				cursorClip.setStep(index, notes[index].getNoteValue(), notes[index].getVelocity(),
						position.getGridResolution() * gatePercent);
			}
		}
	}

//	private void adjustMode(final double clipLength) {
//		final int notes = (int) (clipLength / 0.25);
//		adjustMode(notes);
//	}

	private void handleChanceChange(final int index, final int increment) {
		final NoteValue note = notes[index];
		if (!note.isPresent()) {
			return;
		}
		final double newValue = Math.max(0,
				Math.min(1, note.getChance() + increment * (octaveMode.get() ? 0.025 : 0.01)));
		note.setChance(newValue);
		note.getNoteStep().setChance(newValue);
	}

	private void handleVelChange(final int index, final int increment) {
		final NoteValue note = notes[index];
		if (!note.isPresent()) {
			return;
		}
		final double newVel = Math.max(0,
				Math.min(1, note.getVelocityValue() + increment * (octaveMode.get() ? 0.025 : 0.01)));
		note.setVelocityValue(newVel);
		note.getNoteStep().setVelocity(newVel);
	}

	private void handleNoteChange(final int index, final int increment) {
		final NoteValue note = notes[index];
		if (!note.isPresent()) {
			return;
		}
		final int nv = note.getNoteValue();
		final int newNote = scale.applyIncrement(note.getNoteValue(), increment * (octaveMode.get() ? 12 : 1));
		if (newNote < 128 && newNote >= 0) {
			note.notifyChange(newNote);
			cursorClip.clearStep(index, nv);
			cursorClip.setStep(index, newNote, note.getVelocity(), note.getDuration());
		}
	}

	private ColorButtonLedState stepColor(final int index) {
		final int maxSteps = position.getAvailableSteps();
		if (index < maxSteps) {
			if (notes[index].isPresent()) {
				if (index == this.playingStep) {
					return ColorButtonLedState.BLUE_ACTIVE;
				}
				return ColorButtonLedState.BLUE;
			}
			if (index == this.playingStep) {
				return ColorButtonLedState.WHITE_DIM;
			}
			return ColorButtonLedState.WHITE;
		}
		return null;
	}

	private void handlePlayingStep(final int playingStep) {
		if (playingStep == -1) {
			this.playingStep = -1;
		}
		this.playingStep = playingStep - position.getStepOffset();
	}

	// TODO think about triggering notes
	private void handleNoteStep(final NoteStep notestep) {
		final int which = notestep.x();
		if (notestep.state() == State.NoteOn) {
			notes[which].apply(notestep);
		} else {
			if (notestep.y() == notes[which].getNoteValue()) {
				notes[which].setPresent(false);
			}
		}
	}

	@Override
	protected void onActivate() {
		currentEncoderLayer.activate();
		buttonRowLayer.activate();
		mainButtonLayer.activate();
	}

	@Override
	protected void onDeactivate() {
		currentEncoderLayer.deactivate();
		buttonRowLayer.deactivate();
		mainButtonLayer.deactivate();
	}

}
