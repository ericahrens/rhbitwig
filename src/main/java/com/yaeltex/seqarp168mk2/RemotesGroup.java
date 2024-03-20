package com.yaeltex.seqarp168mk2;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import com.bitwig.extension.controller.api.CursorRemoteControlsPage;
import com.bitwig.extension.controller.api.SettableIntegerValue;

public class RemotesGroup {
    
    private final List<CursorRemoteControlsPage> remotes = new ArrayList<>();
    private final String name;
    private int pages = 0;
    
    public RemotesGroup(final String name, final Function<Integer, CursorRemoteControlsPage> creator, final int pages) {
        this.name = name;
        for (int i = 0; i < pages; i++) {
            final int index = i;
            final CursorRemoteControlsPage remote = creator.apply(i);
            remote.selectedPageIndex().markInterested();
            if (i == 0) {
                final SettableIntegerValue pageIndex = remote.selectedPageIndex();
                pageIndex.addValueObserver(this::changeFromFirstIndex);
                remote.pageCount().addValueObserver(pagesChanged -> {
                    this.pages = pages;
                    updatePages();
                });
            }
            remotes.add(remote);
        }
    }
    
    private void updatePages() {
        final int page1Index = remotes.get(0).selectedPageIndex().get();
        changeFromFirstIndex(page1Index);
    }
    
    private void changeFromFirstIndex(final int index) {
        for (int i = 1; i < remotes.size(); i++) {
            remotes.get(i).selectedPageIndex().set(i + index);
        }
    }
    
    public CursorRemoteControlsPage getRemotes(final int index) {
        return remotes.get(index);
    }
    
}
