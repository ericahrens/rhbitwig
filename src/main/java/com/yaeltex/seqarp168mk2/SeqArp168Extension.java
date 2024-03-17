package com.yaeltex.seqarp168mk2;

import com.bitwig.extension.controller.ControllerExtension;
import com.bitwig.extension.controller.api.ControllerHost;
import com.bitwig.extension.controller.api.HardwareSurface;
import com.bitwig.extensions.framework.Layer;
import com.bitwig.extensions.framework.di.Context;
import com.yaeltex.common.YaeltexButtonLedState;
import com.yaeltex.common.YaeltexIntensityColorState;
import com.yaeltex.common.YaeltexMidiProcessor;
import com.yaeltex.controls.RgbButton;
import com.yaeltex.seqarp168mk2.arpcontrol.ArpButtonMode;
import com.yaeltex.seqarp168mk2.arpcontrol.ArpControlLayer;
import com.yaeltex.seqarp168mk2.sequencer.SequencerLayer;

public class SeqArp168Extension extends ControllerExtension {
    
    private static ControllerHost debugHost;
    private Layer mainLayer;
    private HardwareSurface surface;
    private final YaeltexIntensityColorState cursorColor = new YaeltexIntensityColorState(12, 127);
    private Mode mode = Mode.ARP;
    private boolean optHeld;
    private boolean buttonCombinationOccurred;
    private ArpControlLayer arpLayer;
    private SequencerLayer sequencerLayer;
    private RemotesLayer remotesLayer;
    private SendsLayer sendsLayer;
    private Layer currentLayer;
    
    public static void println(final String format, final Object... args) {
        if (debugHost != null) {
            debugHost.println(format.formatted(args));
        }
    }
    
    protected SeqArp168Extension(final SeqArp168ExtensionDefinition definition, final ControllerHost host) {
        super(definition, host);
    }
    
    @Override
    public void init() {
        debugHost = getHost();
        final Context diContext = new Context(this);
        final YaeltexMidiProcessor midiProcessor = new YaeltexMidiProcessor(getHost());
        diContext.registerService(YaeltexMidiProcessor.class, midiProcessor);
        mainLayer = diContext.createLayer("MAIN_LAYER");
        surface = diContext.getService(HardwareSurface.class);
        arpLayer = diContext.getService(ArpControlLayer.class);
        sequencerLayer = diContext.getService(SequencerLayer.class);
        remotesLayer = diContext.getService(RemotesLayer.class);
        sendsLayer = diContext.getService(SendsLayer.class);
        initModeButton(diContext);
        
        mainLayer.activate();
        currentLayer.activate();
        diContext.activate();
        midiProcessor.start();
    }
    
    private void initModeButton(final Context diContext) {
        final SeqArpHardwareElements hwElements = diContext.getService(SeqArpHardwareElements.class);
        currentLayer = arpLayer;
        bindArpModeButtons(hwElements, mainLayer);
    }
    
    private void bindArpModeButtons(final SeqArpHardwareElements hwElements, final Layer layer) {
        final RgbButton optButton = hwElements.getControlButton(6);
        optButton.bindLight(layer, () -> getModeOptButtonState(arpLayer));
        optButton.bindIsPressed(layer, this::handleOptButtonPressed);
        
        final RgbButton timeWarpButton = hwElements.getControlButton(3);
        timeWarpButton.bindIsPressed(layer, pressed -> handleTimeWarpPressed(pressed));
        timeWarpButton.bindLightPressed(
            layer, pressed -> pressed ? YaeltexButtonLedState.GREEN : YaeltexButtonLedState.WHITE);
        
        final RgbButton stepMuteButton = hwElements.getControlButton(0);
        stepMuteButton.bindLight(layer, this::getStepMuteLight);
        stepMuteButton.bindPressed(layer, this::handleStepMute);
        
        final RgbButton velMuteButton = hwElements.getControlButton(1);
        velMuteButton.bindLight(layer, this::getVelMuteLight);
        velMuteButton.bindPressed(layer, this::handleVelMute);
        
        final RgbButton stepGlobalVelButton = hwElements.getControlButton(4);
        stepGlobalVelButton.bindLight(layer, this::getStepVelLight);
        stepGlobalVelButton.bindPressed(layer, this::handleStepVel);
        
        final RgbButton stepGlobalGateButton = hwElements.getControlButton(5);
        stepGlobalGateButton.bindLight(layer, this::getStepGlobalGateLight);
        stepGlobalGateButton.bindPressed(layer, this::handleStepGlobal);
    }
    
    private YaeltexButtonLedState getStepGlobalGateLight() {
        if (mode == Mode.ARP) {
            return arpLayer.getButtonMode() == ArpButtonMode.STEP_GATE_GLOBAL
                ? YaeltexButtonLedState.GREEN
                : YaeltexButtonLedState.OFF;
        } else if (mode == Mode.SEQUENCER) {
            return sequencerLayer.isShuffleActive()
                ? YaeltexButtonLedState.DARK_ORANGE
                : YaeltexButtonLedState.ORANGE_DIM;
        } else if (mode == Mode.REMOTES) {
            return remotesLayer.getMode() == RemotesLayer.RemoteMode.DEVICE
                ? YaeltexButtonLedState.ORANGE
                : YaeltexButtonLedState.OFF;
        }
        return YaeltexButtonLedState.OFF;
    }
    
    private void handleTimeWarpPressed(final boolean pressed) {
        if (mode != Mode.ARP) {
            setMode(Mode.ARP);
        }
        arpLayer.setTimeWarpActive(pressed);
    }
    
    private void handleStepGlobal() {
        if (mode == Mode.ARP) {
            arpLayer.setMode(ArpButtonMode.STEP_GATE_GLOBAL);
        } else if (mode == Mode.SEQUENCER) {
            sequencerLayer.toggleShuffle();
        } else if (mode == Mode.REMOTES) {
            remotesLayer.setMode(RemotesLayer.RemoteMode.DEVICE);
        }
    }
    
    private void handleOptButtonPressed(final boolean pressed) {
        optHeld = pressed;
        if (!pressed && !buttonCombinationOccurred) {
            if (mode != Mode.ARP) {
                setMode(Mode.ARP);
            }
            arpLayer.handleOptMode();
        }
        buttonCombinationOccurred = false;
    }
    
    private void handleStepMute() {
        if (optHeld) {
            buttonCombinationOccurred = true;
            setMode(Mode.SEQUENCER);
        } else {
            if (mode == Mode.ARP) {
                arpLayer.setMode(ArpButtonMode.STEP_MUTE);
            } else if (mode == Mode.REMOTES) {
                remotesLayer.setMode(RemotesLayer.RemoteMode.TRACK);
            }
        }
    }
    
    public Mode getMode() {
        return mode;
    }
    
    private void setMode(final Mode mode) {
        if (this.mode == mode) {
            return;
        }
        currentLayer.setIsActive(false);
        this.mode = mode;
        currentLayer = modeToLayer(this.mode);
        currentLayer.setIsActive(true);
    }
    
    private Layer modeToLayer(final Mode mode) {
        return switch (mode) {
            case ARP -> arpLayer;
            case SENDS -> sendsLayer;
            case REMOTES -> remotesLayer;
            case SEQUENCER -> sequencerLayer;
        };
    }
    
    private YaeltexButtonLedState getStepMuteLight() {
        if (mode == Mode.ARP) {
            return arpLayer.getButtonMode() == ArpButtonMode.STEP_MUTE
                ? YaeltexButtonLedState.GREEN
                : YaeltexButtonLedState.OFF;
        } else if (mode == Mode.SEQUENCER) {
            return YaeltexButtonLedState.PURPLE;
        } else if (mode == Mode.REMOTES) {
            return remotesLayer.getMode() == RemotesLayer.RemoteMode.TRACK
                ? YaeltexButtonLedState.ORANGE
                : YaeltexButtonLedState.OFF;
        }
        return YaeltexButtonLedState.OFF;
    }
    
    private YaeltexButtonLedState getVelMuteLight() {
        if (mode == Mode.ARP) {
            return arpLayer.getButtonMode() == ArpButtonMode.VEL_MUTE
                ? YaeltexButtonLedState.GREEN
                : YaeltexButtonLedState.OFF;
        } else if (mode == Mode.SENDS) {
            return YaeltexButtonLedState.PURPLE;
        } else if (mode == Mode.REMOTES) {
            return remotesLayer.getMode() == RemotesLayer.RemoteMode.PROJECT
                ? YaeltexButtonLedState.ORANGE
                : YaeltexButtonLedState.OFF;
        }
        return YaeltexButtonLedState.OFF;
    }
    
    private YaeltexButtonLedState getStepVelLight() {
        if (mode == Mode.ARP) {
            return arpLayer.getButtonMode() == ArpButtonMode.STEP_VEL_GLOBAL
                ? YaeltexButtonLedState.GREEN
                : YaeltexButtonLedState.OFF;
        } else if (mode == Mode.REMOTES) {
            return YaeltexButtonLedState.PURPLE;
        }
        return YaeltexButtonLedState.OFF;
    }
    
    
    private void handleVelMute() {
        if (optHeld) {
            buttonCombinationOccurred = true;
            setMode(Mode.SENDS);
        } else {
            if (mode == Mode.ARP) {
                arpLayer.setMode(ArpButtonMode.VEL_MUTE);
            } else if (mode == Mode.REMOTES) {
                remotesLayer.setMode(RemotesLayer.RemoteMode.PROJECT);
            }
        }
    }
    
    private void handleStepVel() {
        if (optHeld) {
            buttonCombinationOccurred = true;
            setMode(Mode.REMOTES);
        } else {
            if (mode == Mode.ARP) {
                arpLayer.setMode(ArpButtonMode.STEP_VEL_GLOBAL);
            }
        }
    }
    
    private YaeltexButtonLedState getModeOptButtonState(final ArpControlLayer arpLayer) {
        if (mode == Mode.ARP) {
            return arpLayer.getButtonMode() == ArpButtonMode.DEFAULT
                ? YaeltexButtonLedState.GREEN
                : arpLayer.getButtonMode() == ArpButtonMode.PAT
                    ? YaeltexButtonLedState.BLUE
                    : YaeltexButtonLedState.OFF;
        }
        return YaeltexButtonLedState.OFF;
    }
    
    public void exit() {
        getHost().showPopupNotification("YaeltexArpControl Exited");
    }
    
    @Override
    public void flush() {
        surface.updateHardware();
    }
    
}
