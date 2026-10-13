package com.fuckbaiduinput;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class PreferenceCommitSessionTest {
    @Test
    public void pendingSubmissionRejectsDuplicateSwitchEvents() {
        PreferenceCommitSession session = new PreferenceCommitSession();
        long token = session.begin();
        assertNotEquals(0L, token);
        assertEquals(0L, session.begin());
        assertTrue(session.complete(token, true));
        assertNotEquals(0L, session.begin());
    }

    @Test
    public void staleCompletionCannotReleaseANewerSubmission() {
        PreferenceCommitSession session = new PreferenceCommitSession();
        long first = session.begin();
        assertTrue(session.complete(first, true));
        long second = session.begin();
        assertFalse(session.complete(first, true));
        assertEquals(0L, session.begin());
        assertTrue(session.complete(second, true));
    }

    @Test
    public void failedOrRejectedSchedulingCanReleasePendingForRetry() {
        PreferenceCommitSession session = new PreferenceCommitSession();
        long failed = session.begin();
        assertTrue(session.complete(failed, true));
        assertFalse(session.complete(failed, true));
        assertNotEquals(0L, session.begin());
    }

    @Test
    public void destroyedActivityIgnoresResultAndClosesItsPage() {
        PreferenceCommitSession session = new PreferenceCommitSession();
        long token = session.begin();
        assertFalse(session.complete(token, false));
        assertEquals(0L, session.begin());
        assertFalse(session.complete(token, true));
    }
}
