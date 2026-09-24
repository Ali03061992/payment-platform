package com.paymentplatform.organization.domain.model;

import com.paymentplatform.shared.domain.exception.ConflictException;

import java.util.EnumSet;
import java.util.Set;

public enum DisputeStatus {

    OPEN, IN_PROGRESS, RESOLVED, CLOSED;

    private static final java.util.Map<DisputeStatus, Set<DisputeStatus>> TRANSITIONS =
            new java.util.EnumMap<>(DisputeStatus.class);

    static {
        TRANSITIONS.put(OPEN, EnumSet.of(IN_PROGRESS, CLOSED));
        TRANSITIONS.put(IN_PROGRESS, EnumSet.of(RESOLVED, CLOSED));
        TRANSITIONS.put(RESOLVED, EnumSet.of(CLOSED));
        TRANSITIONS.put(CLOSED, EnumSet.noneOf(DisputeStatus.class));
    }

    public boolean isTerminal() {
        return this == CLOSED;
    }

    public void assertCanTransitionTo(DisputeStatus target) {
        Set<DisputeStatus> allowed = TRANSITIONS.getOrDefault(this, EnumSet.noneOf(DisputeStatus.class));
        if (!allowed.contains(target)) {
            throw new ConflictException("Transition de litige invalide : " + this + " -> " + target);
        }
    }
}
