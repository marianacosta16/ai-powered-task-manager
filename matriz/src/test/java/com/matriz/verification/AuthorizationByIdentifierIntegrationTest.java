package com.matriz.verification;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import com.matriz.testsupport.ApiIntegrationTest;

/**
 * Exercise 8, case 08 (authorization in the API): someone without permission cannot read or change the resources of
 * another project <b>by changing the identifiers of the request</b>: the id of the project, of the task, of the comment or
 * of a person in the body. Every operation of the contract that takes one of those identifiers is tried with the real ids
 * of a project the actor has no right to, and the answer must be a refusal (403, or 404 when the resource is hidden) and
 * the data must be exactly as it was (a fingerprint of the five tables before and after). The legitimate user is the
 * control: the same requests work for them, so the refusals are not caused by a bad set-up.
 */
class AuthorizationByIdentifierIntegrationTest extends ApiIntegrationTest {

    private static final Set<HttpStatus> REFUSED = Set.of(HttpStatus.FORBIDDEN, HttpStatus.NOT_FOUND);
    private static final Set<HttpStatus> FORBIDDEN = Set.of(HttpStatus.FORBIDDEN);

    private record Attempt(String actor, HttpMethod method, String path, Object body, Set<HttpStatus> allowed) {
        String name() {
            return actor + " " + method + " " + path;
        }
    }

    private long project; // the project of Marta, with Ana and Rui, that the outsiders try to reach
    private long other; // the project of Pedro, with Luís: the outsiders' own
    private long task;
    private long comment;

    private void setUp() {
        project = insertProject("Target", "In progress", "2026-01-01", "2027-12-31", "marta", "ana", "rui");
        other = insertProject("Elsewhere", "In progress", "2026-01-01", "2027-12-31", "pedro", "luis");
        task = insertTask(project, null, "Target task", "To do", "2026-12-15", 3, 3, "ana");
        comment = jdbc.queryForObject("""
                INSERT INTO task_comment (task_id, author_id, content, created_at) VALUES (?, ?, 'A comment of Ana', now()) RETURNING id
                """, Long.class, task, userId("ana"));
    }

    @Test
    void whoIsOutsideTheProjectCannotReachItsResourcesByTheirIdentifiers() {
        setUp();
        List<Attempt> attempts = new ArrayList<>();
        for (String actor : List.of("luis", "pedro")) { // a member of another project, and a manager of another project
            attempts.addAll(everyOperationOn(actor, REFUSED));
        }

        String before = fingerprint();
        SoftAssertions softly = new SoftAssertions();
        for (Attempt attempt : attempts) {
            ResponseEntity<String> response = exchange(attempt.path(), attempt.method(), sessionOf(attempt.actor()), attempt.body());
            softly.assertThat(response.getStatusCode()).as(attempt.name()).isIn(attempt.allowed());
        }
        softly.assertThat(fingerprint()).as("the data after the attempts").isEqualTo(before);
        softly.assertAll();
        assertThat(attempts).hasSizeGreaterThanOrEqualTo(40); // the table is not empty by accident
    }

    @Test
    void aMemberCannotDoWhatOnlyTheManagerCanDoNorTouchTheCommentOfAnother() {
        setUp();
        List<Attempt> attempts = List.of(
                new Attempt("rui", HttpMethod.PATCH, "/projects/" + project, Map.of("name", "Hacked by a member"), FORBIDDEN),
                new Attempt("rui", HttpMethod.DELETE, "/projects/" + project, null, FORBIDDEN),
                new Attempt("rui", HttpMethod.POST, "/projects/" + project + "/members", Map.of("userId", userId("carla")), FORBIDDEN),
                new Attempt("rui", HttpMethod.POST, "/projects/" + project + "/members/" + userId("ana") + "/removal",
                        Map.of("reassignments", List.of()), FORBIDDEN),
                new Attempt("rui", HttpMethod.GET, "/projects/" + project + "/candidate-members", null, FORBIDDEN),
                new Attempt("rui", HttpMethod.POST, "/recycle-bin/projects/" + project + "/restore", null, REFUSED),
                // the comment of Ana: not Rui (a member) and not even Marta (the manager) may change it (A26)
                new Attempt("rui", HttpMethod.PATCH, "/comments/" + comment, Map.of("content", "Hacked by Rui"), FORBIDDEN),
                new Attempt("rui", HttpMethod.DELETE, "/comments/" + comment, null, FORBIDDEN),
                new Attempt("marta", HttpMethod.PATCH, "/comments/" + comment, Map.of("content", "Hacked by the manager"), FORBIDDEN),
                new Attempt("marta", HttpMethod.DELETE, "/comments/" + comment, null, FORBIDDEN));

        String before = fingerprint();
        SoftAssertions softly = new SoftAssertions();
        for (Attempt attempt : attempts) {
            ResponseEntity<String> response = exchange(attempt.path(), attempt.method(), sessionOf(attempt.actor()), attempt.body());
            softly.assertThat(response.getStatusCode()).as(attempt.name()).isIn(attempt.allowed());
        }
        softly.assertThat(fingerprint()).as("the data after the attempts").isEqualTo(before);
        softly.assertAll();
    }

    @Test
    void whoIsNotTheManagerCannotRestoreADeletedProjectOrItsTasks() { // the restore of a project that is in the recycle bin (R9)
        setUp();
        assertThat(delete("/tasks/" + task, sessionOf("ana")).getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        assertThat(delete("/projects/" + project, sessionOf("marta")).getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        List<Attempt> attempts = List.of(
                new Attempt("rui", HttpMethod.POST, "/recycle-bin/projects/" + project + "/restore", null, FORBIDDEN),
                new Attempt("luis", HttpMethod.POST, "/recycle-bin/projects/" + project + "/restore", null, REFUSED),
                new Attempt("pedro", HttpMethod.POST, "/recycle-bin/projects/" + project + "/restore", null, REFUSED),
                new Attempt("luis", HttpMethod.POST, "/recycle-bin/tasks/" + task + "/restore", null, REFUSED));

        String before = fingerprint();
        SoftAssertions softly = new SoftAssertions();
        for (Attempt attempt : attempts) {
            softly.assertThat(exchange(attempt.path(), attempt.method(), sessionOf(attempt.actor()), attempt.body()).getStatusCode())
                    .as(attempt.name()).isIn(attempt.allowed());
        }
        softly.assertThat(fingerprint()).as("the data after the attempts").isEqualTo(before);
        softly.assertAll();
        assertThat(post("/recycle-bin/projects/" + project + "/restore", sessionOf("marta"), null).getStatusCode()) // the control
                .isEqualTo(HttpStatus.NO_CONTENT);
    }

    @Test
    void aTaskOrAPersonOfAnotherProjectCannotBeSlippedInThroughTheBody() {
        setUp();
        String before = fingerprint();

        // Luís manages nothing but is in Pedro's project: he names the task of Marta's project as the parent of a new subtask
        ResponseEntity<String> asParent = post("/projects/" + other + "/tasks", sessionOf("luis"),
                taskBody("Slipped in", userId("luis"), Map.of("parentTaskId", task)));
        // ... and assigns a person who is not in his project
        ResponseEntity<String> strangerAssigned = post("/projects/" + other + "/tasks", sessionOf("luis"),
                taskBody("Stranger assigned", userId("ana"), Map.of()));

        assertThat(asParent.getStatusCode()).isIn(HttpStatus.FORBIDDEN, HttpStatus.NOT_FOUND, HttpStatus.BAD_REQUEST, HttpStatus.UNPROCESSABLE_ENTITY);
        assertThat(strangerAssigned.getStatusCode()).isIn(HttpStatus.BAD_REQUEST, HttpStatus.UNPROCESSABLE_ENTITY);
        assertThat(fingerprint()).isEqualTo(before);
    }

    @Test
    void theLegitimateUsersCanDoTheSameThings() { // the control: the refusals above are not caused by a broken set-up
        setUp();

        assertThat(get("/projects/" + project, sessionOf("marta")).getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(get("/projects/" + project, sessionOf("ana")).getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(get("/tasks/" + task, sessionOf("ana")).getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(get("/tasks/" + task + "/comments", sessionOf("rui")).getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(patch("/comments/" + comment, sessionOf("ana"), Map.of("content", "Edited by the author")).getStatusCode())
                .isEqualTo(HttpStatus.OK);
        assertThat(patch("/projects/" + project, sessionOf("marta"), Map.of("name", "Renamed by the manager")).getStatusCode())
                .isEqualTo(HttpStatus.OK);
    }

    // ---- the attempts ----

    /** Every operation of the contract that takes the id of the project, a task, a comment or a person, on the resources of the target project. */
    private List<Attempt> everyOperationOn(String actor, Set<HttpStatus> allowed) {
        String p = "/projects/" + project;
        String t = "/tasks/" + task;
        long ana = userId("ana");
        long carla = userId("carla");
        long me = userId(actor);
        return List.of(
                new Attempt(actor, HttpMethod.GET, p, null, allowed),
                new Attempt(actor, HttpMethod.PATCH, p, Map.of("name", "Hacked project"), allowed),
                new Attempt(actor, HttpMethod.DELETE, p, null, allowed),
                new Attempt(actor, HttpMethod.GET, p + "/members", null, allowed),
                new Attempt(actor, HttpMethod.POST, p + "/members", Map.of("userId", me), allowed), // adding oneself
                new Attempt(actor, HttpMethod.POST, p + "/members", Map.of("userId", carla), allowed),
                new Attempt(actor, HttpMethod.POST, p + "/members/" + ana + "/removal", Map.of("reassignments", List.of()), allowed),
                new Attempt(actor, HttpMethod.GET, p + "/candidate-members", null, allowed),
                new Attempt(actor, HttpMethod.GET, p + "/assignable-users", null, allowed),
                new Attempt(actor, HttpMethod.GET, p + "/tasks", null, allowed),
                new Attempt(actor, HttpMethod.GET, p + "/tasks?mine=true&status=To%20do", null, allowed),
                new Attempt(actor, HttpMethod.POST, p + "/tasks", taskBody("Injected task", ana, Map.of()), allowed),
                new Attempt(actor, HttpMethod.POST, p + "/tasks", taskBody("Injected subtask", ana, Map.of("parentTaskId", task)), allowed),
                new Attempt(actor, HttpMethod.GET, t, null, allowed),
                new Attempt(actor, HttpMethod.PATCH, t, Map.of("version", 1, "title", "Hacked task"), allowed),
                new Attempt(actor, HttpMethod.PATCH, t, Map.of("version", 1, "status", "Done"), allowed),
                new Attempt(actor, HttpMethod.DELETE, t, null, allowed),
                new Attempt(actor, HttpMethod.POST, t + "/assignees", Map.of("userId", me), allowed), // assigning oneself
                new Attempt(actor, HttpMethod.DELETE, t + "/assignees/" + ana, null, allowed),
                new Attempt(actor, HttpMethod.GET, t + "/comments", null, allowed),
                new Attempt(actor, HttpMethod.POST, t + "/comments", Map.of("content", "Injected comment"), allowed),
                new Attempt(actor, HttpMethod.PATCH, "/comments/" + comment, Map.of("content", "Hacked comment"), allowed),
                new Attempt(actor, HttpMethod.DELETE, "/comments/" + comment, null, allowed),
                new Attempt(actor, HttpMethod.POST, "/recycle-bin/projects/" + project + "/restore", null, allowed),
                new Attempt(actor, HttpMethod.POST, "/recycle-bin/tasks/" + task + "/restore", null, allowed));
    }

    private Map<String, Object> taskBody(String title, long assignee, Map<String, Object> extra) {
        Map<String, Object> body = new HashMap<>();
        body.put("title", title);
        body.put("deadline", "2026-12-15");
        body.put("urgency", 3);
        body.put("importance", 3);
        body.put("assigneeIds", List.of(assignee));
        body.putAll(extra);
        return body;
    }

    /** A fingerprint of everything an attempt could change: the same text before and after means nothing was touched. */
    private String fingerprint() {
        StringBuilder all = new StringBuilder();
        for (String table : List.of("project", "project_member", "task", "task_assignee", "task_comment")) {
            all.append(table).append(':')
                    .append(jdbc.queryForObject("SELECT coalesce(md5(string_agg(x::text, '|' ORDER BY x::text)), '') FROM " + table + " x",
                            String.class))
                    .append(';');
        }
        return all.toString();
    }
}
