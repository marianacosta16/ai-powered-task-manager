package com.matriz.usecases;

import java.time.Instant;

import com.matriz.access.UserInfo;

/** Comment of the contract: its author and the date and time it was created. */
public record CommentView(long id, long taskId, UserInfo author, String content, Instant createdAt) {
}
