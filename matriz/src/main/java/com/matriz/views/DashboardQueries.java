package com.matriz.views;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

/**
 * Read-only counts for the dashboard (F07). It only reads the data of Tasks (docs/architecture.md,
 * section 4); who may see each project is decided before it is called.
 */
@Repository
public class DashboardQueries {

    private final NamedParameterJdbcTemplate jdbc;

    public DashboardQueries(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /**
     * The number of main tasks of each project by status (R18); tasks and projects that are deleted do not
     * count (R9). A project without tasks is in the result, with all the counts at 0.
     */
    public Map<Long, StatusCounts> countsByProject(Collection<Long> projectIds) {
        Map<Long, Map<String, Integer>> byProject = new HashMap<>();
        if (!projectIds.isEmpty()) {
            jdbc.query("""
                    SELECT project_id, status, count(*) AS total FROM task
                    WHERE project_id IN (:ids) AND parent_task_id IS NULL AND deleted_at IS NULL
                    GROUP BY project_id, status
                    """, new MapSqlParameterSource("ids", projectIds), rs -> {
                byProject.computeIfAbsent(rs.getLong("project_id"), id -> new HashMap<>())
                        .put(rs.getString("status"), rs.getInt("total"));
            });
        }
        Map<Long, StatusCounts> result = new HashMap<>();
        for (Long id : projectIds) {
            result.put(id, StatusCounts.of(byProject.getOrDefault(id, Map.of())));
        }
        return result;
    }
}
