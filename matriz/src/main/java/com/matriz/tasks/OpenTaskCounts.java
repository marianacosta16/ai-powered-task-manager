package com.matriz.tasks;

import java.util.HashMap;
import java.util.Map;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/** Open tasks and subtasks of each assignee in a project: those not in a final status and not deleted. */
@Repository
public class OpenTaskCounts {

    private final JdbcTemplate jdbc;

    public OpenTaskCounts(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public Map<Long, Integer> byAssignee(long projectId) {
        Map<Long, Integer> counts = new HashMap<>();
        jdbc.query("""
                SELECT a.user_id, count(*) AS open_tasks
                FROM task t JOIN task_assignee a ON a.task_id = t.id
                WHERE t.project_id = ? AND t.deleted_at IS NULL
                  AND t.status NOT IN ('Done', 'Cancelled', 'Duplicate')
                GROUP BY a.user_id
                """, rs -> {
                    counts.put(rs.getLong("user_id"), rs.getInt("open_tasks"));
                }, projectId);
        return counts;
    }

    public int forAssignee(long projectId, long userId) {
        return jdbc.queryForObject("""
                SELECT count(*) FROM task t JOIN task_assignee a ON a.task_id = t.id
                WHERE t.project_id = ? AND a.user_id = ? AND t.deleted_at IS NULL
                  AND t.status NOT IN ('Done', 'Cancelled', 'Duplicate')
                """, Integer.class, projectId, userId);
    }
}
