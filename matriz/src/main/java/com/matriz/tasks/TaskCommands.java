package com.matriz.tasks;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/** Writes tasks and their assignees. The rules are checked by the callers (use cases). */
@Repository
public class TaskCommands {

    private final JdbcTemplate jdbc;

    public TaskCommands(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public long insert(NewTask t) {
        return jdbc.queryForObject("""
                INSERT INTO task (project_id, parent_task_id, title, description, status, deadline, urgency, importance)
                VALUES (?, ?, ?, ?, 'To do', ?, ?, ?) RETURNING id
                """, Long.class, t.projectId(), t.parentTaskId(), t.title(), t.description(), t.deadline(),
                t.urgency(), t.importance());
    }

    /** Saves the data of the task and goes up one version, only if the stored version is still the expected one. */
    public boolean update(TaskRecord t, int expectedVersion) {
        return jdbc.update("""
                UPDATE task SET title = ?, description = ?, status = ?, deadline = ?, urgency = ?, importance = ?,
                                block_reason = ?, original_task_id = ?, version = version + 1
                WHERE id = ? AND version = ? AND deleted_at IS NULL
                """, t.title(), t.description(), t.status(), t.deadline(), t.urgency(), t.importance(),
                t.blockReason(), t.originalTaskId(), t.id(), expectedVersion) == 1;
    }

    public void addAssignee(long taskId, long userId) {
        jdbc.update("INSERT INTO task_assignee (task_id, user_id) VALUES (?, ?) ON CONFLICT DO NOTHING", taskId, userId);
    }

    public boolean removeAssignee(long taskId, long userId) {
        return jdbc.update("DELETE FROM task_assignee WHERE task_id = ? AND user_id = ?", taskId, userId) == 1;
    }
}
