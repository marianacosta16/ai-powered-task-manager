package com.matriz.comments;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.support.TransactionTemplate;

import com.jayway.jsonpath.JsonPath;
import com.matriz.tasks.CommentQueries;
import com.matriz.testsupport.ApiIntegrationTest;

/**
 * US11 (scenarios 11.1 to 11.6 and 11.8 to 11.10, and the first half of 11.7), EC32 and EC34, R24,
 * P28 and NF09, through real HTTP requests. Tasks used: Website has "Design homepage" (with the
 * subtask "Draft wireframes", closed) and Old Site, a completed project, has "Shut down server".
 */
class CommentsIntegrationTest extends ApiIntegrationTest {

    @Autowired CommentQueries commentQueries;

    // ---- adding and listing (11.1 to 11.5) ----

    @Test
    void theAuthorAndTheDateAreSetByTheServerFromTheSession() { // 11.1
        clock.set("2026-06-01T10:00:00Z");
        long design = taskId("Design homepage");

        ResponseEntity<String> created = comment("ana", design, "Falta rever o capítulo 2");

        assertThat(created.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(json(created, "$.content")).isEqualTo("Falta rever o capítulo 2");
        assertThat(json(created, "$.author.name")).isEqualTo("Ana");
        assertThat(json(created, "$.createdAt")).isEqualTo("2026-06-01T10:00:00Z");
        assertThat(json(created, "$.taskId")).isEqualTo((int) design);
        assertThat(authorOf(((Number) json(created, "$.id")).longValue())).isEqualTo(userId("ana"));
    }

    @Test
    void theClientCannotChooseTheAuthorOrTheDate() { // Exercise 3: inferred in the backend
        long design = taskId("Design homepage");
        int before = count("task_comment");

        ResponseEntity<String> response = post("/tasks/" + design + "/comments", sessionOf("ana"),
                Map.of("content", "Pretending", "authorId", userId("marta"), "createdAt", "2020-01-01T00:00:00Z"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(json(response, "$.code")).isEqualTo("VALIDATION_ERROR");
        assertThat(count("task_comment")).isEqualTo(before);
    }

    @Test
    void commentsAreListedFromTheOldestToTheNewestByDate() { // 11.2
        long design = taskId("Design homepage");
        jdbc.update("DELETE FROM task_comment");
        // written out of order on purpose: the order is by date, not by when the row was inserted
        clock.set("2026-06-01T11:00:00Z");
        comment("marta", design, "Third");
        clock.set("2026-06-01T09:00:00Z");
        comment("ana", design, "First");
        clock.set("2026-06-01T10:00:00Z");
        comment("rui", design, "Second");

        ResponseEntity<String> list = get("/tasks/" + design + "/comments", sessionOf("luis"));

        assertThat(JsonPath.<List<String>>read(list.getBody(), "$[*].content")).containsExactly("First", "Second", "Third");
        assertThat(JsonPath.<List<String>>read(list.getBody(), "$[*].author.name")).containsExactly("Ana", "Rui", "Marta");
        assertThat(JsonPath.<List<String>>read(list.getBody(), "$[*].createdAt"))
                .containsExactly("2026-06-01T09:00:00Z", "2026-06-01T10:00:00Z", "2026-06-01T11:00:00Z");
    }

    @Test
    void onlyThoseWhoCanReachTheProjectSeeOrAddComments() { // 11.3
        long design = taskId("Design homepage");
        int before = count("task_comment");

        ResponseEntity<String> read = get("/tasks/" + design + "/comments", sessionOf("sofia")); // not in Website
        ResponseEntity<String> write = comment("sofia", design, "Let me in");

        assertThat(read.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(write.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(count("task_comment")).isEqualTo(before);
        assertThat(get("/tasks/" + design + "/comments", null).getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(get("/tasks/999999/comments", sessionOf("ana")).getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(get("/tasks/abc/comments", sessionOf("ana")).getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void textIsStoredAndReturnedLiterally() { // 11.4, NF09
        ResponseEntity<String> created = comment("ana", taskId("Design homepage"), "<b>x</b>");

        assertThat(json(created, "$.content")).isEqualTo("<b>x</b>");
        assertThat(jdbc.queryForObject("SELECT content FROM task_comment WHERE id = ?", String.class,
                ((Number) json(created, "$.id")).longValue())).isEqualTo("<b>x</b>");
    }

    @Test
    void aCommentHasAtMost2000Characters() { // 11.5, EC32
        long design = taskId("Design homepage");

        ResponseEntity<String> exact = comment("ana", design, "c".repeat(2000));
        ResponseEntity<String> tooLong = comment("ana", design, "c".repeat(2001));
        ResponseEntity<String> blank = comment("ana", design, "   ");

        assertThat(exact.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(tooLong.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(JsonPath.<List<String>>read(tooLong.getBody(), "$.details[*].field")).containsExactly("content");
        assertThat(blank.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    // ---- editing and deleting (11.6) ----

    @Test
    void theAuthorEditsKeepingTheAuthorAndTheDateOfCreation() { // 11.6
        clock.set("2026-06-01T09:00:00Z");
        long id = idOf(comment("ana", taskId("Design homepage"), "Falta rever o capítulo 2"));
        clock.set("2026-06-02T15:00:00Z");

        ResponseEntity<String> edited = patch("/comments/" + id, sessionOf("ana"), Map.of("content", "Capítulo 2 revisto"));

        assertThat(edited.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(json(edited, "$.content")).isEqualTo("Capítulo 2 revisto");
        assertThat(json(edited, "$.author.name")).isEqualTo("Ana");
        assertThat(json(edited, "$.createdAt")).isEqualTo("2026-06-01T09:00:00Z"); // the original date
        assertThat(patch("/comments/" + id, sessionOf("ana"), Map.of("content", "c".repeat(2001))).getStatusCode())
                .isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(contentOf(id)).isEqualTo("Capítulo 2 revisto"); // the previous text stays
    }

    @Test
    void nobodyButTheAuthorEditsOrDeletes() { // 11.6, R24: not even the manager
        long id = idOf(comment("ana", taskId("Design homepage"), "Mine"));

        for (String other : List.of("rui", "marta")) { // another member, and the manager of the project
            assertThat(patch("/comments/" + id, sessionOf(other), Map.of("content", "Changed")).getStatusCode())
                    .as("edit by " + other).isEqualTo(HttpStatus.FORBIDDEN);
            assertThat(delete("/comments/" + id, sessionOf(other)).getStatusCode())
                    .as("delete by " + other).isEqualTo(HttpStatus.FORBIDDEN);
        }
        assertThat(contentOf(id)).isEqualTo("Mine");
        assertThat(patch("/comments/" + id, sessionOf("sofia"), Map.of("content", "Changed")).getStatusCode())
                .isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void deletingRemovesTheCommentForGood() { // 11.6, P28
        long design = taskId("Design homepage");
        long id = idOf(comment("ana", design, "Soon gone"));

        ResponseEntity<String> deleted = delete("/comments/" + id, sessionOf("ana"));

        assertThat(deleted.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        assertThat(JsonPath.<List<String>>read(get("/tasks/" + design + "/comments", sessionOf("ana")).getBody(), "$[*].content"))
                .doesNotContain("Soon gone");
        assertThat(count("task_comment WHERE id = " + id)).isZero(); // no recycle bin for comments
        assertThat(delete("/comments/" + id, sessionOf("ana")).getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(patch("/comments/" + id, sessionOf("ana"), Map.of("content", "Back?")).getStatusCode())
                .isEqualTo(HttpStatus.NOT_FOUND);
    }

    // ---- they follow the task (11.7, first half), subtasks (11.8) ----

    @Test
    void theCommentsOfADeletedTaskAreNotReachable() { // 11.7: they disappear with the task (the recycle bin is increment 7)
        long design = taskId("Design homepage");
        long id = idOf(comment("ana", design, "Follows the task"));
        jdbc.update("UPDATE task SET deleted_at = now() WHERE id = ?", design);

        assertThat(get("/tasks/" + design + "/comments", sessionOf("ana")).getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(patch("/comments/" + id, sessionOf("ana"), Map.of("content", "x")).getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);

        jdbc.update("UPDATE task SET deleted_at = NULL WHERE id = ?", design); // recovered: they are back
        assertThat(JsonPath.<List<String>>read(get("/tasks/" + design + "/comments", sessionOf("ana")).getBody(), "$[*].content"))
                .contains("Follows the task");
    }

    @Test
    void subtasksHaveCommentsWithTheSameRules() { // 11.8
        long subtask = taskId("Pick colour palette"); // an open subtask

        ResponseEntity<String> created = comment("ana", subtask, "Enviado");
        long id = idOf(created);

        assertThat(created.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(json(created, "$.author.name")).isEqualTo("Ana");
        assertThat(patch("/comments/" + id, sessionOf("rui"), Map.of("content", "x")).getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(patch("/comments/" + id, sessionOf("ana"), Map.of("content", "Enviado!")).getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(delete("/comments/" + id, sessionOf("ana")).getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
    }

    @Test
    void aClosedTaskOrSubtaskTakesNoChangesToItsComments() { // P37: they are only read
        long closedSubtask = taskId("Draft wireframes"); // a subtask, Done, with a comment of Ana
        long id = jdbc.queryForObject("SELECT id FROM task_comment WHERE task_id = ?", Long.class, closedSubtask);
        String text = contentOf(id);
        String ana = sessionOf("ana");

        ResponseEntity<String> list = get("/tasks/" + closedSubtask + "/comments", ana);
        List<ResponseEntity<String>> refused = List.of(
                comment("ana", closedSubtask, "New"),
                patch("/comments/" + id, ana, Map.of("content", "Changed")),
                delete("/comments/" + id, ana));

        assertThat(list.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(JsonPath.<List<String>>read(list.getBody(), "$[*].content")).containsExactly(text);
        for (ResponseEntity<String> r : refused) {
            assertThat(r.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
            assertThat(json(r, "$.code")).isEqualTo("TASK_CLOSED");
        }
        assertThat(contentOf(id)).isEqualTo(text);

        // every final status closes it, a main task too; reopening it (only the manager can, P8) lifts the freeze
        long hosting = taskId("Set up hosting");
        patch("/tasks/" + hosting, sessionOf("luis"), Map.of("version", 1, "status", "Cancelled"));
        long copyText = taskId("Contact page text"); // a Duplicate
        assertThat(json(comment("ana", hosting, "x"), "$.code")).isEqualTo("TASK_CLOSED");
        assertThat(json(comment("ana", copyText, "x"), "$.code")).isEqualTo("TASK_CLOSED");
        patch("/tasks/" + closedSubtask, sessionOf("marta"), Map.of("version", 1, "status", "In progress"));
        assertThat(comment("ana", closedSubtask, "After reopening").getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(patch("/comments/" + id, ana, Map.of("content", "Edited again")).getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    // ---- project states (11.9, 11.10, EC34) ----

    @Test
    void inASuspendedProjectCommentsCanBeAddedEditedAndDeleted() { // 11.9
        long design = taskId("Design homepage");
        patch("/projects/" + projectId("Website"), sessionOf("marta"), Map.of("status", "Suspended"));
        String ana = sessionOf("ana");

        ResponseEntity<String> created = comment("ana", design, "While suspended");
        long id = idOf(created);

        assertThat(created.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(patch("/comments/" + id, ana, Map.of("content", "Edited")).getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(delete("/comments/" + id, ana).getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
    }

    @Test
    void inACompletedProjectCommentsAreVisibleButNothingChanges() { // 11.10, EC34
        long shutdown = taskId("Shut down server"); // Old Site is completed; Diana has a comment there
        String diana = sessionOf("diana");
        long id = jdbc.queryForObject("SELECT id FROM task_comment WHERE task_id = ?", Long.class, shutdown);
        String text = contentOf(id);

        ResponseEntity<String> list = get("/tasks/" + shutdown + "/comments", diana);
        List<ResponseEntity<String>> refused = List.of(
                comment("diana", shutdown, "New"),
                patch("/comments/" + id, diana, Map.of("content", "Changed")),
                delete("/comments/" + id, diana));

        assertThat(list.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(JsonPath.<List<String>>read(list.getBody(), "$[*].content")).containsExactly(text);
        for (ResponseEntity<String> r : refused) {
            assertThat(r.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
            assertThat(json(r, "$.code")).isEqualTo("PROJECT_COMPLETED");
        }
        assertThat(contentOf(id)).isEqualTo(text);
    }

    @Test
    void whoMayNotCommentGetsNoHintsAboutTheBody() { // 403 before 400
        long design = taskId("Design homepage");

        ResponseEntity<String> outsider = post("/tasks/" + design + "/comments", sessionOf("sofia"), Map.of("content", ""));
        ResponseEntity<String> member = post("/tasks/" + design + "/comments", sessionOf("ana"), Map.of("content", ""));

        assertThat(outsider.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(member.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void whoMayNotCommentGetsNoHintsEvenWithAnUnknownField() { // 403 before 400
        long design = taskId("Design homepage");

        ResponseEntity<String> outsider = post("/tasks/" + design + "/comments", sessionOf("sofia"),
                Map.of("content", "x", "authorId", userId("marta")));
        ResponseEntity<String> member = post("/tasks/" + design + "/comments", sessionOf("ana"),
                Map.of("content", "x", "authorId", userId("marta")));

        assertThat(outsider.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(member.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void editingAndDeletingNeedASession() {
        long id = idOf(comment("ana", taskId("Design homepage"), "Mine"));

        assertThat(patch("/comments/" + id, null, Map.of("content", "x")).getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(delete("/comments/" + id, null).getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(contentOf(id)).isEqualTo("Mine");
    }

    @Test
    void theLimitCountsCharactersNotUtf16Units() { // R14: a symbol counts once
        long design = taskId("Design homepage");
        String emoji = "\uD83D\uDC4D"; // one character, two UTF-16 units

        ResponseEntity<String> fits = comment("ana", design, emoji.repeat(1200)); // 1200 characters, 2400 units
        ResponseEntity<String> exact = comment("ana", design, emoji.repeat(2000));
        ResponseEntity<String> tooLong = comment("ana", design, emoji.repeat(2001));

        assertThat(fits.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(exact.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(json(exact, "$.content")).isEqualTo(emoji.repeat(2000));
        assertThat(tooLong.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void theQueriesDoNotReturnTheCommentsOfADeletedTask() { // a second line of defence for the recycle bin
        long design = taskId("Design homepage");
        long id = idOf(comment("ana", design, "Hidden with the task"));
        assertThat(commentQueries.findById(id)).isPresent();

        jdbc.update("UPDATE task SET deleted_at = now() WHERE id = ?", design);

        assertThat(commentQueries.findById(id)).isEmpty();
        assertThat(commentQueries.listByTask(design)).isEmpty();
    }

    // ---- at the same time ----

    @Test
    void aCommentAndTheClosingOfTheTaskAtTheSameTimeNeverLeaveACommentOnAClosedTask() throws Exception {
        long hosting = taskId("Set up hosting"); // Blocked, open
        String ana = sessionOf("ana");
        int before = count("task_comment");
        ExecutorService pool = Executors.newSingleThreadExecutor();
        try {
            // the task is being closed (its row is locked) when the comment arrives: the comment waits, then sees it closed
            Future<ResponseEntity<String>> request = new TransactionTemplate(transactions).execute(tx -> {
                jdbc.queryForObject("SELECT id FROM task WHERE id = ? FOR UPDATE", Long.class, hosting);
                Future<ResponseEntity<String>> pending = pool.submit(() -> post("/tasks/" + hosting + "/comments", ana,
                        Map.of("content", "Too late")));
                awaitLockWaiters(1);
                jdbc.update("UPDATE task SET status = 'Done' WHERE id = ?", hosting);
                return pending;
            });

            ResponseEntity<String> response = request.get(15, TimeUnit.SECONDS);
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
            assertThat(json(response, "$.code")).isEqualTo("TASK_CLOSED");
        } finally {
            pool.shutdownNow();
        }
        assertThat(count("task_comment")).isEqualTo(before);
    }

    @Test
    void anEditAndADeletionAtTheSameTimeAreDoneOneAfterTheOther() throws Exception {
        long id = idOf(comment("ana", taskId("Design homepage"), "Racing"));
        String ana = sessionOf("ana");
        ExecutorService pool = Executors.newSingleThreadExecutor();
        try {
            // the edit arrives while the comment is being deleted: it waits for the lock and then finds it gone
            Future<ResponseEntity<String>> edit = new TransactionTemplate(transactions).execute(tx -> {
                jdbc.queryForObject("SELECT id FROM task_comment WHERE id = ? FOR UPDATE", Long.class, id);
                Future<ResponseEntity<String>> pending = pool.submit(() -> patch("/comments/" + id, ana, Map.of("content", "Late")));
                awaitLockWaiters(1);
                jdbc.update("DELETE FROM task_comment WHERE id = ?", id);
                return pending;
            });

            ResponseEntity<String> response = edit.get(15, TimeUnit.SECONDS);
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND); // not a 200, and not an error
        } finally {
            pool.shutdownNow();
        }
    }

    // ---- helpers ----

    private ResponseEntity<String> comment(String username, long taskId, String text) {
        return post("/tasks/" + taskId + "/comments", sessionOf(username), Map.of("content", text));
    }

    private long idOf(ResponseEntity<String> created) {
        return ((Number) json(created, "$.id")).longValue();
    }

    private String contentOf(long commentId) {
        return jdbc.queryForObject("SELECT content FROM task_comment WHERE id = ?", String.class, commentId);
    }

    private long authorOf(long commentId) {
        return jdbc.queryForObject("SELECT author_id FROM task_comment WHERE id = ?", Long.class, commentId);
    }
}
