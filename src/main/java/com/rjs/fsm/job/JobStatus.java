package com.rjs.fsm.job;

import java.util.Map;
import java.util.Set;

public enum JobStatus {
    OPEN,
    ASSIGNED,
    IN_PROGRESS,
    DONE,
    NEED_FOLLOWUP,
    CLOSED,
    CANCELLED;

    /**
     * FSM transition rules.
     * Key = current status, Value = set of allowed next statuses.
     */
    private static final Map<JobStatus, Set<JobStatus>> TRANSITIONS = Map.of(
            OPEN,           Set.of(ASSIGNED),
            ASSIGNED,       Set.of(IN_PROGRESS),
            IN_PROGRESS,    Set.of(DONE, NEED_FOLLOWUP),
            DONE,           Set.of(CLOSED),
            NEED_FOLLOWUP,  Set.of(ASSIGNED)
    );

    /**
     * Transitions that only ADMIN can perform.
     */
    private static final Set<String> ADMIN_ONLY = Set.of(
            "DONE->CLOSED",
            "NEED_FOLLOWUP->ASSIGNED"
    );

    /**
     * Transitions that only TECHNICIAN can perform.
     */
    private static final Set<String> TECH_ONLY = Set.of(
            "ASSIGNED->IN_PROGRESS",
            "IN_PROGRESS->DONE",
            "IN_PROGRESS->NEED_FOLLOWUP"
    );

    public boolean canTransitionTo(JobStatus target) {
        Set<JobStatus> allowed = TRANSITIONS.get(this);
        return allowed != null && allowed.contains(target);
    }

    public boolean isAdminOnlyTransition(JobStatus target) {
        return ADMIN_ONLY.contains(this.name() + "->" + target.name());
    }

    public boolean isTechOnlyTransition(JobStatus target) {
        return TECH_ONLY.contains(this.name() + "->" + target.name());
    }
}
