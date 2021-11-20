package com.yaeltex2;

import java.util.function.IntConsumer;

import com.bitwig.extension.controller.api.ControllerHost;
import com.bitwig.extension.controller.api.HardwareSurface;
import com.bitwig.extension.controller.api.MidiIn;
import com.bitwig.extension.controller.api.MidiOut;
import com.bitwig.extension.controller.api.RelativeHardwarControlBindable;
import com.bitwig.extensions.framework.Layers;

public interface ExtensionDriver {
	HardwareSurface getSurface();

	MidiIn getMidiIn();

	MidiOut getMidiOut();

	Layers getLayers();

	ViewCursorControl getViewControl();

	RingEncoder[] getEncoders();

	ColorButton[] getModeButtons();

	ColorButton[] getRowButtons();

	ControllerHost getHost();

	ScaleSetting getScaleSetting();

	RelativeHardwarControlBindable createIncrementBinder(final IntConsumer consumer);
}
