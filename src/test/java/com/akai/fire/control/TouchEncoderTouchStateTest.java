package com.akai.fire.control;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.akai.fire.sequence.EncoderTouchTracker;

class TouchEncoderTouchStateTest {

    @Test
    void touchStateShouldStayLatchedUntilLastRelease() {
        final EncoderTouchTracker tracker = new EncoderTouchTracker();

        assertTrue(tracker.getCurrentActiveEncoder() < 0);
        assertTrue(tracker.beginTouch(1, "Send 1"));
        assertTrue(tracker.beginTouch(2, "Send 2"));
        assertTrue(tracker.isAnyTouchActive());
        assertEquals(2, tracker.getCurrentActiveEncoder());

        assertTrue(tracker.endTouch(1));
        assertTrue(tracker.isAnyTouchActive());
        assertEquals(2, tracker.getCurrentActiveEncoder());

        assertFalse(tracker.endTouch(2));
        assertFalse(tracker.isAnyTouchActive());
    }
}
