package com.paymentplatform.organization.domain.model;

import com.paymentplatform.shared.domain.exception.ConflictException;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

public enum OrderStatus {

    DRAFT, CONFIRMED, PREPARING, READY_FOR_DELIVERY, DELIVERY_ACCEPTED, DELIVERY_REJECTED, IN_DELIVERY, DELIVERED, ACCEPTED, CANCELLED, REJECTED;

    private static final Map<OrderStatus, Set<OrderStatus>> TRANSITIONS = new EnumMap<>(OrderStatus.class);

    static {
        TRANSITIONS.put(DRAFT, EnumSet.of(CONFIRMED, CANCELLED));
        TRANSITIONS.put(CONFIRMED, EnumSet.of(PREPARING, CANCELLED));
        TRANSITIONS.put(PREPARING, EnumSet.of(READY_FOR_DELIVERY, CANCELLED));
        TRANSITIONS.put(READY_FOR_DELIVERY, EnumSet.of(DELIVERY_ACCEPTED, DELIVERY_REJECTED));
        TRANSITIONS.put(DELIVERY_ACCEPTED, EnumSet.of(IN_DELIVERY));
        TRANSITIONS.put(IN_DELIVERY, EnumSet.of(DELIVERED, DELIVERY_REJECTED));
        TRANSITIONS.put(DELIVERED, EnumSet.of(ACCEPTED, REJECTED));
        TRANSITIONS.put(DELIVERY_REJECTED, EnumSet.of(CONFIRMED, CANCELLED));
        TRANSITIONS.put(CANCELLED, EnumSet.noneOf(OrderStatus.class));
        TRANSITIONS.put(REJECTED, EnumSet.noneOf(OrderStatus.class));
        TRANSITIONS.put(ACCEPTED, EnumSet.noneOf(OrderStatus.class));
    }

    public boolean isTerminal() {
        return this == CANCELLED || this == REJECTED;
    }

    public boolean canCancel() {
        return this == DRAFT || this == CONFIRMED || this == PREPARING;
    }

    public void assertCanTransitionTo(OrderStatus target) {
        Set<OrderStatus> allowed = TRANSITIONS.getOrDefault(this, EnumSet.noneOf(OrderStatus.class));
        if (!allowed.contains(target)) {
            throw new ConflictException(
                    "Transition invalide : " + this + " -> " + target);
        }
    }
}
