package com.matriz.views;

import java.util.Map;

/** Number of main tasks in each status (R18): the StatusCounts of the contract. */
public record StatusCounts(int toDo, int inProgress, int inReview, int blocked, int done, int cancelled, int duplicate) {

    /** From the number of tasks by status label; a status that is missing counts 0. */
    public static StatusCounts of(Map<String, Integer> byStatus) {
        return new StatusCounts(byStatus.getOrDefault("To do", 0), byStatus.getOrDefault("In progress", 0),
                byStatus.getOrDefault("In review", 0), byStatus.getOrDefault("Blocked", 0),
                byStatus.getOrDefault("Done", 0), byStatus.getOrDefault("Cancelled", 0),
                byStatus.getOrDefault("Duplicate", 0));
    }

    /** The tasks that count for the progress: all but the cancelled and the duplicates (P7). */
    public int countable() {
        return toDo + inProgress + inReview + blocked + done;
    }
}
