package com.yaeltex2;

import java.util.function.IntConsumer;

import com.bitwig.extension.api.util.midi.ShortMidiMessage;
import com.bitwig.extension.callback.ShortMidiMessageReceivedCallback;
import com.bitwig.extension.controller.ControllerExtension;
import com.bitwig.extension.controller.api.ControllerHost;
import com.bitwig.extension.controller.api.HardwareSurface;
import com.bitwig.extension.controller.api.MidiIn;
import com.bitwig.extension.controller.api.MidiOut;
import com.bitwig.extension.controller.api.RelativeHardwarControlBindable;
import com.bitwig.extensions.framework.Layer;
import com.bitwig.extensions.framework.Layers;
import com.bitwig.extensions.rh.Midi;
import com.yaeltex.EncoderUtil;
import com.yaeltex.RgbButton;

public class YaeltexSeqArp168Extension extends ControllerExtension implements ExtensionDriver {
	private HardwareSurface surface;
	private MidiIn midiIn;
	private MidiOut midiOut;

	public static final int[] BUTTON_INDEX = new int[] { //
			56, 40, 64, 48 };

	public static final int MODE_BUTTON_OFFSET = 32;

	private Layers layers;

	private final int[] lastCcValue = new int[128];

	final RingEncoder[] encoders = new RingEncoder[32];
	final ColorButton[] rowButtons = new ColorButton[32];
	final ColorButton[] modeButtons = new ColorButton[8];

	private Layer mainLayer;
	private ViewCursorControl viewControl;
	private ScaleSetting scaleSetting;

	protected YaeltexSeqArp168Extension(final YaeltexSeqArp168Definition definition, final ControllerHost host) {
		super(definition, host);
	}

	@Override
	public void init() {
		final ControllerHost host = getHost();
		for (int i = 0; i < lastCcValue.length; i++) {
			lastCcValue[i] = -1;
		}
		layers = new Layers(this);
		midiIn = host.getMidiInPort(0);
		midiIn.setMidiCallback((ShortMidiMessageReceivedCallback) msg -> onMidi0(msg));
		midiOut = host.getMidiOutPort(0);
		surface = host.createHardwareSurface();
		viewControl = new ViewCursorControl(host);
		scaleSetting = new ScaleSetting();

		setUpHardware();

		mainLayer = new Layer(layers, "Main");
		mainLayer.activate();
		final MonoSeqencerMode seqLayer = new MonoSeqencerMode(this);
		seqLayer.activate();

		host.showPopupNotification("SEQ ARP 168 Initialized");
	}

	@Override
	public HardwareSurface getSurface() {
		return surface;
	}

	@Override
	public MidiIn getMidiIn() {
		return midiIn;
	}

	@Override
	public Layers getLayers() {
		return layers;
	}

	@Override
	public ViewCursorControl getViewControl() {
		return viewControl;
	}

	@Override
	public RingEncoder[] getEncoders() {
		return encoders;
	}

	@Override
	public RelativeHardwarControlBindable createIncrementBinder(final IntConsumer consumer) {
		final ControllerHost host = getHost();
		return host.createRelativeHardwareControlStepTarget(//
				host.createAction(() -> consumer.accept(1), () -> "+"),
				host.createAction(() -> consumer.accept(-1), () -> "-"));
	}

	private void setUpHardware() {
		for (int i = 0; i < 32; i++) {
			final int index = i;
			final int midiValue = EncoderUtil.ENCODER_INDEX[i];

			encoders[index] = new RingEncoder(index, midiValue, this);
//			final RelativeHardwareKnob encoder = surface.createRelativeHardwareKnob("ENDLESKNOB_" + i);
//
//			encoders[index] = encoder;
//			encoder.setAdjustValueMatcher(midiIn.createRelativeBinOffsetCCValueMatcher(0, midiValue, 64));
//			encoder.setStepSize(1 / 64.0);
//
//			final HardwareActionBindable incAction = getHost().createAction(() -> {
//				value[index] = changeValue(value[index], 1);
//				midiOut.sendMidi(Midi.CC, midiValue, value[index]);
//				midiOut.sendMidi(Midi.CC | 15, midiValue, value[index]);
//			}, () -> "+");
//			final HardwareActionBindable decAction = getHost().createAction(() -> {
//				value[index] = changeValue(value[index], -1);
//				midiOut.sendMidi(Midi.CC, midiValue, value[index]);
//			}, () -> "-");
//			encoder.addBinding(getHost().createRelativeHardwareControlStepTarget(incAction, decAction));
		}

		for (int i = 0; i < 16; i++) {
			int midiValue = BUTTON_INDEX[i / 8] + i % 8;
			rowButtons[i] = new ColorButton(this, "R", i, BUTTON_INDEX[i / 8] + i % 8, 0);

			midiValue = BUTTON_INDEX[i / 8 + 2] + i % 8;
			rowButtons[i + 16] = new ColorButton(this, "R", i + 16, midiValue, 0);
		}
		for (int i = 0; i < 8; i++) {
			modeButtons[i] = new ColorButton(this, "MD", i, MODE_BUTTON_OFFSET + i, 0);
		}

	}

	int changeValue(final int value, final int amount) {
		return Math.min(Math.max(0, value + amount), 127);
	}

	private void onMidi0(final ShortMidiMessage msg) {
		// getHost().println("MIDI " + msg.getStatusByte() + " " + msg.getData1() + " "
		// + msg.getData2());
	}

	@Override
	public void exit() {
		getHost().showPopupNotification("Yaeltex SEQ ARP 168 - SPEC");
	}

	@Override
	public void flush() {
		surface.updateHardware();
	}

	private int count = 0;

	public void sendCC(final int ccNr, final int value) {
		if (lastCcValue[ccNr] == -1 || lastCcValue[ccNr] != value) {
			midiOut.sendMidi(Midi.CC, ccNr, value);
			lastCcValue[ccNr] = value;
			count++;
			if (count % 128 == 0) { // Device seems get into hickups when bombarded with CCS
				pause(10); // This giving it some air seems to improve things
			}
		}
	}

	public void pause(final int amount) {
		try {
			Thread.sleep(amount);
			if (count > 10000) {
				count = 1;
			}
		} catch (final InterruptedException e) {
		}
	}

	public void initButton(final RgbButton button) {
		midiOut.sendMidi(button.getMidiStatus(), button.getMidiDataNr(), 0);
		count++;
	}

	@Override
	public MidiOut getMidiOut() {
		return midiOut;
	}

	@Override
	public ScaleSetting getScaleSetting() {
		return scaleSetting;
	}

	@Override
	public ColorButton[] getModeButtons() {
		return modeButtons;
	}

	@Override
	public ColorButton[] getRowButtons() {
		return rowButtons;
	}

}
