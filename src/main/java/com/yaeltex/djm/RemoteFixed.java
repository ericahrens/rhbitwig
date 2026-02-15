package com.yaeltex.djm;

import com.bitwig.extension.controller.api.CursorRemoteControlsPage;
import com.bitwig.extension.controller.api.RemoteControl;
import com.bitwig.extension.controller.api.Track;

class RemoteFixed {
    private final CursorRemoteControlsPage remote;
    //private final int index;
    private int position;
    private String[] pageNames = new String[0];
    private final String name;
    
    public RemoteFixed(final int index, final String name, final Track sourceTrack) {
        //this.index = index;
        this.name = name;
        remote = sourceTrack.createCursorRemoteControlsPage(name, 8, null);
        remote.pageNames().addValueObserver(this::handlePageNames);
        remote.pageCount().addValueObserver(this::handlePageCount);
        remote.selectedPageIndex().addValueObserver(this::handlePosition);
    }
    
    private void handlePosition(final int position) {
        this.position = position;
    }
    
    private void handlePageCount(final int count) {
        //            if (this.index < count && this.position != this.index) {
        //                DjmAControllerExtension.println(" Fixed to %d", this.name, this.index);
        //                remote.selectedPageIndex().set(this.index);
        //            }
    }
    
    public RemoteControl getParameter(final int index) {
        return remote.getParameter(index);
    }
    
    private void handlePageNames(final String[] names) {
        this.pageNames = names;
        final int index = determineIndex();
        if (index != -1) {
            this.position = index;
            remote.selectedPageIndex().set(this.position);
            //DjmControllerExtension.println(" Placed Page <%s>  %d", this.name, position);
        }
    }
    
    private int determineIndex() {
        for (int i = 0; i < this.pageNames.length; i++) {
            if (this.pageNames[i].equals(this.name)) {
                return i;
            }
        }
        return -1;
    }
    
    public CursorRemoteControlsPage getRemote() {
        return remote;
    }
}
