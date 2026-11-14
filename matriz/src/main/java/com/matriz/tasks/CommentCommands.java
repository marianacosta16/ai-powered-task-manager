package com.matriz.tasks;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/** Writes comments. The rules are checked by the callers (use cases). */
@Repository
public class CommentCommands {

    private final JdbcTemplate jdbc;

    public CommentCommands(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** The author and the date are given by the application, never by the client (R24). */
    public long insert(long taskId, long authorId, String content, Instant createdAt) {
        return jdbc.queryForObject("""
                INSERT INTO task_comment (task_id, author_id, content, created_at) VALUES (?, ?, ?, ?) RETURNING id
                """, Long.class, taskId, authorId, content, OffsetDateTime.ofInstant(createdAt, ZoneOffset.UTC));
    }

    /** Only the text changes: the author and the date of creation stay (R24). */
    public boolean updateContent(long id, String content) {
        return jdbc.update("UPDATE task_comment SET content = ? WHERE id = ?", content, id) == 1;
    }

    /** Deleting removes the comment for good: there is no recycle bin for comments (R24, P28). */
    public boolean delete(long id) {
        return jdbc.update("DELETE FROM task_comment WHERE id = ?", id) == 1;
    }
}
