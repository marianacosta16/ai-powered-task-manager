package com.matriz.projects;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

/** The members of each project (F01, F06). The manager does not need a row in his own project (M5). */
@Repository
public class TeamRepository {

    private static final String OPEN_STATUSES = "('Not started', 'In progress')"; // P4

    private final JdbcTemplate jdbc;
    private final NamedParameterJdbcTemplate named;

    public TeamRepository(JdbcTemplate jdbc, NamedParameterJdbcTemplate named) {
        this.jdbc = jdbc;
        this.named = named;
    }

    /** Identifiers of the members of the project, by identifier. */
    public List<Long> memberIds(long projectId) {
        return jdbc.queryForList("SELECT user_id FROM project_member WHERE project_id = ? ORDER BY user_id",
                Long.class, projectId);
    }

    public boolean isMember(long projectId, long userId) {
        return Boolean.TRUE.equals(jdbc.queryForObject(
                "SELECT EXISTS (SELECT 1 FROM project_member WHERE project_id = ? AND user_id = ?)",
                Boolean.class, projectId, userId));
    }

    public void add(long projectId, long userId) {
        jdbc.update("INSERT INTO project_member (project_id, user_id) VALUES (?, ?)", projectId, userId);
    }

    public void remove(long projectId, long userId) {
        jdbc.update("DELETE FROM project_member WHERE project_id = ? AND user_id = ?", projectId, userId);
    }

    /** Which of these people are members of another open project, not counting the given one (R11, P22). */
    public Set<Long> inOtherOpenProject(Collection<Long> userIds, long excludedProjectId) {
        if (userIds.isEmpty()) {
            return Set.of();
        }
        return new HashSet<>(named.queryForList("""
                SELECT DISTINCT m.user_id FROM project_member m JOIN project p ON p.id = m.project_id
                WHERE m.user_id IN (:ids) AND p.id <> :pid AND p.deleted_at IS NULL
                  AND p.status IN """ + OPEN_STATUSES,
                new MapSqlParameterSource("ids", userIds).addValue("pid", excludedProjectId), Long.class));
    }
}
