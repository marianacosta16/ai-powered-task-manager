package com.matriz.tasks;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import com.matriz.shared.Page;
import com.matriz.shared.PageRequest;

/**
 * Reads comments. They follow their task (R24): a comment of a deleted task is not found (R9, M4).
 * The use cases check the task too, so this filter is a second line of defence for new callers.
 */
@Repository
public class CommentQueries {

    private static final String FROM = """
            FROM task_comment c JOIN task t ON t.id = c.task_id WHERE t.deleted_at IS NULL
            """;
    private static final String COLUMNS = "c.id, c.task_id, c.author_id, c.content, c.created_at";

    private final JdbcTemplate jdbc;

    public CommentQueries(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public Optional<CommentRecord> findById(long id) {
        return jdbc.query("SELECT " + COLUMNS + " " + FROM + " AND c.id = ?", CommentQueries::map, id)
                .stream().findFirst();
    }

    /** One page of the comments of the task, from the oldest to the newest (P28, R24); the id settles ties. */
    public Page<CommentRecord> listByTask(long taskId, PageRequest page) {
        long total = jdbc.queryForObject("SELECT count(*) " + FROM + " AND c.task_id = ?", Long.class, taskId);
        List<CommentRecord> comments = jdbc.query("SELECT " + COLUMNS + " " + FROM
                + " AND c.task_id = ? ORDER BY c.created_at, c.id LIMIT ? OFFSET ?",
                CommentQueries::map, taskId, page.size(), page.offset());
        return new Page<>(comments, total);
    }

    private static CommentRecord map(ResultSet rs, int row) throws SQLException {
        return new CommentRecord(rs.getLong("id"), rs.getLong("task_id"), rs.getLong("author_id"),
                rs.getString("content"), rs.getObject("created_at", OffsetDateTime.class).toInstant());
    }
}
