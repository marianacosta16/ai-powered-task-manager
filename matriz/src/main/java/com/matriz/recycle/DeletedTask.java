package com.matriz.recycle;

import java.time.Instant;
import java.time.LocalDate;

/**
 * A task or subtask in the recycle bin, with what is needed to decide who sees it and whether it can come
 * back: its project (and whether that is in the bin too) and, for a subtask, its parent.
 *
 * @param projectDeletedAt null when the project is not in the recycle bin
 * @param parentId         null for a main task
 * @param parentDeletedAt  null when the parent is not in the recycle bin
 */
public record DeletedTask(long id, long projectId, String projectName, long managerId, String projectStatus,
        Instant projectDeletedAt, Long parentId, String parentStatus, LocalDate parentDeadline, Instant parentDeletedAt,
        String title, String status, LocalDate deadline, Instant deletedAt) {

    public boolean isSubtask() {
        return parentId != null;
    }
}
