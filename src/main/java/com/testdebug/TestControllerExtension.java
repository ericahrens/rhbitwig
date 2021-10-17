package com.testdebug;

import java.util.ArrayList;
import java.util.List;

import com.bitwig.extension.api.util.midi.ShortMidiMessage;
import com.bitwig.extension.callback.ShortMidiMessageReceivedCallback;
import com.bitwig.extension.controller.ControllerExtension;
import com.bitwig.extension.controller.ControllerExtensionDefinition;
import com.bitwig.extension.controller.api.Application;
import com.bitwig.extension.controller.api.Arpeggiator;
import com.bitwig.extension.controller.api.ControllerHost;
import com.bitwig.extension.controller.api.HardwareSurface;
import com.bitwig.extension.controller.api.MidiIn;
import com.bitwig.extension.controller.api.MidiOut;
import com.bitwig.extension.controller.api.NoteInput;
import com.bitwig.extension.controller.api.SettableEnumValue;
import com.bitwig.extension.controller.api.Transport;
import com.bitwig.extensions.framework.Layers;
import com.yaeltex.debug.RemoteConsole;

public class TestControllerExtension extends ControllerExtension {

	private static final String HEADER = "F0 00 20 29 02 0E ";
	private static final String INQ = "F0 7E 7F 06 01 F7";
	private static final String DAW_MODE = "0E 01";
	private static final String STAND_ALONE_MODE = "0E 00";

	private HardwareSurface surface;
	private MidiIn midiIn;
	private MidiOut midiOut;
	private Layers layers;
	private NoteInput noteInput;
	private Transport transport;
	private Application application;

	protected TestControllerExtension(final ControllerExtensionDefinition definition,
			final ControllerHost host) {
		super(definition, host);
	}

	@Override
	public void init() {
		final ControllerHost host = getHost();
		layers = new Layers(this);
		surface = host.createHardwareSurface();
		transport = host.createTransport();
		application = host.createApplication();
		midiIn = host.getMidiInPort(0);
		midiIn.setMidiCallback((ShortMidiMessageReceivedCallback) msg -> onMidi0(msg));
		midiOut = host.getMidiOutPort(0);

		noteInput = midiIn.createNoteInput("MIDI", "80????", "90????", "A0????", "D0????");
		noteInput.setShouldConsumeEvents(false);

		setUpMidiSysExCommands();
		host.showPopupNotification(" Intialize Test ");
		fill();
		sendSysExCommand(DAW_MODE);
	}

	private void setUpMidiSysExCommands() {
		midiIn.setSysexCallback(data -> {
			RemoteConsole.out.println(" MIDI SYS EX {}", data);
		});
	}

	private void fill() {
//		for (int row = 0; row < 8; row++) {
//			for (int col = 0; col < 8; col++) {
//				final int note = (row + 1) * 10 + col + 1;
//				final int index = row * 8 + col;
//				midiOut.sendMidi(144, note, index);
//			}
//		}
	}

	private void onMidi0(final ShortMidiMessage msg) {
		if (msg.getChannel() == 0 && msg.getStatusByte() == 144) {
		}
	}

	public NoteInput getNoteInput() {
		return noteInput;
	}

	private void sendSysExCommand(final String command) {
		midiOut.sendSysex(HEADER + command + " F7");
	}

	@Override
	public void exit() {
		sendSysExCommand(STAND_ALONE_MODE);
		getHost().showPopupNotification("Test Exited");
	}

	@Override
	public void flush() {
		surface.updateHardware();
	}

	public Application getApplication() {
		return application;
	}
}
