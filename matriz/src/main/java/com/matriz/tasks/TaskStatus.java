package com.matriz.tasks;

import com.matriz.shared.InvalidRequestException;
import com.matriz.shared.Labelled;

/** Status of a task or subtask. Done, Cancelled and Duplicate are final (a closed task). */
public enum TaskStatus implements Labelled {
    TO_DO("To do", false),
    IN_PROGRESS("In progress", false),
    IN_REVIEW("In review", false),
    BLOCKED("Blocked", false),
    DONE("Done", true),
    CANCELLED("Cancelled", true),
    DUPLICATE("Duplicate", true);

    private final String label;
    private final boolean closed;

    TaskStatus(String label, boolean closed) {
        this.label = label;
        this.closed = closed;
    }

    @Override
    public String label() {
        return label;
    }

    /** Final status: the task counts as closed (R5). */
    public boolean isFinal() {
        return closed;
    }

    public static TaskStatus fromLabel(String field, String label) {
        return Labelled.find(TaskStatus.class, label).orElseThrow(
                () -> new InvalidRequestException("The status is not valid.", field,
                        "Must be one of: To do, In progress, In review, Blocked, Done, Cancelled, Duplicate."));
    }

    /** Reads a status that is stored; an unknown value is a data problem, not a bad request. */
    public static TaskStatus ofStored(String label) {
        return Labelled.find(TaskStatus.class, label)
                .orElseThrow(() -> new IllegalStateException("Unknown task status in the database: " + label));
    }
}
