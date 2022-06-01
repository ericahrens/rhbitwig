package com.akai.fire.sequence;

import com.akai.fire.ColorLookup;
import com.akai.fire.display.ParameterDisplayBinding;
import com.akai.fire.lights.RgbLigthState;
import com.bitwig.extension.controller.api.DrumPad;
import com.bitwig.extension.controller.api.Send;
import com.bitwig.extensions.framework.Layer;
import com.bitwig.extensions.rh.BooleanValueObject;
import com.bitwig.extensions.rh.DawColor;

class PadContainer {

    private static final double SHIFT_INC = 0.01;
    private static final double REGULAR_INC = 0.025;

    private static final RgbLigthState TR_RED = new RgbLigthState(70, 0, 0, true);
    private static final RgbLigthState TR_ORANGE = new RgbLigthState(90, 15, 0, true);
    private static final RgbLigthState TR_YELLOW = new RgbLigthState(110, 55, 0, true);
    private static final RgbLigthState TR_WHITE = new RgbLigthState(80, 80, 80, true);

    private static final RgbLigthState[] fixedPadColorTable = {TR_RED, TR_RED, TR_RED, TR_RED, //
            TR_ORANGE, TR_ORANGE, TR_ORANGE, TR_ORANGE, TR_YELLOW, TR_YELLOW, TR_YELLOW, TR_YELLOW, //
            TR_WHITE, TR_WHITE, TR_WHITE, TR_WHITE};

    private final PadHandler padHandler;

    private RgbLigthState padColor;
    private RgbLigthState bitwigPadColor = RgbLigthState.OFF;

    private final RgbLigthState muteColor = ColorLookup.getColor(DawColor.LIGHT_BROWN);
    private final RgbLigthState soloColor = ColorLookup.getColor(DawColor.BLUISH_GREEN);

    final DrumPad pad;
    final int index;

    private final BooleanValueObject playing;
    private boolean selected;
    private boolean exists;

    private final ParameterDisplayBinding volumeBinding;
    private final ParameterDisplayBinding panBinding;
    private final ParameterDisplayBinding[] sendBindings = new ParameterDisplayBinding[8];

    public PadContainer(final PadHandler padHandler, final int index, final DrumPad pad,
                        final BooleanValueObject playing) {
        super();
        this.padHandler = padHandler;
        this.index = index;
        this.pad = pad;
        this.playing = playing;
        this.playing.markInterested();

        for (int i = 0; i < 8; i++) {
            final Send sendItem = pad.sendBank().getItemAt(i);
            sendBindings[i] = new ParameterDisplayBinding(i + 2, index, sendItem, padHandler.getDiplayTarget(), false);
        }

        pad.mute().markInterested();
        pad.solo().markInterested();
        pad.name().markInterested();
        pad.addIsSelectedInEditorObserver(selected -> handlePadSelection(index, selected));
        pad.exists().addValueObserver(exists -> this.exists = exists);
        //padColor = fixedPadColorTable[index];
        padColor = RgbLigthState.OFF;
        pad.color().addValueObserver((r, g, b) -> {
            padColor = ColorLookup.getColor(r, g, b);
            bitwigPadColor = ColorLookup.getColor(r, g, b);
            // padColor = fixedPadColorTable[index];
            if (selected) {
                this.padHandler.currentPadColor = bitwigPadColor;
            }
        });
        volumeBinding = new ParameterDisplayBinding(0, index, pad.volume(), padHandler.getDiplayTarget(), false);
        panBinding = new ParameterDisplayBinding(1, index, pad.pan(), padHandler.getDiplayTarget(), true);
    }


    public void bindParameters(final Layer layer) {
        layer.addBinding(volumeBinding);
        layer.addBinding(panBinding);
        for (final ParameterDisplayBinding binding : sendBindings) {
            layer.addBinding(binding);
        }
    }

    public RgbLigthState getPadColor() {
        return padColor;
    }

    public RgbLigthState getBitwigPadColor() {
        return bitwigPadColor;
    }

    public int getIndex() {
        return index;
    }

    private void handlePadSelection(final int index, final boolean selected) {
        this.selected = selected;
        if (this.selected) {
            padHandler.executePadSelection(this);
        }
    }

    public RgbLigthState mutingColors() {
        if (!exists) {
            return RgbLigthState.OFF;
        }
        if (pad.mute().get()) {
            return playing.returnTrueFalse(muteColor.getDimmed(), muteColor.getVeryDimmed());
        }
        return playing.returnTrueFalse(muteColor.getBrightest(), muteColor);
    }

    public RgbLigthState soloingColors() {
        if (!exists) {
            return RgbLigthState.OFF;
        }
        if (pad.solo().get()) {
            return playing.returnTrueFalse(soloColor.getBrightest(), soloColor);
        }
        return playing.returnTrueFalse(soloColor.getDimmed(), soloColor.getVeryDimmed());
    }

    public String getName() {
        return pad.name().get();
    }

    public RgbLigthState getColor() {
//        if (!exists) {
//            return RgbLigthState.OFF;
//        }
        if (selected) {
            return playing.returnTrueFalse(padColor.getBrightest(), padColor.getBrightend());
        }
        return playing.returnTrueFalse(padColor, padColor.getDimmed());
    }

    public void select() {
        pad.selectInEditor();
    }

    public void modifyValue(final int typeIndex, final int inc, final boolean shiftHeld) {
        final double amount = inc * (shiftHeld ? SHIFT_INC : REGULAR_INC);
        switch (typeIndex) {
            case 0:
                volumeBinding.modify(amount);
                break;
            case 1:
                panBinding.modify(amount);
                break;
            case 2:
                sendBindings[0].modify(amount);
                break;
            case 3:
                sendBindings[1].modify(amount);
                break;
            default:
                break;
        }
    }

    public void updateDisplay(final int typeIndex) {
        switch (typeIndex) {
            case 0:
                volumeBinding.update();
                break;
            case 1:
                panBinding.update();
                break;
            case 2:
                sendBindings[0].update();
                break;
            case 3:
                sendBindings[1].update();
                break;
            default:
                break;
        }
    }
}