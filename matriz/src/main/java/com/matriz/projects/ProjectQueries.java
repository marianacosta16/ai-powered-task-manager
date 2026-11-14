package com.matriz.projects;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

/** Read access to the projects (not deleted ones only: R9, M4). Who may see them is decided elsewhere. */
@Repository
public class ProjectQueries {

    private static final String COLUMNS = """
            id, name, description, start_date, start_time, end_date, end_time, timezone, phase, status, manager_id
            """;

    private final NamedParameterJdbcTemplate jdbc;

    public ProjectQueries(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public Optional<ProjectRecord> findById(long id) {
        return jdbc.query("SELECT " + COLUMNS + " FROM project WHERE id = :id AND deleted_at IS NULL",
                new MapSqlParameterSource("id", id), ProjectQueries::map).stream().findFirst();
    }

    /**
     * Reads the project and locks its row until the end of the transaction, so a concurrent update
     * waits and then reads the fresh values instead of overwriting them with stale ones.
     */
    public Optional<ProjectRecord> findByIdForUpdate(long id) {
        return jdbc.query("SELECT " + COLUMNS + " FROM project WHERE id = :id AND deleted_at IS NULL FOR UPDATE",
                new MapSqlParameterSource("id", id), ProjectQueries::map).stream().findFirst();
    }

    /** The given projects, optionally only those whose name contains the text, ignoring case (P29). */
    public List<ProjectRecord> findAmong(Collection<Long> ids, String search) {
        if (ids.isEmpty()) {
            return List.of();
        }
        MapSqlParameterSource params = new MapSqlParameterSource("ids", ids);
        String sql = "SELECT " + COLUMNS + " FROM project WHERE id IN (:ids) AND deleted_at IS NULL";
        if (search != null && !search.isEmpty()) { // spaces are text too (P29); an empty value is refused by the API
            sql += " AND name ILIKE :search ESCAPE '\\'";
            params.addValue("search", "%" + escapeLike(search) + "%");
        }
        return jdbc.query(sql + " ORDER BY id", params, ProjectQueries::map);
    }

    /** The given projects that are not completed (P27): the ones the dashboard shows. */
    public List<ProjectRecord> findNotCompletedAmong(Collection<Long> ids) {
        if (ids.isEmpty()) {
            return List.of();
        }
        return jdbc.query("SELECT " + COLUMNS + " FROM project WHERE id IN (:ids) AND deleted_at IS NULL"
                + " AND status <> :completed ORDER BY id",
                new MapSqlParameterSource("ids", ids).addValue("completed", ProjectStatus.COMPLETED.label()),
                ProjectQueries::map);
    }

    private static String escapeLike(String text) {
        return text.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }

    private static ProjectRecord map(ResultSet rs, int row) throws SQLException {
        return new ProjectRecord(rs.getLong("id"), rs.getString("name"), rs.getString("description"),
                rs.getObject("start_date", LocalDate.class), rs.getObject("start_time", LocalTime.class),
                rs.getObject("end_date", LocalDate.class), rs.getObject("end_time", LocalTime.class),
                rs.getString("timezone"), rs.getString("phase"), rs.getString("status"),
                rs.getLong("manager_id"));
    }
}
