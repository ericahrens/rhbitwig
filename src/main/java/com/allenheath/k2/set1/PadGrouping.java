package com.allenheath.k2.set1;

import java.util.Arrays;
import java.util.List;

public class PadGrouping {
    private final PadAssignment[] assignmentList = new PadAssignment[8];
    private final int[] padToSlotIndex = new int[16];

    public PadGrouping() {
        for (int i = 0; i < assignmentList.length; i++) {
            assignmentList[i] = new PadAssignment(i);
        }
    }

    public PadAssignment getAssignment(int i) {
        return assignmentList[i];
    }

    public void assign(int slotIndex, final String assignValues, final List<PadContainer> drumPadsList) {
        assignmentList[slotIndex].assign(assignValues, drumPadsList);
        Arrays.fill(padToSlotIndex, -1);
        for (int i = 0; i < assignmentList.length; i++) {
            PadAssignment assignment = assignmentList[i];
            for (PadContainer container : assignment.getPads()) {
                padToSlotIndex[container.getIndex()] = i;
            }
        }
        AllenHeathK2ControllerExtension.println(" ==> %s", Arrays.toString(padToSlotIndex));
    }

    public int padIndexToSlot(int padIndex) {
        if (padIndex >= 0 && padIndex < 16) {
            return padToSlotIndex[padIndex];
        }
        return -1;
    }
}
