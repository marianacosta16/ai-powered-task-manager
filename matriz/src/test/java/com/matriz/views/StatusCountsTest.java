package com.matriz.views;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;

import org.junit.jupiter.api.Test;

import com.matriz.tasks.TaskStatus;

/** Every status of a task is counted: a new or renamed status cannot silently count as 0. */
class StatusCountsTest {

    @Test
    void everyStatusIsCountedInExactlyOneField() {
        for (TaskStatus status : TaskStatus.values()) {
            StatusCounts counts = StatusCounts.of(Map.of(status.label(), 7));

            int total = counts.toDo() + counts.inProgress() + counts.inReview() + counts.blocked() + counts.done()
                    + counts.cancelled() + counts.duplicate();
            assertThat(total).as(status.label()).isEqualTo(7);
        }
    }

    @Test
    void aMissingStatusCountsZero() {
        assertThat(StatusCounts.of(Map.of())).isEqualTo(new StatusCounts(0, 0, 0, 0, 0, 0, 0));
    }
}
