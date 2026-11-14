package com.matriz.views;

import java.util.Map;

import com.matriz.tasks.TaskStatus;

/** Number of main tasks in each status (R18): the StatusCounts of the contract. */
public record StatusCounts(int toDo, int inProgress, int inReview, int blocked, int done, int cancelled, int duplicate) {

    /** From the number of tasks by status label; a status that is missing counts 0. */
    public static StatusCounts of(Map<String, Integer> byStatus) {
        return new StatusCounts(count(byStatus, TaskStatus.TO_DO), count(byStatus, TaskStatus.IN_PROGRESS),
                count(byStatus, TaskStatus.IN_REVIEW), count(byStatus, TaskStatus.BLOCKED), count(byStatus, TaskStatus.DONE),
                count(byStatus, TaskStatus.CANCELLED), count(byStatus, TaskStatus.DUPLICATE));
    }

    private static int count(Map<String, Integer> byStatus, TaskStatus status) {
        return byStatus.getOrDefault(status.label(), 0);
    }

    /** The tasks that count for the progress: all but the cancelled and the duplicates (P7). */
    public int countable() {
        return toDo + inProgress + inReview + blocked + done;
    }
}
