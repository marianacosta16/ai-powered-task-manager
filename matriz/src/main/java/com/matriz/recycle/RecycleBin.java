package com.matriz.recycle;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

/**
 * The recycle bin (F09, R9, R20, M4): it has no table of its own, only the mark {@code deleted_at} of projects
 * and tasks. Everything deleted in one action gets the same instant, and that instant is what ties an item
 * to what was deleted with it: restoring brings back only what has the same instant (R9). The rules (who may,
 * what state) are checked by the use cases before they call this class.
 *
 * <p>An item stays {@link #RETENTION 30 days} (R20): it is in the bin while {@code deleted_at} is after
 * {@code now - 30 days}, so at exactly 30 days it is gone (A39). The reads and the restores use this cut-off
 * themselves, so they do not depend on {@link #purgeExpired} having run.
 */
@Repository
public class RecycleBin {

    public static final Duration RETENTION = Duration.ofDays(30);

    private static final String PROJECT_COLUMNS = "p.id, p.name, p.manager_id, p.deleted_at";
    private static final String TASK_SELECT = """
            SELECT t.id, t.project_id, p.name AS project_name, p.manager_id, p.status AS project_status,
                   p.deleted_at AS project_deleted_at, t.parent_task_id, par.status AS parent_status,
                   par.deadline AS parent_deadline, par.deleted_at AS parent_deleted_at,
                   t.title, t.status, t.deadline, t.deleted_at
            FROM task t JOIN project p ON p.id = t.project_id LEFT JOIN task par ON par.id = t.parent_task_id
            """;

    private final NamedParameterJdbcTemplate jdbc;

    public RecycleBin(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** The instant before which an item is already gone. */
    public static Instant cutoff(Instant now) {
        return now.minus(RETENTION);
    }

    // ---- deleting ----

    /** Marks the task, and the subtasks of it that are not deleted yet, with the same instant (R9). */
    public void deleteTask(long taskId, Instant at) {
        var params = new MapSqlParameterSource("id", taskId).addValue("at", stored(at));
        jdbc.update("UPDATE task SET deleted_at = :at WHERE (id = :id OR parent_task_id = :id) AND deleted_at IS NULL", params);
    }

    /** Marks the project and all its tasks that are not deleted yet with the same instant (R9, M4). */
    public void deleteProject(long projectId, Instant at) {
        var params = new MapSqlParameterSource("id", projectId).addValue("at", stored(at));
        jdbc.update("UPDATE project SET deleted_at = :at WHERE id = :id AND deleted_at IS NULL", params);
        jdbc.update("UPDATE task SET deleted_at = :at WHERE project_id = :id AND deleted_at IS NULL", params);
    }

    // ---- reading ----

    /** The projects of the manager that are in the bin, newest first. */
    public List<DeletedProject> projectsOfManager(long managerId, Instant now) {
        return jdbc.query("SELECT " + PROJECT_COLUMNS + ", " + INCLUDED + " FROM project p"
                + " WHERE p.manager_id = :uid AND p.deleted_at > :cutoff ORDER BY p.deleted_at DESC, p.id",
                new MapSqlParameterSource("uid", managerId).addValue("cutoff", stored(cutoff(now))),
                RecycleBin::project);
    }

    /**
     * The tasks and subtasks in the bin of the projects the user manages or belongs to, newest first, without
     * those deleted together with their project or their parent: they come back with it (R9).
     */
    public List<DeletedTask> tasksVisibleTo(long userId, Instant now) {
        return jdbc.query(TASK_SELECT + """
                WHERE t.deleted_at > :cutoff
                  AND (p.manager_id = :uid OR EXISTS (SELECT 1 FROM project_member m WHERE m.project_id = p.id AND m.user_id = :uid))
                  AND (p.deleted_at IS NULL OR p.deleted_at <> t.deleted_at)
                  AND (par.id IS NULL OR par.deleted_at IS NULL OR par.deleted_at <> t.deleted_at)
                ORDER BY t.deleted_at DESC, t.id
                """, new MapSqlParameterSource("uid", userId).addValue("cutoff", stored(cutoff(now))), RecycleBin::task);
    }

    public Optional<DeletedProject> findProject(long id, Instant now) {
        return jdbc.query("SELECT " + PROJECT_COLUMNS + ", " + INCLUDED + " FROM project p"
                + " WHERE p.id = :id AND p.deleted_at > :cutoff",
                new MapSqlParameterSource("id", id).addValue("cutoff", stored(cutoff(now))), RecycleBin::project)
                .stream().findFirst();
    }

    /** The task in the bin, with its project and parent. */
    public Optional<DeletedTask> findTask(long id, Instant now) {
        return queryTask(id, now, "");
    }

    /** As findTask, and the task row is locked until the end of the transaction. */
    public Optional<DeletedTask> lockTask(long id, Instant now) {
        return queryTask(id, now, " FOR UPDATE OF t");
    }

    private Optional<DeletedTask> queryTask(long id, Instant now, String lock) {
        return jdbc.query(TASK_SELECT + " WHERE t.id = :id AND t.deleted_at > :cutoff" + lock,
                new MapSqlParameterSource("id", id).addValue("cutoff", stored(cutoff(now))), RecycleBin::task)
                .stream().findFirst();
    }

    /** The task and its subtasks that were deleted with it: what a restore brings back (R9). */
    public List<Long> idsDeletedWith(long taskId, Instant deletedAt) {
        return jdbc.queryForList("SELECT id FROM task WHERE (id = :id OR parent_task_id = :id) AND deleted_at = :at ORDER BY id",
                new MapSqlParameterSource("id", taskId).addValue("at", stored(deletedAt)), Long.class);
    }

    /** Locks the project row in the bin, so a restore and a purge of it do not cross. */
    public Optional<DeletedProject> lockProject(long id, Instant now) {
        return jdbc.query("SELECT " + PROJECT_COLUMNS + ", " + INCLUDED + " FROM project p"
                + " WHERE p.id = :id AND p.deleted_at > :cutoff FOR UPDATE OF p",
                new MapSqlParameterSource("id", id).addValue("cutoff", stored(cutoff(now))), RecycleBin::project)
                .stream().findFirst();
    }

    public boolean isMember(long projectId, long userId) {
        return Boolean.TRUE.equals(jdbc.queryForObject(
                "SELECT EXISTS (SELECT 1 FROM project_member WHERE project_id = :p AND user_id = :u)",
                new MapSqlParameterSource("p", projectId).addValue("u", userId), Boolean.class));
    }

    // ---- restoring ----

    /** Brings back the project and the tasks deleted with it, those with the same instant; the others stay in the bin (R9). */
    public void restoreProject(long projectId, Instant deletedAt) {
        var params = new MapSqlParameterSource("id", projectId).addValue("at", stored(deletedAt));
        jdbc.update("UPDATE task SET deleted_at = NULL WHERE project_id = :id AND deleted_at = :at", params);
        jdbc.update("UPDATE project SET deleted_at = NULL WHERE id = :id AND deleted_at = :at", params);
    }

    /** Brings back the task and the subtasks deleted with it (R9). */
    public void restoreTask(long taskId, Instant deletedAt) {
        var params = new MapSqlParameterSource("id", taskId).addValue("at", stored(deletedAt));
        jdbc.update("UPDATE task SET deleted_at = NULL WHERE (id = :id OR parent_task_id = :id) AND deleted_at = :at", params);
    }

    // ---- eliminating for good ----

    /**
     * Removes for good what has been in the bin for 30 days or more (R20): the expired tasks, the tasks of an
     * expired project, the subtasks of an expired task, and the projects. Their comments and assignees go
     * with them. A duplicate whose original is removed keeps the status Duplicate and loses the link to it
     * (A41). Returns the number of tasks and projects removed.
     */
    public int purgeExpired(Instant now) {
        var params = new MapSqlParameterSource("cutoff", stored(cutoff(now)));
        List<Long> projects = jdbc.queryForList(
                "SELECT id FROM project WHERE deleted_at IS NOT NULL AND deleted_at <= :cutoff", params, Long.class);
        List<Long> tasks = new ArrayList<>(jdbc.queryForList("""
                SELECT t.id FROM task t
                WHERE (t.deleted_at IS NOT NULL AND t.deleted_at <= :cutoff)
                   OR t.project_id IN (SELECT id FROM project WHERE deleted_at IS NOT NULL AND deleted_at <= :cutoff)
                   OR t.parent_task_id IN (SELECT id FROM task WHERE deleted_at IS NOT NULL AND deleted_at <= :cutoff)
                """, params, Long.class));
        if (!tasks.isEmpty()) {
            var ids = new MapSqlParameterSource("ids", tasks);
            jdbc.update("DELETE FROM task_comment WHERE task_id IN (:ids)", ids);
            jdbc.update("DELETE FROM task_assignee WHERE task_id IN (:ids)", ids);
            jdbc.update("UPDATE task SET original_task_id = NULL WHERE original_task_id IN (:ids)", ids);
            jdbc.update("DELETE FROM task WHERE parent_task_id IS NOT NULL AND id IN (:ids)", ids); // subtasks first
            jdbc.update("DELETE FROM task WHERE id IN (:ids)", ids);
        }
        if (!projects.isEmpty()) {
            var ids = new MapSqlParameterSource("ids", projects);
            jdbc.update("DELETE FROM project_member WHERE project_id IN (:ids)", ids);
            jdbc.update("DELETE FROM project WHERE id IN (:ids)", ids);
        }
        return tasks.size() + projects.size();
    }

    // ---- mapping ----

    /** The number of main tasks deleted together with the project. */
    private static final String INCLUDED = """
            (SELECT count(*) FROM task i WHERE i.project_id = p.id AND i.parent_task_id IS NULL
                                           AND i.deleted_at = p.deleted_at) AS included""";

    /** The database keeps microseconds: the instant is cut to them, so what is stored equals what is read back. */
    private static OffsetDateTime stored(Instant instant) {
        return instant.truncatedTo(ChronoUnit.MICROS).atOffset(java.time.ZoneOffset.UTC);
    }

    private static DeletedProject project(ResultSet rs, int row) throws SQLException {
        return new DeletedProject(rs.getLong("id"), rs.getString("name"), rs.getLong("manager_id"),
                instant(rs, "deleted_at"), rs.getInt("included"));
    }

    private static DeletedTask task(ResultSet rs, int row) throws SQLException {
        return new DeletedTask(rs.getLong("id"), rs.getLong("project_id"), rs.getString("project_name"),
                rs.getLong("manager_id"), rs.getString("project_status"), instant(rs, "project_deleted_at"),
                (Long) rs.getObject("parent_task_id"), rs.getString("parent_status"),
                rs.getObject("parent_deadline", LocalDate.class), instant(rs, "parent_deleted_at"),
                rs.getString("title"), rs.getString("status"), rs.getObject("deadline", LocalDate.class),
                instant(rs, "deleted_at"));
    }

    private static Instant instant(ResultSet rs, String column) throws SQLException {
        OffsetDateTime value = rs.getObject(column, OffsetDateTime.class);
        return value == null ? null : value.toInstant();
    }
}
