package com.matriz.views;

import java.util.Arrays;
import java.util.List;

import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import com.matriz.shared.Page;
import com.matriz.shared.PageRequest;
import com.matriz.tasks.Quadrant;
import com.matriz.tasks.TaskQueries;
import com.matriz.tasks.TaskRecord;
import com.matriz.tasks.TaskStatus;

/**
 * Read-only view of the tasks of a project, for the List and the Matrix (F04, F05, F22). It only reads
 * the data of Tasks (docs/architecture.md, section 4): it never writes, and who may see the project is
 * decided before it is called.
 */
@Repository
public class ProjectTaskQueries {

    /** The labels of the final statuses (closed tasks), taken from {@link TaskStatus}. */
    private static final List<String> FINAL_STATUSES = Arrays.stream(TaskStatus.values())
            .filter(TaskStatus::isFinal).map(TaskStatus::label).toList();

    private final NamedParameterJdbcTemplate jdbc;

    public ProjectTaskQueries(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /**
     * One page of the main tasks of the project that are not deleted (R18, R9), in the order of the List
     * (R15, R23): the open ones first, then the closed ones; inside each group by deadline, earliest first,
     * and with the same deadline by urgency and then importance, highest first; the order of creation
     * settles any tie, so the pages never overlap. The filters combine (R25, P29):
     * <ul>
     * <li>{@code assigneeId}: only the tasks of that person; a main task counts if the person is
     * responsible for it or for any of its subtasks (P25);</li>
     * <li>{@code status} and {@code quadrant}: only the main tasks with that status or quadrant.</li>
     * </ul>
     * Any filter that is null is not applied. The total counts all the tasks that match the filters.
     */
    public Page<TaskRecord> mainTasks(long projectId, Long assigneeId, TaskStatus status, Quadrant quadrant,
            PageRequest page) {
        MapSqlParameterSource params = new MapSqlParameterSource("pid", projectId);
        StringBuilder where = new StringBuilder(" WHERE t.project_id = :pid AND t.parent_task_id IS NULL AND t.deleted_at IS NULL");
        if (status != null) {
            where.append(" AND t.status = :status");
            params.addValue("status", status.label());
        }
        if (quadrant != null) {
            where.append(" AND t.urgency ").append(quadrant.urgent() ? ">=" : "<").append(" :high")
                    .append(" AND t.importance ").append(quadrant.important() ? ">=" : "<").append(" :high");
            params.addValue("high", Quadrant.HIGH_LEVEL);
        }
        if (assigneeId != null) {
            where.append("""
                     AND (EXISTS (SELECT 1 FROM task_assignee a WHERE a.task_id = t.id AND a.user_id = :uid)
                          OR EXISTS (SELECT 1 FROM task s JOIN task_assignee sa ON sa.task_id = s.id
                                     WHERE s.parent_task_id = t.id AND s.deleted_at IS NULL AND sa.user_id = :uid))
                    """);
            params.addValue("uid", assigneeId);
        }
        long total = jdbc.queryForObject("SELECT count(*) FROM task t" + where, params, Long.class);
        params.addValue("final", FINAL_STATUSES);
        params.addValue("limit", page.size());
        params.addValue("offset", page.offset());
        List<TaskRecord> tasks = jdbc.query("SELECT " + TaskQueries.columns("t") + " FROM task t" + where
                + " ORDER BY CASE WHEN t.status IN (:final) THEN 1 ELSE 0 END,"
                + " t.deadline, t.urgency DESC, t.importance DESC, t.id LIMIT :limit OFFSET :offset",
                params, TaskQueries::map);
        return new Page<>(tasks, total);
    }
}
