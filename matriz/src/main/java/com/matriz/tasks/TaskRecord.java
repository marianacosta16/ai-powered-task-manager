package com.matriz.tasks;

import java.time.LocalDate;

/** A task or a subtask as stored (docs/database.md, TASK). A subtask has a parent (M1). */
public record TaskRecord(long id, long projectId, Long parentTaskId, Long originalTaskId, String title,
        String description, String status, LocalDate deadline, int urgency, int importance, String blockReason,
        int version) {

    public boolean isSubtask() {
        return parentTaskId != null;
    }

    public TaskRecord withVersion(int newVersion) {
        return new TaskRecord(id, projectId, parentTaskId, originalTaskId, title, description, status, deadline,
                urgency, importance, blockReason, newVersion);
    }

    public TaskStatus statusValue() {
        return TaskStatus.ofStored(status);
    }
}
