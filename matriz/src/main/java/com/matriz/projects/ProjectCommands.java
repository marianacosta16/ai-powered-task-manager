package com.matriz.projects;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/** Writes projects. The rules are checked by the callers (use cases). */
@Repository
public class ProjectCommands {

    private final JdbcTemplate jdbc;

    public ProjectCommands(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public long insert(NewProject p) {
        return jdbc.queryForObject("""
                INSERT INTO project (name, description, start_date, start_time, end_date, end_time,
                                     timezone, phase, status, manager_id)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?) RETURNING id
                """, Long.class, p.name(), p.description(), p.startDate(), p.startTime(), p.endDate(), p.endTime(),
                p.timezone(), p.phase(), p.status(), p.managerId());
    }

    public void update(ProjectRecord p) {
        jdbc.update("""
                UPDATE project SET name = ?, description = ?, start_date = ?, start_time = ?, end_date = ?,
                                   end_time = ?, timezone = ?, phase = ?, status = ?, manager_id = ?
                WHERE id = ? AND deleted_at IS NULL
                """, p.name(), p.description(), p.startDate(), p.startTime(), p.endDate(), p.endTime(),
                p.timezone(), p.phase(), p.status(), p.managerId(), p.id());
    }
}
