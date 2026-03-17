package com.yaeltex.djm;

import java.util.List;

import com.bitwig.extension.controller.ControllerExtension;
import com.bitwig.extension.controller.api.AbsoluteHardwareKnob;
import com.bitwig.extension.controller.api.ControllerHost;
import com.bitwig.extension.controller.api.HardwareSlider;
import com.bitwig.extension.controller.api.HardwareSurface;
import com.bitwig.extension.controller.api.Track;
import com.bitwig.extensions.framework.Layer;
import com.bitwig.extensions.framework.di.Context;
import com.yaeltex.common.YaeltexButtonLedState;
import com.yaeltex.common.controls.RgbButton;
import com.yaeltex.common.controls.VuMeter;
import com.yaeltex.djm.definitions.DjmExtensionDefinition;

public class DjmControllerExtension extends ControllerExtension {
    
    private static ControllerHost debugHost;
    private HardwareSurface surface;
    private Layer mainLayer;
    private DjmMidiProcessor midiProcessor;
    private SculptMode sculptMode = SculptMode.FQ;
    private Layer sculptFqLayer;
    private Layer sculptCtLayer;
    private DjmAHwElements hwElementsA;
    
    private enum SculptMode {
        FQ,
        LF,
        EQ,
        CT
    }
    
    public static void println(final String format, final Object... args) {
        if (debugHost != null) {
            debugHost.println(format.formatted(args));
        }
    }
    
    public DjmControllerExtension(final DjmExtensionDefinition definition, final ControllerHost host) {
        super(definition, host);
    }
    
    @Override
    public void init() {
        debugHost = getHost();
        final Context diContext = new Context(this);
        midiProcessor = new DjmMidiProcessor(getHost(), 2);
        midiProcessor.setInitCallback(this::handleHwReady);
        surface = diContext.getService(HardwareSurface.class);
        surface.setPhysicalSize(380, 320);
        mainLayer = diContext.createLayer("MAIN");
        sculptFqLayer = diContext.createLayer("SCULPT_FQ_LAYER");
        sculptCtLayer = diContext.createLayer("SCULPT_CT_LAYER");
        hwElementsA = new DjmAHwElements(getHost(), surface, midiProcessor, 0);
        final DjmBHwElements hwElementsB = new DjmBHwElements(surface, midiProcessor, 1);
        final DjmViewControl viewControl = diContext.getService(DjmViewControl.class);
        final TargetRemotes<ProjectPage> remotes = viewControl.getProjectTargetRemotes();
        
        bindDjmBBoth(remotes.getRemotes(ProjectPage.BOOTH_EQ_MASTER_FILTER), hwElementsB);
        bindGain(remotes.getRemotes(ProjectPage.GAIN_KNOBS), hwElementsB);
        bindSmallFaders(remotes.getRemotes(ProjectPage.SMALL_FADERS), hwElementsB);
        bindChannelFaders(remotes.getRemotes(ProjectPage.CHANNEL_FADERS), hwElementsB);
        
        bindButtonsB(remotes, hwElementsB);
        bindVuMeters(viewControl, hwElementsB);
        bindVuMeterDjmA(viewControl, hwElementsA);
        binAuxSends(remotes.getRemotes(ProjectPage.AUX_SENDS), hwElementsA);
        bindAuxButtons(remotes.getRemotes(ProjectPage.AUX2_PRE_POST), hwElementsA);
        bindSculpt(remotes, hwElementsA);
        //bindLines(remotes, hwElementsA);
        bindStemButtons(remotes.getRemotes(ProjectPage.STEMS_FX_BUTTON), hwElementsA);
        clearTexts();
        
        mainLayer.setIsActive(true);
        applySculptMode();
        //hwElementsA.refreshHardware();
    }
    
     private void handleHwReady() {
        for (int i = 0; i < 20; i++) {
            midiProcessor.sendNoteColor(0, 0, i, YaeltexButtonLedState.GREEN);
            midiProcessor.sendNoteColor(0, 0, i, YaeltexButtonLedState.OFF);
            hwElementsA.refreshHardware();
        }
    }
    
    private void applySculptMode() {
        sculptCtLayer.setIsActive(sculptMode == SculptMode.LF);
        sculptFqLayer.setIsActive(sculptMode == SculptMode.FQ);
    }
    
    private void bindButtonsB(final TargetRemotes<ProjectPage> remotes, final DjmBHwElements hwElementsB) {
        final List<RgbButton> buttonsSmall = hwElementsB.getMasterButtons();
        final RemoteFixed remotesSmall = remotes.getRemotes(ProjectPage.FLG_FLT_SMALL);
        for (int i = 0; i < 8; i++) {
            final RgbButton button = buttonsSmall.get(i);
            button.bindToggleValueDimmed(
                mainLayer, remotesSmall.getParameter(i),
                i < 4 ? YaeltexButtonLedState.GREEN : YaeltexButtonLedState.RED);
        }
        final List<RgbButton> trackButtons = hwElementsB.getTrackButtons();
        final RemoteFixed remoteCueFilter = remotes.getRemotes(ProjectPage.CUE_FILTER);
        final RemoteFixed remoteEfx = remotes.getRemotes(ProjectPage.FLANGE_EFX);
        for (int i = 0; i < trackButtons.size(); i++) {
            final RgbButton button = trackButtons.get(i);
            final int rowIndex = i / 8;
            final int trackIndex = (i % 8) / 2;
            final int elementIndex = i % 2;
            if (rowIndex == 0) {
                if (elementIndex == 0) {
                    button.bindToggleValueDimmed(
                        mainLayer, remoteCueFilter.getParameter(trackIndex), YaeltexButtonLedState.WHITE);
                } else {
                    button.bindToggleValueDimmed(
                        mainLayer, remoteCueFilter.getParameter(trackIndex + 4), YaeltexButtonLedState.RED);
                }
            } else {
                if (elementIndex == 0) {
                    button.bindToggleValueDimmed(
                        mainLayer, remoteEfx.getParameter(trackIndex), YaeltexButtonLedState.GREEN);
                } else {
                    button.bindToggleValueDimmed(
                        mainLayer, remoteEfx.getParameter(trackIndex + 4), YaeltexButtonLedState.BLUE);
                }
            }
        }
    }
    
    private void bindVuMeterDjmA(final DjmViewControl viewControl, final DjmAHwElements hwElements) {
        final VuMeter vuLeft = hwElements.getVuLeft();
        final VuMeter vuRight = hwElements.getVuRight();
        final FlexTrackBank flexBank = viewControl.getFlexBank();
        mainLayer.addBinding(new VuStereoTrackBinding(flexBank.getTrack(FixedTracks.BOOTH), vuLeft, vuRight));
    }
    
    private void bindVuMeters(final DjmViewControl viewControl, final DjmBHwElements hwElements) {
        final Track rootTrack = viewControl.getRootTrack();
        final VuMeter vuLeft = hwElements.getVuLeft();
        final VuMeter vuRight = hwElements.getVuRight();
        rootTrack.addVuMeterObserver(128, 0, true, value -> vuLeft.sendVuValue(value));
        rootTrack.addVuMeterObserver(128, 1, true, value -> vuRight.sendVuValue(value));
        
        final FlexTrackBank flexBank = viewControl.getFlexBank();
        final FixedTracks[] mainTracks =
            {FixedTracks.TRACK_1, FixedTracks.TRACK_2, FixedTracks.TRACK_3, FixedTracks.TRACK_4};
        for (int i = 0; i < 4; i++) {
            final Track track = flexBank.getTrack(mainTracks[i]);
            final VuMeter vuMeter = hwElements.getTrackMeters().get(i);
            mainLayer.addBinding(new VuTrackBinding(track, vuMeter));
        }
        
        final List<VuMeter> meters = hwElements.getDeckMeters();
        mainLayer.addBinding(new VuTrackBinding(flexBank.getTrack(FixedTracks.FLG), meters.get(3)));
        mainLayer.addBinding(new VuTrackBinding(flexBank.getTrack(FixedTracks.REV), meters.get(4)));
        mainLayer.addBinding(new VuTrackBinding(flexBank.getTrack(FixedTracks.DLY), meters.get(5)));
        mainLayer.addBinding(new VuTrackBinding(flexBank.getTrack(FixedTracks.MEL), meters.get(6)));
        mainLayer.addBinding(new VuTrackBinding(flexBank.getTrack(FixedTracks.DRUM), meters.get(7)));
    }
    
    private void bindStemButtons(final RemoteFixed remotes, final DjmAHwElements hwElements) {
        final List<RgbButton> buttons = hwElements.getSideButtons();
        buttons.get(10).bindToggleValueDimmed(mainLayer, remotes.getParameter(6), YaeltexButtonLedState.PURPLE);
        buttons.get(11).bindToggleValueDimmed(mainLayer, remotes.getParameter(7), YaeltexButtonLedState.RED);
    }
    
    private void bindSculpt(final TargetRemotes<ProjectPage> remotes, final DjmAHwElements hwElements) {
        final List<DjmRingEncoder> sculptEncoders = hwElements.getSculptEncoders();
        final List<RgbButton> buttons = hwElements.getSideButtons();
        
        final RemoteFixed abRemotes = remotes.getRemotes(ProjectPage.CHANNEL_EQ_A_B);
        final RemoteFixed cdRemotes = remotes.getRemotes(ProjectPage.CHANNEL_EQ_C_D);
        final RemoteFixed ctEqRemotes = remotes.getRemotes(ProjectPage.EQ_CT_BUTTONS);
        
        final RgbButton sculptFqButton = buttons.get(6);
        final RgbButton sculptCtButton = buttons.get(2);
        
        final RgbButton eqButton = buttons.get(7);
        final RgbButton ctButton = buttons.get(3);
        
        eqButton.bindToggleValueDimmed(mainLayer, ctEqRemotes.getParameter(0), YaeltexButtonLedState.PURPLE);
        ctButton.bindToggleValueDimmed(mainLayer, ctEqRemotes.getParameter(1), YaeltexButtonLedState.BLUE_ACTIVE);
        
        sculptFqButton.bindLight(
            mainLayer, () -> sculptMode == SculptMode.FQ ? YaeltexButtonLedState.WHITE : YaeltexButtonLedState.OFF);
        sculptFqButton.bindPressed(mainLayer, () -> setMode(SculptMode.FQ));
        sculptCtButton.bindLight(
            mainLayer, () -> sculptMode == SculptMode.LF ? YaeltexButtonLedState.WHITE : YaeltexButtonLedState.OFF);
        sculptCtButton.bindPressed(mainLayer, () -> setMode(SculptMode.LF));
        
        
        for (int i = 0; i < 4; i++) {
            final AbsoluteHardwareKnob lpfKnob = hwElements.getLpfKnobs().get(i);
            final AbsoluteHardwareKnob hpfKnob = hwElements.getHpfKnobs().get(i);
            final RemoteFixed remotePage = i < 2 ? abRemotes : cdRemotes;
            final int paramOffset = (i % 2) * 4;
            mainLayer.bind(lpfKnob, remotePage.getParameter(paramOffset));
            mainLayer.bind(hpfKnob, remotePage.getParameter(paramOffset + 3));
            final DjmRingEncoder encoder = sculptEncoders.get(i);
            
            encoder.bindParameter(sculptFqLayer, remotePage.getParameter(paramOffset + 1));
            encoder.bindRingLightColor(sculptFqLayer, () -> YaeltexButtonLedState.BLUE);
            encoder.bindParameter(sculptCtLayer, remotePage.getParameter(paramOffset + 2));
            encoder.bindRingLightColor(sculptCtLayer, () -> YaeltexButtonLedState.ORANGE);
        }
    }
    
//    private void bindLines(final TargetRemotes<ProjectPage> remotes, final DjmAHwElements hwElements) {
//        final RemoteFixed abRemotes = remotes.getRemotes(ProjectPage.STEMS_A_B);
//        final RemoteFixed cdRemotes = remotes.getRemotes(ProjectPage.STEMS_C_D);
//
//        for (int i = 0; i < 4; i++) {
//            final DjmRingEncoder vocalEncoder = hwElements.getVocalEncoders().get(i);
//            final DjmRingEncoder melodyEncoder = hwElements.getMelodyEncoders().get(i);
//            final DjmRingEncoder bassEncoder = hwElements.getBaselineEncoders().get(i);
//            final DjmRingEncoder drumEncoder = hwElements.getDrumEncoders().get(i);
//            final RemoteFixed remotePage = i < 2 ? abRemotes : cdRemotes;
//            final int paramOffset = (i % 2) * 4;
//            vocalEncoder.bindParameter(mainLayer, remotePage.getParameter(paramOffset));
//            vocalEncoder.bindRingLightColor(mainLayer, () -> YaeltexButtonLedState.PURPLE);
//
//            melodyEncoder.bindParameter(mainLayer, remotePage.getParameter(paramOffset + 1));
//            melodyEncoder.bindRingLightColor(mainLayer, () -> YaeltexButtonLedState.BLUE);
//
//            bassEncoder.bindParameter(mainLayer, remotePage.getParameter(paramOffset + 2));
//            bassEncoder.bindRingLightColor(mainLayer, () -> YaeltexButtonLedState.DEEP_GREEN);
//
//            drumEncoder.bindParameter(mainLayer, remotePage.getParameter(paramOffset + 3));
//            drumEncoder.bindRingLightColor(mainLayer, () -> YaeltexButtonLedState.AQUA);
//        }
//    }
    
    private void setMode(final SculptMode mode) {
        if (this.sculptMode != mode) {
            this.sculptMode = mode;
            applySculptMode();
        }
    }
    
    private void bindAuxButtons(final RemoteFixed remotes, final DjmAHwElements hwElements) {
        final List<RgbButton> buttons = hwElements.getSideButtons();
        final int[] buttonMapping = {4, 0, 5, 1};
        for (int i = 0; i < 4; i++) {
            final RgbButton button = buttons.get(buttonMapping[i]);
            button.bindToggleValueDimmed(mainLayer, remotes.getParameter(i), YaeltexButtonLedState.ORANGE);
        }
    }
    
    private void binAuxSends(final RemoteFixed remotes, final DjmAHwElements hwElements) {
        final List<AbsoluteHardwareKnob> aux1Knobs = hwElements.getAux1Knobs();
        for (int i = 0; i < 4; i++) {
            mainLayer.bind(aux1Knobs.get(i), remotes.getParameter(i));
        }
        final List<AbsoluteHardwareKnob> aux2Knobs = hwElements.getAux2Knobs();
        for (int i = 0; i < 4; i++) {
            mainLayer.bind(aux2Knobs.get(i), remotes.getParameter(i + 4));
        }
    }
    
    private void bindDjmBBoth(final RemoteFixed remotes, final DjmBHwElements hwElements) {
        final List<AbsoluteHardwareKnob> controls = hwElements.getLargeKnobs();
        for (int i = 0; i < 8; i++) {
            mainLayer.bind(controls.get(i), remotes.getParameter(i));
        }
    }
    
    private void bindSmallFaders(final RemoteFixed remotes, final DjmBHwElements hwElementsB) {
        final List<HardwareSlider> faders = hwElementsB.getDeckSliders();
        for (int i = 0; i < 8; i++) {
            mainLayer.bind(faders.get(i), remotes.getParameter(i));
        }
    }
    
    private void bindChannelFaders(final RemoteFixed remotes, final DjmBHwElements hwElementsB) {
        final List<HardwareSlider> faders = hwElementsB.getTrackSliders();
        for (int i = 0; i < 4; i++) {
            mainLayer.bind(faders.get(i), remotes.getParameter(i));
        }
    }
    
    private void bindGain(final RemoteFixed remotes, final DjmBHwElements hwElements) {
        final List<AbsoluteHardwareKnob> controls = hwElements.getMasterGainKnobs();
        for (int i = 0; i < 4; i++) {
            mainLayer.bind(controls.get(fourBy2Mapping(i)), remotes.getParameter(i));
        }
        mainLayer.bind(controls.get(2), remotes.getParameter(4));
        mainLayer.bind(controls.get(3), remotes.getParameter(5));
        mainLayer.bind(controls.get(6), remotes.getParameter(6));
        mainLayer.bind(controls.get(7), remotes.getParameter(7));
    }
    
    private void clearTexts() {
        for (int i = 0; i < 14; i++) {
            midiProcessor.sendText(1, i, "");
        }
        midiProcessor.sendText(0, 0, "");
        midiProcessor.sendText(0, 1, "");
    }
    
    private static int fourBy2Mapping(final int index) {
        return (index % 2) * 4 + (index / 2);
    }
    
    @Override
    public void exit() {
        
    }
    
    @Override
    public void flush() {
        surface.updateHardware();
    }
    
}
