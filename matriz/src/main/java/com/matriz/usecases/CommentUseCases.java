package com.matriz.usecases;

import java.time.Clock;
import java.util.List;
import java.util.function.Supplier;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.matriz.access.AuthenticatedUser;
import com.matriz.access.UserDirectory;
import com.matriz.shared.ForbiddenException;
import com.matriz.shared.NotFoundException;
import com.matriz.tasks.CommentCommands;
import com.matriz.tasks.CommentQueries;
import com.matriz.tasks.CommentRecord;

/**
 * listComments, addComment, editComment and deleteComment (F17; US11, R24, P28). The author and the
 * date are set here, from the session and the clock, never taken from the request. The body is read
 * only after 404, 403, R27/R28 and P37 are checked (P37 with the task locked), so someone who may not comment gets no hints about it.
 */
@Service
public class CommentUseCases {

    private final CommentQueries queries;
    private final CommentCommands commands;
    private final TaskRules rules;
    private final UserDirectory users;
    private final Locks locks;
    private final Clock clock;

    CommentUseCases(CommentQueries queries, CommentCommands commands, TaskRules rules, UserDirectory users, Locks locks,
            Clock clock) {
        this.queries = queries;
        this.commands = commands;
        this.rules = rules;
        this.users = users;
        this.locks = locks;
        this.clock = clock;
    }

    /** Whoever can see the task sees its comments, also in a completed project (R24). */
    public List<CommentView> listComments(AuthenticatedUser user, long taskId) {
        rules.load(user, taskId);
        return views(queries.listByTask(taskId));
    }

    @Transactional
    public CommentView addComment(AuthenticatedUser user, long taskId, Supplier<String> body) {
        rules.loadForChange(user, taskId); // 404, 403 and R28
        requireTaskOpenForComments(taskId);
        String content = body.get();
        long id = commands.insert(taskId, user.id(), content, clock.instant());
        return views(List.of(queries.findById(id).orElseThrow())).get(0);
    }

    @Transactional
    public CommentView editComment(AuthenticatedUser user, long commentId, Supplier<String> body) {
        CommentRecord comment = ownComment(user, commentId);
        String content = body.get();
        if (!commands.updateContent(comment.id(), content)) {
            throw new NotFoundException("The comment was not found.");
        }
        return views(List.of(new CommentRecord(comment.id(), comment.taskId(), comment.authorId(), content,
                comment.createdAt()))).get(0);
    }

    @Transactional
    public void deleteComment(AuthenticatedUser user, long commentId) {
        CommentRecord comment = ownComment(user, commentId);
        if (!commands.delete(comment.id())) {
            throw new NotFoundException("The comment was not found.");
        }
    }

    /**
     * The comment, when the user may change it: they can reach its task, the project is not
     * completed (R28) and they are its author, and nobody else, not even the manager (R24). No lock
     * is needed: if the comment is deleted meanwhile, the update or the delete finds no row and the
     * answer is 404.
     */
    private CommentRecord ownComment(AuthenticatedUser user, long commentId) {
        CommentRecord comment = queries.findById(commentId)
                .orElseThrow(() -> new NotFoundException("The comment was not found."));
        rules.loadForChange(user, comment.taskId());
        requireTaskOpenForComments(comment.taskId());
        if (comment.authorId() != user.id()) {
            throw new ForbiddenException("Only the author of a comment can edit or delete it.");
        }
        return comment;
    }

    /**
     * P37. The task is read with its row locked until the end of the transaction, so a task that is
     * being closed at the same moment is either closed before this check (and refused) or after the
     * comment is written, never in between.
     */
    private void requireTaskOpenForComments(long taskId) {
        TaskRules.requireTaskNotClosed(locks.task(taskId)
                .orElseThrow(() -> new NotFoundException("The task was not found.")));
    }

    private List<CommentView> views(List<CommentRecord> comments) {
        var authors = users.findByIds(comments.stream().map(CommentRecord::authorId).distinct().toList());
        return comments.stream().map(c -> new CommentView(c.id(), c.taskId(), authors.get(c.authorId()), c.content(),
                c.createdAt())).toList();
    }
}
