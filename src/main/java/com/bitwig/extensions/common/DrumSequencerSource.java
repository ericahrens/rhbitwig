package com.bitwig.extensions.common;

import com.bitwig.extensions.context.CrossControlListenerHandler;

public interface DrumSequencerSource extends CrossControlListenerHandler {

    public interface ChangeListener {
        void changePadsOffset(int bankOffset, int padOffset, int bankSize);

        void changeGridResolution(double resolution);
    }

    int getRefVelocity();

    double getGatePercent();

    double getGridResolution();

    int getNoteOffset();

    void registerListener(String id, ChangeListener listener);

}
