package com.rjs.fsm.job;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class JobStatusTest {

    @Test
    void openCanTransitionToAssigned() {
        assertTrue(JobStatus.OPEN.canTransitionTo(JobStatus.ASSIGNED));
    }

    @Test
    void openCannotTransitionToInProgress() {
        assertFalse(JobStatus.OPEN.canTransitionTo(JobStatus.IN_PROGRESS));
    }

    @Test
    void assignedCanTransitionToInProgress() {
        assertTrue(JobStatus.ASSIGNED.canTransitionTo(JobStatus.IN_PROGRESS));
    }

    @Test
    void inProgressCanTransitionToDone() {
        assertTrue(JobStatus.IN_PROGRESS.canTransitionTo(JobStatus.DONE));
    }

    @Test
    void inProgressCanTransitionToNeedFollowup() {
        assertTrue(JobStatus.IN_PROGRESS.canTransitionTo(JobStatus.NEED_FOLLOWUP));
    }

    @Test
    void doneCanTransitionToClosed() {
        assertTrue(JobStatus.DONE.canTransitionTo(JobStatus.CLOSED));
    }

    @Test
    void closedHasNoTransitions() {
        for (JobStatus s : JobStatus.values()) {
            assertFalse(JobStatus.CLOSED.canTransitionTo(s),
                    "CLOSED should not transition to " + s);
        }
    }

    @Test
    void needFollowupCanTransitionToAssigned() {
        assertTrue(JobStatus.NEED_FOLLOWUP.canTransitionTo(JobStatus.ASSIGNED));
    }

    @Test
    void assignedInProgressIsTechOnly() {
        assertTrue(JobStatus.ASSIGNED.isTechOnlyTransition(JobStatus.IN_PROGRESS));
        assertFalse(JobStatus.ASSIGNED.isAdminOnlyTransition(JobStatus.IN_PROGRESS));
    }

    @Test
    void doneToClosedIsAdminOnly() {
        assertTrue(JobStatus.DONE.isAdminOnlyTransition(JobStatus.CLOSED));
        assertFalse(JobStatus.DONE.isTechOnlyTransition(JobStatus.CLOSED));
    }
}
