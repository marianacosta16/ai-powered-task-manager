package com.matriz.usecases;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.support.TransactionTemplate;

import com.matriz.testsupport.ApiIntegrationTest;

/**
 * Writing in a project at the same moment as it is completed or deleted (R27, R28, R9): every way of writing
 * in a project takes the project in shared mode and reads its state with the lock, so the write either happens
 * before the change of state or sees the new state, never in between. In each test the project row is held (as a
 * change of its state would hold it), the request arrives and waits for the lock, and the state changes before the
 * lock is let go. The request must then be refused. Plan section 7, risk "concluir ou apagar um projeto e escrever nele".
 */
class ProjectStateRaceIntegrationTest extends ApiIntegrationTest {

    private static final String COMPLETE = "UPDATE project SET status = 'Completed' WHERE id = ?";
    private static final String DELETE = "UPDATE project SET deleted_at = now() WHERE id = ?";

    // ---- the project is completed while the request waits ----

    @Test
    void aTaskIsNotCreatedInAProjectThatIsCompletedMeanwhile() throws Exception { // R22, R28
        long project = project();
        int before = count("task");

        ResponseEntity<String> response = whileStateChanges(project, COMPLETE, () -> post("/projects/" + project + "/tasks",
                sessionOf("ana"), Map.of("title", "Late", "deadline", "2026-12-01", "urgency", 3, "importance", 3,
                        "assigneeIds", List.of(userId("ana")))));

        assertRefused(response, "PROJECT_NOT_OPEN");
        assertThat(count("task")).isEqualTo(before);
    }

    @Test
    void aTaskIsNotChangedInAProjectThatIsCompletedMeanwhile() throws Exception { // R28
        long project = project();
        long task = task(project, "T");

        ResponseEntity<String> response = whileStateChanges(project, COMPLETE,
                () -> patch("/tasks/" + task, sessionOf("ana"), Map.of("version", 1, "title", "Changed")));

        assertRefused(response, "PROJECT_COMPLETED");
        assertThat(jdbc.queryForObject("SELECT title FROM task WHERE id = ?", String.class, task)).isEqualTo("T");
    }

    @Test
    void anAssigneeIsNotAddedInAProjectThatIsCompletedMeanwhile() throws Exception { // R28
        long project = project();
        long task = task(project, "T");

        ResponseEntity<String> response = whileStateChanges(project, COMPLETE,
                () -> post("/tasks/" + task + "/assignees", sessionOf("ana"), Map.of("userId", userId("rui"))));

        assertRefused(response, "PROJECT_COMPLETED");
        assertThat(assignees(task)).isEqualTo(1);
    }

    @Test
    void anAssigneeIsNotRemovedInAProjectThatIsCompletedMeanwhile() throws Exception { // R28
        long project = project();
        long task = task(project, "T");
        jdbc.update("INSERT INTO task_assignee (task_id, user_id) VALUES (?, ?)", task, userId("rui"));

        ResponseEntity<String> response = whileStateChanges(project, COMPLETE,
                () -> delete("/tasks/" + task + "/assignees/" + userId("rui"), sessionOf("ana")));

        assertRefused(response, "PROJECT_COMPLETED");
        assertThat(assignees(task)).isEqualTo(2);
    }

    @Test
    void aCommentIsNotAddedInAProjectThatIsCompletedMeanwhile() throws Exception { // R28
        long project = project();
        long task = task(project, "T");
        int before = count("task_comment");

        ResponseEntity<String> response = whileStateChanges(project, COMPLETE,
                () -> post("/tasks/" + task + "/comments", sessionOf("ana"), Map.of("content", "Late")));

        assertRefused(response, "PROJECT_COMPLETED");
        assertThat(count("task_comment")).isEqualTo(before);
    }

    @Test
    void aCommentIsNotEditedNorDeletedInAProjectThatIsCompletedMeanwhile() throws Exception { // R28
        long project = project();
        long task = task(project, "T");
        int before = count("task_comment");
        long comment = jdbc.queryForObject("INSERT INTO task_comment (task_id, author_id, content, created_at) VALUES (?, ?, 'Old', now()) RETURNING id",
                Long.class, task, userId("ana"));

        assertRefused(whileStateChanges(project, COMPLETE,
                () -> patch("/comments/" + comment, sessionOf("ana"), Map.of("content", "New"))), "PROJECT_COMPLETED");
        assertThat(jdbc.queryForObject("SELECT content FROM task_comment WHERE id = ?", String.class, comment)).isEqualTo("Old");

        long other = project();
        long otherTask = task(other, "T");
        long otherComment = jdbc.queryForObject("INSERT INTO task_comment (task_id, author_id, content, created_at) VALUES (?, ?, 'Old', now()) RETURNING id",
                Long.class, otherTask, userId("ana"));
        assertRefused(whileStateChanges(other, COMPLETE, () -> delete("/comments/" + otherComment, sessionOf("ana"))),
                "PROJECT_COMPLETED");
        assertThat(count("task_comment")).isEqualTo(before + 2);
    }

    @Test
    void aTaskIsNotDeletedNorRestoredInAProjectThatIsCompletedMeanwhile() throws Exception { // R28
        long project = project();
        long task = task(project, "T");
        assertRefused(whileStateChanges(project, COMPLETE, () -> delete("/tasks/" + task, sessionOf("ana"))), "PROJECT_COMPLETED");
        assertThat(jdbc.queryForObject("SELECT deleted_at IS NULL FROM task WHERE id = ?", Boolean.class, task)).isTrue();

        long other = project();
        long deleted = task(other, "D");
        delete("/tasks/" + deleted, sessionOf("ana"));
        assertRefused(whileStateChanges(other, COMPLETE,
                () -> post("/recycle-bin/tasks/" + deleted + "/restore", sessionOf("ana"), null)), "PROJECT_COMPLETED");
        assertThat(jdbc.queryForObject("SELECT deleted_at IS NOT NULL FROM task WHERE id = ?", Boolean.class, deleted)).isTrue();
    }

    @Test
    void aMemberIsNotAddedNorRemovedInAProjectThatIsCompletedMeanwhile() throws Exception { // R28
        long project = project();

        assertRefused(whileStateChanges(project, COMPLETE,
                () -> post("/projects/" + project + "/members", sessionOf("marta"), Map.of("userId", userId("diana")))),
                "PROJECT_COMPLETED");
        assertThat(jdbc.queryForObject("SELECT count(*) FROM project_member WHERE project_id = ?", Integer.class, project))
                .isEqualTo(3);

        long other = project();
        assertRefused(whileStateChanges(other, COMPLETE,
                () -> post("/projects/" + other + "/members/" + userId("luis") + "/removal", sessionOf("marta"),
                        Map.of("reassignments", List.of()))), "PROJECT_COMPLETED");
        assertThat(jdbc.queryForObject("SELECT count(*) FROM project_member WHERE project_id = ?", Integer.class, other))
                .isEqualTo(3);
    }

    @Test
    void aProjectIsNotChangedNorDeletedWhenItIsCompletedMeanwhile() throws Exception { // R28
        long project = project();
        assertRefused(whileStateChanges(project, COMPLETE,
                () -> patch("/projects/" + project, sessionOf("marta"), Map.of("description", "Late"))), "PROJECT_COMPLETED");
        assertThat(jdbc.queryForObject("SELECT description FROM project WHERE id = ?", String.class, project)).isNull();

        long other = project();
        assertRefused(whileStateChanges(other, COMPLETE, () -> delete("/projects/" + other, sessionOf("marta"))),
                "PROJECT_COMPLETED");
        assertThat(jdbc.queryForObject("SELECT deleted_at IS NULL FROM project WHERE id = ?", Boolean.class, other)).isTrue();
    }

    // ---- the project is deleted while the request waits ----

    @Test
    void nothingIsWrittenInAProjectThatIsDeletedMeanwhile() throws Exception { // R9
        long project = project();
        task(project, "T");
        int comments = count("task_comment");

        assertThat(whileStateChanges(project, DELETE, () -> post("/projects/" + project + "/tasks", sessionOf("ana"),
                Map.of("title", "Late", "deadline", "2026-12-01", "urgency", 3, "importance", 3,
                        "assigneeIds", List.of(userId("ana"))))).getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);

        long other = project();
        long otherTask = task(other, "T");
        assertThat(whileStateChanges(other, DELETE, () -> post("/tasks/" + otherTask + "/comments", sessionOf("ana"),
                Map.of("content", "Late"))).getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);

        assertThat(count("task_comment")).isEqualTo(comments);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM task WHERE project_id = ?", Integer.class, project)).isEqualTo(1);
    }

    // ---- the other way round: the change of state waits for the writes under way ----

    @Test
    void completingAProjectWaitsForTheWritesUnderWayAndThenWins() throws Exception { // R28
        long project = project();
        long task = task(project, "T");
        ExecutorService pool = Executors.newSingleThreadExecutor();
        try {
            // a write is under way (it holds the project in shared mode) when the manager completes the project
            Future<ResponseEntity<String>> completion = new TransactionTemplate(transactions).execute(tx -> {
                jdbc.queryForObject("SELECT id FROM project WHERE id = ? FOR SHARE", Long.class, project);
                Future<ResponseEntity<String>> pending = pool.submit(
                        () -> patch("/projects/" + project, sessionOf("marta"), Map.of("status", "Completed")));
                awaitLockWaiters(1); // the completion waits for the write
                jdbc.update("UPDATE task SET title = 'Written before the end' WHERE id = ?", task);
                return pending;
            });

            assertThat(completion.get(15, TimeUnit.SECONDS).getStatusCode()).isEqualTo(HttpStatus.OK);
        } finally {
            pool.shutdownNow();
        }
        assertThat(jdbc.queryForObject("SELECT title FROM task WHERE id = ?", String.class, task)).isEqualTo("Written before the end");
        assertThat(jdbc.queryForObject("SELECT status FROM project WHERE id = ?", String.class, project)).isEqualTo("Completed");
    }

    @Test
    void severalWritersGoOnTogetherInTheSameProject() throws Exception {
        long project = project();
        task(project, "A");
        long b = task(project, "B");

        // two writers hold the project in shared mode at once: neither waits for the other
        new TransactionTemplate(transactions).executeWithoutResult(tx -> {
            jdbc.queryForObject("SELECT id FROM project WHERE id = ? FOR SHARE", Long.class, project);
            ExecutorService pool = Executors.newSingleThreadExecutor();
            try {
                Future<ResponseEntity<String>> other = pool.submit(
                        () -> patch("/tasks/" + b, sessionOf("ana"), Map.of("version", 1, "title", "B2")));
                assertThat(other.get(15, TimeUnit.SECONDS).getStatusCode()).isEqualTo(HttpStatus.OK);
            } catch (Exception e) {
                throw new AssertionError(e);
            } finally {
                pool.shutdownNow();
            }
        });
    }

    // ---- helpers ----

    /**
     * Holds the project row, sends the request (which waits for it), changes the state of the project with the
     * given statement and lets the lock go; returns the answer to the request.
     */
    private ResponseEntity<String> whileStateChanges(long project, String stateChange, Supplier<ResponseEntity<String>> request)
            throws Exception {
        ExecutorService pool = Executors.newSingleThreadExecutor();
        try {
            Future<ResponseEntity<String>> pending = new TransactionTemplate(transactions).execute(tx -> {
                jdbc.queryForObject("SELECT id FROM project WHERE id = ? FOR UPDATE", Long.class, project);
                Future<ResponseEntity<String>> future = pool.submit(request::get);
                awaitLockWaiters(1);
                jdbc.update(stateChange, project);
                return future;
            });
            return pending.get(15, TimeUnit.SECONDS);
        } finally {
            pool.shutdownNow();
        }
    }

    private static void assertRefused(ResponseEntity<String> response, String code) {
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
        assertThat(json(response, "$.code")).isEqualTo(code);
    }

    /** An open project of Marta with Ana, Rui and Luís, made by hand so that R11 does not get in the way. */
    private long project() {
        return insertProject("Race", "In progress", "2026-01-01", "2027-12-31", "marta", "ana", "rui", "luis");
    }

    private long task(long project, String title) {
        return insertTask(project, null, title, "To do", "2026-12-31", 3, 3, "ana");
    }

    private int assignees(long task) {
        return jdbc.queryForObject("SELECT count(*) FROM task_assignee WHERE task_id = ?", Integer.class, task);
    }
}
