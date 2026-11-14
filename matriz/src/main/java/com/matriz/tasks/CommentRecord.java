package com.matriz.tasks;

import java.time.Instant;

/** A comment on a task or a subtask (docs/database.md, COMMENT). The author is held by identifier only. */
public record CommentRecord(long id, long taskId, long authorId, String content, Instant createdAt) {
}
