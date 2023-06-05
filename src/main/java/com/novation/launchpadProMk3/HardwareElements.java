package com.novation.launchpadProMk3;

import com.bitwig.extension.api.opensoundcontrol.OscConnection;
import com.bitwig.extension.controller.api.HardwareSurface;
import com.bitwig.extension.controller.api.MidiIn;
import com.bitwig.extension.controller.api.MidiOut;

import java.util.*;

public class HardwareElements {
    private final GridButton[][] gridButtons = new GridButton[8][8];
    private final List<LabeledButton> sceneLaunchButtons = new ArrayList<>();
    private final List<LabeledButton> trackSelectButtons = new ArrayList<>();
    private Map<LabelCcAssignments, LabeledButton> labeledButtons = new HashMap<>();

    public HardwareElements(HardwareSurface surface, OscConnection gridOSCconnection, MidiIn midiIn, MidiOut midiOut) {
        for (int row = 0; row < 8; row++) {
            for (int col = 0; col < 8; col++) {
                gridButtons[row][col] = new GridButton(surface, midiIn, midiOut, row, col, gridOSCconnection);
            }
        }
        for (int i = 0; i < 8; i++) {
            final LabeledButton sceneButton = new LabeledButton("SCENE_LAUNCH_" + (i + 1), surface, midiIn, midiOut,
                    LabelCcAssignments.R8_PRINT_TO_CLIP.getCcValue() + (7 - i) * 10);
            sceneLaunchButtons.add(sceneButton);

            final LabeledButton trackButton = new LabeledButton("TRACK_" + (i + 1), surface, midiIn, midiOut,
                    LabelCcAssignments.TRACK_SEL_1.getCcValue() + i);
            trackSelectButtons.add(trackButton);
        }
        Arrays.stream(LabelCcAssignments.values())
                .filter(LabelCcAssignments::isDedicated)
                .forEach(labelCcAssignments -> labeledButtons.put(labelCcAssignments,
                        new LabeledButton(surface, midiIn, midiOut, labelCcAssignments)));
    }

    public LabeledButton getButton(LabelCcAssignments key) {
        return labeledButtons.get(key);
    }

    public List<LabeledButton> getSceneLaunchButtons() {
        return sceneLaunchButtons;
    }

    public List<LabeledButton> getTrackSelectButtons() {
        return trackSelectButtons;
    }

    public GridButton getGridButton(int row, int col) {
        return gridButtons[row][col];
    }
}
