package com.matriz.tasks;

import java.time.LocalDate;

/** Data of a task to insert; the values have been validated. The status starts as "To do". */
public record NewTask(long projectId, Long parentTaskId, String title, String description, LocalDate deadline,
        int urgency, int importance) {
}
