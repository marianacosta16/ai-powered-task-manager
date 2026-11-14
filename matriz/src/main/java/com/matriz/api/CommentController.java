package com.matriz.api;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.matriz.access.AuthenticatedUser;
import com.matriz.usecases.CommentUseCases;
import com.matriz.usecases.CommentView;

import jakarta.validation.constraints.Min;

/**
 * listComments, addComment, editComment and deleteComment. The author is always the user of the
 * session. The use case checks who may do it before it reads the body (403 before 400).
 */
@Validated
@RestController
class CommentController {

    private final CommentUseCases comments;
    private final RequestValidator validator;

    CommentController(CommentUseCases comments, RequestValidator validator) {
        this.comments = comments;
        this.validator = validator;
    }

    @GetMapping("/tasks/{taskId}/comments")
    List<CommentView> listComments(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable @Min(1) long taskId) {
        return comments.listComments(user, taskId);
    }

    @PostMapping("/tasks/{taskId}/comments")
    @ResponseStatus(HttpStatus.CREATED)
    CommentView addComment(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable @Min(1) long taskId,
            @RequestBody CommentWriteRequest body) {
        return comments.addComment(user, taskId, () -> content(body));
    }

    @PatchMapping("/comments/{commentId}")
    CommentView editComment(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable @Min(1) long commentId,
            @RequestBody CommentWriteRequest body) {
        return comments.editComment(user, commentId, () -> content(body));
    }

    @DeleteMapping("/comments/{commentId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void deleteComment(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable @Min(1) long commentId) {
        comments.deleteComment(user, commentId);
    }

    private String content(CommentWriteRequest body) {
        validator.validate(body);
        return body.content();
    }
}
