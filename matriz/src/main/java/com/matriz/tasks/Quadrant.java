package com.matriz.tasks;

import com.matriz.shared.InvalidRequestException;
import com.matriz.shared.Labelled;

/**
 * Quadrant of the Matrix, calculated from urgency and importance (F04, R25): the levels 1 and 2 are
 * low and the levels 3 and 4 are high. It is never stored. The Matrix view and the "priority" filter
 * of the list use this one definition.
 */
public enum Quadrant implements Labelled {
    DO_FIRST("Do first", true, true),
    SCHEDULE("Schedule", false, true),
    DELEGATE("Delegate", true, false),
    LATER("Later", false, false);

    /** The lowest level that counts as high. */
    public static final int HIGH_LEVEL = 3;

    private final String label;
    private final boolean urgent;
    private final boolean important;

    Quadrant(String label, boolean urgent, boolean important) {
        this.label = label;
        this.urgent = urgent;
        this.important = important;
    }

    @Override
    public String label() {
        return label;
    }

    /** True when the quadrant is for high urgency. */
    public boolean urgent() {
        return urgent;
    }

    /** True when the quadrant is for high importance. */
    public boolean important() {
        return important;
    }

    public static Quadrant of(int urgency, int importance) {
        boolean urgent = urgency >= HIGH_LEVEL;
        boolean important = importance >= HIGH_LEVEL;
        for (Quadrant q : values()) {
            if (q.urgent == urgent && q.important == important) {
                return q;
            }
        }
        throw new IllegalStateException("Unreachable");
    }

    public static Quadrant fromLabel(String field, String label) {
        return Labelled.find(Quadrant.class, label).orElseThrow(() -> new InvalidRequestException(
                "The quadrant is not valid.", field, "Must be one of: Do first, Schedule, Delegate, Later."));
    }
}
