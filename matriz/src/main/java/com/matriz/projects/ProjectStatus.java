package com.matriz.projects;

import com.matriz.shared.InvalidRequestException;
import com.matriz.shared.Labelled;

/**
 * Status set by the manager. Attention and Late are calculated and never set (R12, P5), so they
 * are not accepted here.
 */
public enum ProjectStatus implements Labelled {
    NOT_STARTED("Not started"),
    IN_PROGRESS("In progress"),
    SUSPENDED("Suspended"),
    COMPLETED("Completed");

    private final String label;

    ProjectStatus(String label) {
        this.label = label;
    }

    @Override
    public String label() {
        return label;
    }

    /** An open project is not completed and not suspended (P4). */
    public boolean isOpen() {
        return this == NOT_STARTED || this == IN_PROGRESS;
    }

    public static ProjectStatus fromLabel(String field, String label) {
        return Labelled.find(ProjectStatus.class, label).orElseThrow(
                () -> new InvalidRequestException("The status is not valid.", field,
                        "Must be one of: Not started, In progress, Suspended, Completed."));
    }
}
