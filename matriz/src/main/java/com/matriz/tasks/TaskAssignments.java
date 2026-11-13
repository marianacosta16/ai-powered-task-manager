package com.matriz.tasks;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/** Whether a person is responsible for tasks of a project, open or closed, not deleted. */
@Repository
public class TaskAssignments {

    private final JdbcTemplate jdbc;

    public TaskAssignments(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public boolean hasTasks(long projectId, long userId) {
        return Boolean.TRUE.equals(jdbc.queryForObject("""
                SELECT EXISTS (SELECT 1 FROM task t JOIN task_assignee a ON a.task_id = t.id
                               WHERE t.project_id = ? AND a.user_id = ? AND t.deleted_at IS NULL)
                """, Boolean.class, projectId, userId));
    }
}
