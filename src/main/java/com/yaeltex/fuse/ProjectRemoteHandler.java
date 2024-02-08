package com.yaeltex.fuse;

import java.util.List;

import com.bitwig.extension.controller.api.CursorRemoteControlsPage;
import com.bitwig.extension.controller.api.HardwareSlider;
import com.bitwig.extension.controller.api.RemoteControl;
import com.bitwig.extension.controller.api.Track;
import com.bitwig.extensions.framework.Layer;
import com.bitwig.extensions.framework.di.Component;
import com.yaeltex.common.YaelTexColors;
import com.yaeltex.common.YaeltexButtonLedState;
import com.yaeltex.controls.RgbButton;

@Component
public class ProjectRemoteHandler {
    
    private final YaelTexColors[] MASTER_BUTTON_COLORS = {
        YaelTexColors.DODGER_BLUE,
        YaelTexColors.YELLOW,
        YaelTexColors.WHITE,
        YaelTexColors.AQUA,
        YaelTexColors.GREEN,
        YaelTexColors.DARK_ORANGE,
        YaelTexColors.WHITE,
        YaelTexColors.WHITE
    };
    private final YaelTexColors[] FX_BUTTON_COLORS = {
        YaelTexColors.WHITE, YaelTexColors.AQUA, YaelTexColors.GREEN, YaelTexColors.DARK_ORANGE
    };
    private final CursorRemoteControlsPage mainRemotes1;
    private final CursorRemoteControlsPage mainRemotes2;
    private final RemoteTarget faderRemotes;
    private final RemoteTarget muteRemotes;
    
    private static class RemoteTarget {
        private String[] pages;
        private final String pageName;
        CursorRemoteControlsPage remotes;
        
        public RemoteTarget(final String pageName, final Track track) {
            this.pageName = pageName;
            remotes = track.createCursorRemoteControlsPage("P_" + pageName, 8, null);
            remotes.pageNames().addValueObserver(pages -> handlePagesUpdate(pages));
            remotes.selectedPageIndex().markInterested();
        }
        
        private void handlePagesUpdate(final String[] pages) {
            this.pages = pages;
            final int index = getIndexOfTarget();
            if (index != -1) {
                FuseExtension.println(" SELECTED %s", pageName);
                remotes.selectedPageIndex().set(index);
            }
        }
        
        private int getIndexOfTarget() {
            if (this.pages == null || this.pages.length == 0) {
                return -1;
            }
            for (int i = 0; i < pages.length; i++) {
                if (pages[i].equals(pageName)) {
                    return i;
                }
            }
            return -1;
        }
        
        public CursorRemoteControlsPage getRemotes() {
            return remotes;
        }
    }
    
    public ProjectRemoteHandler(final BitwigControl control) {
        final Track rootTrack = control.getRootTrack();
        mainRemotes1 = rootTrack.createCursorRemoteControlsPage("project-remotes-1", 8, null);
        mainRemotes2 = rootTrack.createCursorRemoteControlsPage("project-remotes-2", 8, null);
        mainRemotes2.selectedPageIndex().markInterested();
        mainRemotes2.pageCount().addValueObserver(pages -> {
            if (pages > 1) {
                mainRemotes2.selectedPageIndex().set(1);
            }
        });
        faderRemotes = new RemoteTarget("FADER REMOTES", rootTrack);
        muteRemotes = new RemoteTarget("MUTE REMOTES", rootTrack);
    }
    
    public void bindMasterControls(final HwElements hwElements, final Layer layer) {
        final HardwareSlider masterFilter = hwElements.getMasterSlider();
        for (int i = 0; i < 4; i++) {
            final RgbButton button = hwElements.getFxMainButton(i < 2 ? i + 2 : i + 4);
            final RemoteControl parameter = mainRemotes1.getParameter(i);
            button.bindToggleValue(layer, parameter, YaeltexButtonLedState.of(FX_BUTTON_COLORS[i]));
        }
        layer.bind(masterFilter, mainRemotes1.getParameter(4));
        
        for (int i = 0; i < 8; i++) {
            final RgbButton button = hwElements.getMasterButton(i);
            final RemoteControl parameter = mainRemotes2.getParameter(i);
            button.bindToggleValue(layer, parameter, YaeltexButtonLedState.of(MASTER_BUTTON_COLORS[i]));
        }
        
        final List<StripControl> stripControls = hwElements.getStripControls();
        for (int i = 0; i < 6; i++) {
            final StripControl stripControl = stripControls.get(i);
            layer.bind(stripControl.mainFader(), faderRemotes.getRemotes().getParameter(i));
            stripControl.muteButton().bindToggleValue(layer, muteRemotes.getRemotes().getParameter(i));
        }
    }
    
}
