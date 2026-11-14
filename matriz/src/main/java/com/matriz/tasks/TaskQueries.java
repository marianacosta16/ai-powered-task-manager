package com.matriz.tasks;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

/** Reads tasks that are not deleted (R9, M4). Who may see them is decided elsewhere. */
@Repository
public class TaskQueries {

    private static final String COLUMNS = """
            id, project_id, parent_task_id, original_task_id, title, description, status, deadline,
            urgency, importance, block_reason, version
            """;
    private static final String FINAL = "('Done', 'Cancelled', 'Duplicate')";

    private final JdbcTemplate jdbc;
    private final NamedParameterJdbcTemplate named;

    public TaskQueries(JdbcTemplate jdbc, NamedParameterJdbcTemplate named) {
        this.jdbc = jdbc;
        this.named = named;
    }

    public Optional<TaskRecord> findById(long id) {
        return jdbc.query("SELECT " + COLUMNS + " FROM task WHERE id = ? AND deleted_at IS NULL",
                TaskQueries::map, id).stream().findFirst();
    }

    /**
     * Reads the task and locks its row until the end of the transaction. Whoever changes a task, or
     * something that depends on it (its subtasks, its assignees), takes the lock first, so two
     * requests about the same task are done one after the other (R4, R26, T13).
     */
    public Optional<TaskRecord> findByIdForUpdate(long id) {
        return jdbc.query("SELECT " + COLUMNS + " FROM task WHERE id = ? AND deleted_at IS NULL FOR UPDATE",
                TaskQueries::map, id).stream().findFirst();
    }

    /**
     * The open tasks and subtasks of a person in a project (R17, P24), by id, with their rows locked
     * until the end of the transaction. The locks follow the order people first, then tasks.
     */
    public List<Long> lockOpenTaskIdsOf(long projectId, long userId) {
        return jdbc.queryForList("""
                SELECT t.id FROM task t JOIN task_assignee a ON a.task_id = t.id
                WHERE t.project_id = ? AND a.user_id = ? AND t.deleted_at IS NULL AND t.status NOT IN """ + FINAL + """

                ORDER BY t.id FOR UPDATE OF t
                """, Long.class, projectId, userId);
    }

    /** As lockOpenTaskIdsOf, without the locks: for checks that run before anything is locked. */
    public List<Long> openTaskIdsOf(long projectId, long userId) {
        return jdbc.queryForList("""
                SELECT t.id FROM task t JOIN task_assignee a ON a.task_id = t.id
                WHERE t.project_id = ? AND a.user_id = ? AND t.deleted_at IS NULL AND t.status NOT IN """ + FINAL + """

                ORDER BY t.id
                """, Long.class, projectId, userId);
    }

    public List<TaskRecord> subtasksOf(long parentId) {
        return jdbc.query("SELECT " + COLUMNS + " FROM task WHERE parent_task_id = ? AND deleted_at IS NULL ORDER BY id",
                TaskQueries::map, parentId);
    }

    /** R4: a parent task cannot be completed or cancelled while one of its subtasks is open. */
    public boolean hasOpenSubtasks(long parentId) {
        return Boolean.TRUE.equals(jdbc.queryForObject("SELECT EXISTS (SELECT 1 FROM task WHERE parent_task_id = ? "
                + "AND deleted_at IS NULL AND status NOT IN " + FINAL + ")", Boolean.class, parentId));
    }

    /** R3: the latest deadline among the subtasks of a task. */
    public Optional<LocalDate> latestSubtaskDeadline(long parentId) {
        return Optional.ofNullable(jdbc.queryForObject(
                "SELECT max(deadline) FROM task WHERE parent_task_id = ? AND deleted_at IS NULL", LocalDate.class,
                parentId));
    }

    public List<Long> assigneeIds(long taskId) {
        return jdbc.queryForList("SELECT user_id FROM task_assignee WHERE task_id = ? ORDER BY user_id", Long.class,
                taskId);
    }

    public Map<Long, List<Long>> assigneeIdsByTask(Collection<Long> taskIds) {
        Map<Long, List<Long>> result = new HashMap<>();
        if (taskIds.isEmpty()) {
            return result;
        }
        named.query("SELECT task_id, user_id FROM task_assignee WHERE task_id IN (:ids) ORDER BY user_id",
                new MapSqlParameterSource("ids", taskIds), rs -> {
                    result.computeIfAbsent(rs.getLong("task_id"), k -> new java.util.ArrayList<>())
                            .add(rs.getLong("user_id"));
                });
        return result;
    }

    private static TaskRecord map(ResultSet rs, int row) throws SQLException {
        return new TaskRecord(rs.getLong("id"), rs.getLong("project_id"), (Long) rs.getObject("parent_task_id"),
                (Long) rs.getObject("original_task_id"), rs.getString("title"), rs.getString("description"),
                rs.getString("status"), rs.getObject("deadline", LocalDate.class), rs.getInt("urgency"),
                rs.getInt("importance"), rs.getString("block_reason"), rs.getInt("version"));
    }
}
