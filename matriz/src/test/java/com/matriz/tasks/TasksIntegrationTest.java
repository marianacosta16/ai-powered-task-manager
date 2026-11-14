package com.matriz.tasks;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.support.TransactionTemplate;

import com.jayway.jsonpath.JsonPath;
import com.matriz.testsupport.ApiIntegrationTest;

/**
 * US02, US04 and US05 (scenarios 2.1 to 2.13, 4.1 to 4.5 and 5.1 to 5.4), the rules R1 to R6, R13,
 * R21, R22, R26, R27, P8 and the version of a task (T13, R30), through real HTTP requests.
 * Demo tasks used: Website has "Design homepage" (deadline 2026-12-01, subtasks "Draft wireframes" Done
 * and "Pick colour palette" To do), "Set up hosting" (Blocked) and "Write contact page copy" (Rui and Luís).
 */
class TasksIntegrationTest extends ApiIntegrationTest {

    // ---- creating tasks (2.1 to 2.6, 2.8, 2.9, 2.10, EC10) ----

    @Test
    void aMemberCreatesATaskWithToDoStatusAndItsPriorities() {
        long website = projectId("Website");

        ResponseEntity<String> created = post("/projects/" + website + "/tasks", sessionOf("ana"),
                task("Write report", "2026-12-15", userId("ana")));

        assertThat(created.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(json(created, "$.title")).isEqualTo("Write report");
        assertThat(json(created, "$.status")).isEqualTo("To do");
        assertThat(json(created, "$.urgency")).isEqualTo(3);
        assertThat(json(created, "$.importance")).isEqualTo(4);
        assertThat(json(created, "$.quadrant")).isEqualTo("Do first");
        assertThat(json(created, "$.version")).isEqualTo(1);
        assertThat(JsonPath.<List<String>>read(created.getBody(), "$.assignees[*].name")).containsExactly("Ana");
        assertThat(JsonPath.<List<Object>>read(created.getBody(), "$.subtasks")).isEmpty();
    }

    @Test
    void theDeadlineIsRequired() {
        Map<String, Object> body = task("No deadline", "2026-12-15", userId("ana"));
        body.remove("deadline");

        ResponseEntity<String> response = post("/projects/" + projectId("Website") + "/tasks", sessionOf("ana"), body);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(JsonPath.<List<String>>read(response.getBody(), "$.details[*].field")).contains("deadline");
        assertThat(count("task")).isEqualTo(14);
    }

    @Test
    void aTaskCanHaveSeveralAssignees() {
        ResponseEntity<String> created = post("/projects/" + projectId("Website") + "/tasks", sessionOf("ana"),
                task("Pair work", "2026-12-15", userId("ana"), userId("rui")));

        assertThat(JsonPath.<List<String>>read(created.getBody(), "$.assignees[*].name")).containsExactly("Ana", "Rui");
    }

    @Test
    void theDeadlineIsNotInThePastButTodayIsAccepted() {
        clock.set("2026-11-10T09:00:00Z");
        String ana = sessionOf("ana");
        String path = "/projects/" + projectId("Website") + "/tasks";

        ResponseEntity<String> yesterday = post(path, ana, task("Yesterday", "2026-11-09", userId("ana")));
        ResponseEntity<String> today = post(path, ana, task("Today", "2026-11-10", userId("ana")));

        assertThat(yesterday.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
        assertThat(json(yesterday, "$.code")).isEqualTo("DEADLINE_IN_PAST");
        assertThat(today.getStatusCode()).isEqualTo(HttpStatus.CREATED);
    }

    @Test
    void titleAndDescriptionHaveMaximumLengths() {
        String ana = sessionOf("ana");
        String path = "/projects/" + projectId("Website") + "/tasks";
        Map<String, Object> ok = task("t".repeat(100), "2026-12-15", userId("ana"));
        ok.put("description", "d".repeat(2000));
        assertThat(post(path, ana, ok).getStatusCode()).isEqualTo(HttpStatus.CREATED);

        ResponseEntity<String> longTitle = post(path, ana, task("t".repeat(101), "2026-12-15", userId("ana")));
        Map<String, Object> longDescription = task("Long description", "2026-12-15", userId("ana"));
        longDescription.put("description", "d".repeat(2001));

        assertThat(longTitle.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(JsonPath.<List<String>>read(longTitle.getBody(), "$.details[*].field")).contains("title");
        assertThat(post(path, ana, longDescription).getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void titleAndDescriptionCountCharactersNotUtf16Units() { // R14
        String emoji = "\uD83D\uDC4D"; // one character, two UTF-16 units
        String path = "/projects/" + projectId("Website") + "/tasks";
        String ana = sessionOf("ana");
        Map<String, Object> fits = task(emoji.repeat(100), "2026-12-15", userId("ana"));
        fits.put("description", emoji.repeat(2000));

        ResponseEntity<String> created = post(path, ana, fits);
        ResponseEntity<String> tooLong = post(path, ana, task(emoji.repeat(101), "2026-12-15", userId("ana")));
        ResponseEntity<String> renamed = patch("/tasks/" + taskId("Set up hosting"), ana,
                Map.of("version", 1, "title", emoji.repeat(100)));

        assertThat(created.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(tooLong.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(renamed.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void textIsStoredAndReturnedLiterally() {
        ResponseEntity<String> created = post("/projects/" + projectId("Website") + "/tasks", sessionOf("ana"),
                task("<b>x</b>", "2026-12-15", userId("ana")));

        assertThat(json(created, "$.title")).isEqualTo("<b>x</b>");
        assertThat(jdbc.queryForObject("SELECT title FROM task WHERE id = ?", String.class, ((Number) json(created, "$.id")).longValue()))
                .isEqualTo("<b>x</b>");
    }

    @Test
    void anAssigneeMustBeAMemberOfTheProjectOrItsManager() {
        long website = projectId("Website");
        String path = "/projects/" + website + "/tasks";

        // Sofia is not in Website
        ResponseEntity<String> refused = post(path, sessionOf("ana"), task("For Sofia", "2026-12-15", userId("sofia")));
        // the manager of the project can be assigned (R21)
        ResponseEntity<String> manager = post(path, sessionOf("ana"), task("For Marta", "2026-12-15", userId("marta")));
        Map<String, Object> repeated = task("Twice", "2026-12-15", userId("ana"), userId("ana"));

        assertThat(refused.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
        assertThat(json(refused, "$.code")).isEqualTo("ASSIGNEE_NOT_MEMBER");
        assertThat(manager.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(post(path, sessionOf("ana"), repeated).getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM task WHERE title = 'For Sofia'", Integer.class)).isZero();
    }

    @Test
    void tasksAreNotCreatedInACompletedOrSuspendedProjectUntilItIsResumed() {
        long website = projectId("Website");
        String marta = sessionOf("marta");
        String ana = sessionOf("ana");
        long parent = taskId("Design homepage");
        String path = "/projects/" + website + "/tasks";

        patch("/projects/" + website, marta, Map.of("status", "Suspended"));
        ResponseEntity<String> task = post(path, ana, task("While suspended", "2026-12-15", userId("ana")));
        Map<String, Object> sub = task("Subtask while suspended", "2026-11-30", userId("ana"));
        sub.put("parentTaskId", parent);
        ResponseEntity<String> subtask = post(path, ana, sub);
        ResponseEntity<String> completed = post("/projects/" + projectId("Old Site") + "/tasks", sessionOf("pedro"),
                task("Old news", "2026-12-15", userId("diana")));

        for (ResponseEntity<String> r : List.of(task, subtask, completed)) {
            assertThat(r.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
            assertThat(json(r, "$.code")).isEqualTo("PROJECT_NOT_OPEN");
        }
        patch("/projects/" + website, marta, Map.of("status", "In progress"));
        assertThat(post(path, ana, task("After resuming", "2026-12-15", userId("ana"))).getStatusCode())
                .isEqualTo(HttpStatus.CREATED);
    }

    @Test
    void aMemberOfAnotherProjectGets403EvenWithAnInvalidBody() {
        Map<String, Object> invalid = task("", "2026-12-15", userId("sofia"));

        ResponseEntity<String> response = post("/projects/" + projectId("Website") + "/tasks", sessionOf("sofia"), invalid);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(json(post("/projects/" + projectId("Website") + "/tasks", sessionOf("ana"), invalid), "$.code"))
                .isEqualTo("VALIDATION_ERROR");
    }

    // ---- subtasks and deadlines (4.1 to 4.5) ----

    @Test
    void aSubtaskDeadlineIsNotLaterThanItsParents() {
        long website = projectId("Website");
        long parent = taskId("Design homepage"); // deadline 2026-12-01
        String ana = sessionOf("ana");

        ResponseEntity<String> within = post("/projects/" + website + "/tasks", ana, subtask("Inside", "2026-11-30", parent));
        ResponseEntity<String> later = post("/projects/" + website + "/tasks", ana, subtask("Too late", "2026-12-20", parent));

        assertThat(within.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(json(within, "$.parentTaskId")).isEqualTo((int) parent);
        assertThat(later.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
        assertThat(json(later, "$.code")).isEqualTo("SUBTASK_DEADLINE_AFTER_PARENT");
    }

    @Test
    void theParentDeadlineCannotBeMovedBeforeASubtask() {
        long parent = taskId("Design homepage"); // subtasks end on 2026-11-20 and 2026-11-25

        ResponseEntity<String> response = patch("/tasks/" + parent, sessionOf("ana"),
                Map.of("version", 1, "deadline", "2026-11-22"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
        assertThat(json(response, "$.code")).isEqualTo("PARENT_DEADLINE_BEFORE_SUBTASK");
        assertThat(deadline(parent)).isEqualTo("2026-12-01");
    }

    @Test
    void aSubtaskHasItsOwnStatusAndPriorities() {
        long website = projectId("Website");
        long parent = taskId("Write contact page copy"); // To do, urgency 2, importance 3
        String ana = sessionOf("ana");
        Map<String, Object> body = subtask("Own priorities", "2027-01-10", parent);
        body.put("urgency", 4);
        body.put("importance", 3);

        ResponseEntity<String> created = post("/projects/" + website + "/tasks", ana, body);
        long sub = ((Number) json(created, "$.id")).longValue();
        ResponseEntity<String> started = patch("/tasks/" + sub, ana, Map.of("version", 1, "status", "In progress"));

        assertThat(json(started, "$.status")).isEqualTo("In progress");
        assertThat(json(started, "$.urgency")).isEqualTo(4);
        assertThat(json(started, "$.importance")).isEqualTo(3);
        ResponseEntity<String> parentView = get("/tasks/" + parent, ana);
        assertThat(json(parentView, "$.status")).isEqualTo("To do");
        assertThat(json(parentView, "$.urgency")).isEqualTo(2);
        assertThat(json(parentView, "$.importance")).isEqualTo(3);
    }

    @Test
    void editingTheDeadlineOfASubtaskKeepsItWithinTheParent() {
        long sub = taskId("Pick colour palette"); // 2026-11-25, parent ends on 2026-12-01

        ResponseEntity<String> response = patch("/tasks/" + sub, sessionOf("ana"), Map.of("version", 1, "deadline", "2026-12-20"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
        assertThat(json(response, "$.code")).isEqualTo("SUBTASK_DEADLINE_AFTER_PARENT");
        assertThat(deadline(sub)).isEqualTo("2026-11-25");
    }

    @Test
    void editingTheDeadlineToThePastIsRefused() {
        clock.set("2026-11-10T09:00:00Z");
        long copy = taskId("Write contact page copy"); // 2027-01-15

        ResponseEntity<String> response = patch("/tasks/" + copy, sessionOf("rui"), Map.of("version", 1, "deadline", "2026-11-09"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
        assertThat(json(response, "$.code")).isEqualTo("DEADLINE_IN_PAST");
        assertThat(deadline(copy)).isEqualTo("2027-01-15");
    }

    @Test
    void aSubtaskHasNoSubtasksAndItsParentIsInTheSameOpenTask() {
        long website = projectId("Website");
        String ana = sessionOf("ana");

        ResponseEntity<String> ofSubtask = post("/projects/" + website + "/tasks", ana,
                subtask("Third level", "2026-11-21", taskId("Draft wireframes")));
        ResponseEntity<String> otherProject = post("/projects/" + website + "/tasks", ana,
                subtask("Wrong project", "2026-12-20", taskId("Define app navigation")));
        ResponseEntity<String> closedParent = post("/projects/" + website + "/tasks", ana,
                subtask("Under a closed task", "2027-01-10", taskId("Contact page text"))); // a duplicate: closed

        assertThat(json(ofSubtask, "$.code")).isEqualTo("PARENT_IS_SUBTASK");
        assertThat(otherProject.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(closedParent.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
        assertThat(json(closedParent, "$.code")).isEqualTo("PARENT_TASK_CLOSED");
    }

    // ---- closing tasks (5.1 to 5.4, P8, R6) ----

    @Test
    void aParentWithOpenSubtasksCannotBeCompletedOrCancelled() {
        long parent = taskId("Design homepage"); // "Pick colour palette" is still To do
        String ana = sessionOf("ana");

        ResponseEntity<String> done = patch("/tasks/" + parent, ana, Map.of("version", 1, "status", "Done"));
        ResponseEntity<String> cancelled = patch("/tasks/" + parent, ana, Map.of("version", 1, "status", "Cancelled"));

        for (ResponseEntity<String> r : List.of(done, cancelled)) {
            assertThat(r.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
            assertThat(json(r, "$.code")).isEqualTo("PARENT_HAS_OPEN_SUBTASKS");
        }
        assertThat(status(parent)).isEqualTo("In progress");
    }

    @Test
    void aParentWhoseSubtasksAreAllClosedCanBeCompleted() {
        long website = projectId("Website");
        long parent = taskId("Design homepage");
        String ana = sessionOf("ana");
        // "Draft wireframes" is Done; cancel the second one and make a third one a duplicate of the first
        patch("/tasks/" + taskId("Pick colour palette"), ana, Map.of("version", 1, "status", "Cancelled"));
        long third = ((Number) json(post("/projects/" + website + "/tasks", ana, subtask("Third", "2026-11-28", parent)), "$.id")).longValue();
        patch("/tasks/" + third, ana, Map.of("version", 1, "status", "Duplicate", "originalTaskId", taskId("Draft wireframes")));

        ResponseEntity<String> done = patch("/tasks/" + parent, ana, Map.of("version", 1, "status", "Done"));

        assertThat(done.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(json(done, "$.status")).isEqualTo("Done");
    }

    @Test
    void anyMemberMovesTasksAndSubtasksToFinalStatuses() {
        String rui = sessionOf("rui");

        ResponseEntity<String> subtask = patch("/tasks/" + taskId("Pick colour palette"), rui, Map.of("version", 1, "status", "Done"));
        ResponseEntity<String> task = patch("/tasks/" + taskId("Set up hosting"), sessionOf("luis"), Map.of("version", 1, "status", "Cancelled"));

        assertThat(subtask.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(task.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(json(task, "$.blockReason")).isNull(); // leaving Blocked clears the reason
    }

    @Test
    void onlyTheManagerReopensAClosedTask() {
        long done = taskId("Draft wireframes");

        ResponseEntity<String> byMember = patch("/tasks/" + done, sessionOf("rui"), Map.of("version", 1, "status", "In progress"));
        ResponseEntity<String> byManager = patch("/tasks/" + done, sessionOf("marta"), Map.of("version", 1, "status", "In progress"));

        assertThat(byMember.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(byManager.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(json(byManager, "$.status")).isEqualTo("In progress");
    }

    @Test
    void aBlockedTaskNeedsAReasonOfUpTo100Characters() {
        long copy = taskId("Write contact page copy");
        String rui = sessionOf("rui");

        ResponseEntity<String> noReason = patch("/tasks/" + copy, rui, Map.of("version", 1, "status", "Blocked"));
        Map<String, Object> tooLong = new HashMap<>(Map.of("version", 1, "status", "Blocked"));
        tooLong.put("blockReason", "r".repeat(101));
        ResponseEntity<String> long101 = patch("/tasks/" + copy, rui, tooLong);
        Map<String, Object> exact = new HashMap<>(Map.of("version", 1, "status", "Blocked"));
        exact.put("blockReason", "r".repeat(100));
        ResponseEntity<String> exact100 = patch("/tasks/" + copy, rui, exact);

        assertThat(noReason.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
        assertThat(json(noReason, "$.code")).isEqualTo("BLOCK_REASON_REQUIRED");
        assertThat(long101.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(JsonPath.<List<String>>read(long101.getBody(), "$.details[*].field")).contains("blockReason");
        assertThat(exact100.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(status(copy)).isEqualTo("Blocked");
    }

    @Test
    void aDuplicateNeedsAValidOriginalFromTheSameProject() {
        long copy = taskId("Write contact page copy");
        String rui = sessionOf("rui");
        Map<String, Object> noOriginal = Map.of("version", 1, "status", "Duplicate");

        ResponseEntity<String> missing = patch("/tasks/" + copy, rui, noOriginal);
        ResponseEntity<String> itself = patch("/tasks/" + copy, rui, Map.of("version", 1, "status", "Duplicate", "originalTaskId", copy));
        ResponseEntity<String> otherProject = patch("/tasks/" + copy, rui,
                Map.of("version", 1, "status", "Duplicate", "originalTaskId", taskId("Define app navigation")));
        ResponseEntity<String> aDuplicate = patch("/tasks/" + copy, rui,
                Map.of("version", 1, "status", "Duplicate", "originalTaskId", taskId("Contact page text")));
        ResponseEntity<String> valid = patch("/tasks/" + copy, rui,
                Map.of("version", 1, "status", "Duplicate", "originalTaskId", taskId("Set up hosting")));

        assertThat(json(missing, "$.code")).isEqualTo("ORIGINAL_TASK_REQUIRED");
        for (ResponseEntity<String> r : List.of(itself, otherProject, aDuplicate)) {
            assertThat(r.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
            assertThat(json(r, "$.code")).isEqualTo("ORIGINAL_TASK_NOT_VALID");
        }
        assertThat(valid.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(json(valid, "$.originalTaskId")).isEqualTo((int) taskId("Set up hosting"));
    }

    // ---- assignees (2.9, 2.11, R21, R26) ----

    @Test
    void aPersonOutsideTheProjectIsAssignedOnlyAfterBeingAddedToIt() {
        long website = projectId("Website");
        long copy = taskId("Write contact page copy");
        String ana = sessionOf("ana");
        String marta = sessionOf("marta");

        ResponseEntity<String> refused = post("/tasks/" + copy + "/assignees", ana, Map.of("userId", userId("diana")));
        post("/projects/" + website + "/members", marta, Map.of("userId", userId("diana"))); // Diana is free
        ResponseEntity<String> accepted = post("/tasks/" + copy + "/assignees", ana, Map.of("userId", userId("diana")));

        assertThat(refused.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
        assertThat(json(refused, "$.code")).isEqualTo("ASSIGNEE_NOT_MEMBER");
        assertThat(accepted.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(JsonPath.<List<String>>read(accepted.getBody(), "$[*].name")).containsExactly("Rui", "Luís", "Diana");
    }

    @Test
    void theLastAssigneeCannotBeRemoved() {
        long copy = taskId("Write contact page copy"); // Rui and Luís
        String ana = sessionOf("ana");

        ResponseEntity<String> first = delete("/tasks/" + copy + "/assignees/" + userId("luis"), ana);
        ResponseEntity<String> last = delete("/tasks/" + copy + "/assignees/" + userId("rui"), ana);

        assertThat(first.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        assertThat(last.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
        assertThat(json(last, "$.code")).isEqualTo("LAST_ASSIGNEE");
        assertThat(assigneeNames(copy)).containsExactly("Rui");
        assertThat(delete("/tasks/" + copy + "/assignees/" + userId("sofia"), ana).getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void theAssignableUsersAreTheMembersAndTheManager() {
        ResponseEntity<String> response = get("/projects/" + projectId("Website") + "/assignable-users", sessionOf("ana"));

        assertThat(JsonPath.<List<String>>read(response.getBody(), "$[*].name")).containsExactly("Marta", "Ana", "Rui", "Luís");
        assertThat(get("/projects/" + projectId("Website") + "/assignable-users", sessionOf("sofia")).getStatusCode())
                .isEqualTo(HttpStatus.FORBIDDEN);
    }

    // ---- project states (2.13) ----

    @Test
    void inASuspendedProjectTasksAreEditedAndChangedButInACompletedOneNothingChanges() {
        long website = projectId("Website");
        patch("/projects/" + website, sessionOf("marta"), Map.of("status", "Suspended"));
        String rui = sessionOf("rui");
        long copy = taskId("Write contact page copy");

        // suspended: edit, change the status and the assignees are accepted
        assertThat(patch("/tasks/" + copy, rui, Map.of("version", 1, "title", "Contact page copy v2")).getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(patch("/tasks/" + copy, rui, Map.of("version", 2, "status", "In progress")).getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(delete("/tasks/" + copy + "/assignees/" + userId("luis"), rui).getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);

        // completed (Old Site): the data is visible, every change is refused
        long archive = taskId("Archive old pages");
        String diana = sessionOf("diana");
        assertThat(get("/tasks/" + archive, diana).getStatusCode()).isEqualTo(HttpStatus.OK);
        List<ResponseEntity<String>> refused = List.of(
                patch("/tasks/" + archive, diana, Map.of("version", 1, "title", "Changed")),
                patch("/tasks/" + archive, diana, Map.of("version", 1, "status", "In progress")),
                post("/tasks/" + archive + "/assignees", sessionOf("pedro"), Map.of("userId", userId("diana"))),
                delete("/tasks/" + archive + "/assignees/" + userId("diana"), diana));
        for (ResponseEntity<String> r : refused) {
            assertThat(r.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
            assertThat(json(r, "$.code")).isEqualTo("PROJECT_COMPLETED");
        }
        assertThat(jdbc.queryForObject("SELECT title FROM task WHERE id = ?", String.class, archive)).isEqualTo("Archive old pages");
    }

    // ---- reading tasks (getTask) and access ----

    @Test
    void aMainTaskListsItsSubtasksAndASubtaskDoesNot() {
        ResponseEntity<String> parent = get("/tasks/" + taskId("Design homepage"), sessionOf("rui"));
        ResponseEntity<String> subtask = get("/tasks/" + taskId("Draft wireframes"), sessionOf("rui"));

        assertThat(JsonPath.<List<String>>read(parent.getBody(), "$.subtasks[*].title")).containsExactly("Draft wireframes", "Pick colour palette");
        assertThat(JsonPath.<List<String>>read(parent.getBody(), "$.subtasks[*].status")).containsExactly("Done", "To do");
        assertThat(subtask.getBody()).doesNotContain("\"subtasks\"");
        assertThat(json(subtask, "$.parentTaskId")).isEqualTo((int) taskId("Design homepage"));
    }

    @Test
    void peopleOutsideTheProjectAndMissingTasksAreRefused() {
        long copy = taskId("Write contact page copy");

        assertThat(get("/tasks/" + copy, sessionOf("sofia")).getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(patch("/tasks/" + copy, sessionOf("sofia"), Map.of("version", 1, "title", "X")).getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(post("/tasks/" + copy + "/assignees", sessionOf("sofia"), Map.of("userId", userId("sofia"))).getStatusCode())
                .isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(get("/tasks/" + copy, null).getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(get("/tasks/999999", sessionOf("ana")).getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(get("/tasks/abc", sessionOf("ana")).getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void anUpdateBodyIsCheckedAfterTheAccess() {
        long copy = taskId("Write contact page copy");
        String rui = sessionOf("rui");

        for (Map<String, Object> body : List.<Map<String, Object>>of(
                Map.of("title", "No version"), Map.of("version", 1), Map.of("version", 1, "colour", "red"),
                Map.of("version", 1, "urgency", 5), Map.of("version", 1, "title", ""), Map.of("version", 1, "status", "Party"),
                Map.of("version", 1, "deadline", "15/12/2026"))) {
            ResponseEntity<String> response = patch("/tasks/" + copy, rui, body);
            assertThat(response.getStatusCode()).as(body.toString()).isEqualTo(HttpStatus.BAD_REQUEST);
            assertThat(json(response, "$.code")).isEqualTo("VALIDATION_ERROR");
        }
        assertThat(version(copy)).isEqualTo(1);
    }

    // ---- simultaneous edits (T13, R30, EC43) ----

    @Test
    void aSaveFromAnOldVersionIsRefusedAndNothingIsLost() {
        long copy = taskId("Write contact page copy");
        String ana = sessionOf("ana");
        String rui = sessionOf("rui");
        assertThat(json(get("/tasks/" + copy, ana), "$.version")).isEqualTo(1);

        ResponseEntity<String> first = patch("/tasks/" + copy, ana, Map.of("version", 1, "title", "By Ana"));
        ResponseEntity<String> stale = patch("/tasks/" + copy, rui, Map.of("version", 1, "title", "By Rui"));
        ResponseEntity<String> fresh = patch("/tasks/" + copy, rui, Map.of("version", 2, "title", "By Rui, after reloading"));

        assertThat(first.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(json(first, "$.version")).isEqualTo(2);
        assertThat(stale.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(json(stale, "$.code")).isEqualTo("TASK_CHANGED");
        assertThat(fresh.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(version(copy)).isEqualTo(3);
        assertThat(jdbc.queryForObject("SELECT title FROM task WHERE id = ?", String.class, copy)).isEqualTo("By Rui, after reloading");
    }

    @Test
    void twoSavesOfTheSameVersionAtTheSameTimeLetOnlyOneThrough() throws Exception {
        long copy = taskId("Write contact page copy");
        String ana = sessionOf("ana");
        String rui = sessionOf("rui");
        ExecutorService pool = Executors.newFixedThreadPool(2);
        try {
            // both requests arrive while the row is locked and are then released together
            List<Future<ResponseEntity<String>>> requests = new TransactionTemplate(transactions).execute(tx -> {
                jdbc.queryForObject("SELECT id FROM task WHERE id = ? FOR UPDATE", Long.class, copy);
                Future<ResponseEntity<String>> a = pool.submit(() -> patch("/tasks/" + copy, ana, Map.of("version", 1, "title", "By Ana")));
                Future<ResponseEntity<String>> b = pool.submit(() -> patch("/tasks/" + copy, rui, Map.of("version", 1, "title", "By Rui")));
                awaitLockWaiters(2);
                return List.of(a, b);
            });

            List<HttpStatus> statuses = new ArrayList<>();
            for (Future<ResponseEntity<String>> r : requests) {
                statuses.add(HttpStatus.valueOf(r.get(15, TimeUnit.SECONDS).getStatusCode().value()));
            }
            assertThat(statuses).containsExactlyInAnyOrder(HttpStatus.OK, HttpStatus.CONFLICT);
        } finally {
            pool.shutdownNow();
        }
        assertThat(version(copy)).isEqualTo(2);
    }

    @Test
    void twoRemovalsAtTheSameTimeCannotLeaveATaskWithoutAssignees() throws Exception {
        long copy = taskId("Write contact page copy"); // Rui and Luís
        String ana = sessionOf("ana");
        ExecutorService pool = Executors.newFixedThreadPool(2);
        try {
            List<Future<ResponseEntity<String>>> requests = new TransactionTemplate(transactions).execute(tx -> {
                jdbc.queryForObject("SELECT id FROM task WHERE id = ? FOR UPDATE", Long.class, copy);
                Future<ResponseEntity<String>> a = pool.submit(() -> delete("/tasks/" + copy + "/assignees/" + userId("rui"), ana));
                Future<ResponseEntity<String>> b = pool.submit(() -> delete("/tasks/" + copy + "/assignees/" + userId("luis"), ana));
                awaitLockWaiters(2);
                return List.of(a, b);
            });

            List<HttpStatus> statuses = new ArrayList<>();
            for (Future<ResponseEntity<String>> r : requests) {
                statuses.add(HttpStatus.valueOf(r.get(15, TimeUnit.SECONDS).getStatusCode().value()));
            }
            assertThat(statuses).containsExactlyInAnyOrder(HttpStatus.NO_CONTENT, HttpStatus.UNPROCESSABLE_ENTITY);
        } finally {
            pool.shutdownNow();
        }
        assertThat(assigneeNames(copy)).hasSize(1);
    }

    // ---- found by the code review of this increment ----

    @Test
    void aSubtaskIsNotReopenedUnderAClosedParent() {
        long parent = taskId("Design homepage");
        long wireframes = taskId("Draft wireframes"); // Done
        String ana = sessionOf("ana");
        String marta = sessionOf("marta");
        // close the other subtask and then the parent
        patch("/tasks/" + taskId("Pick colour palette"), ana, Map.of("version", 1, "status", "Cancelled"));
        patch("/tasks/" + parent, ana, Map.of("version", 1, "status", "Done"));

        ResponseEntity<String> refused = patch("/tasks/" + wireframes, marta, Map.of("version", 1, "status", "In progress"));
        patch("/tasks/" + parent, marta, Map.of("version", version(parent), "status", "In progress")); // reopen the parent first
        ResponseEntity<String> accepted = patch("/tasks/" + wireframes, marta, Map.of("version", 1, "status", "In progress"));

        assertThat(refused.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
        assertThat(json(refused, "$.code")).isEqualTo("PARENT_TASK_CLOSED");
        assertThat(accepted.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void anUpdateThatChangesNothingKeepsTheVersion() {
        long copy = taskId("Write contact page copy");
        String ana = sessionOf("ana");

        ResponseEntity<String> same = patch("/tasks/" + copy, ana, Map.of("version", 1, "title", "Write contact page copy", "urgency", 2));
        // Rui read version 1 too, and his real change is not refused because of the no-op
        ResponseEntity<String> real = patch("/tasks/" + copy, sessionOf("rui"), Map.of("version", 1, "title", "A real change"));
        ResponseEntity<String> staleNoOp = patch("/tasks/" + copy, ana, Map.of("version", 1, "title", "A real change"));

        assertThat(same.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(json(same, "$.version")).isEqualTo(1);
        assertThat(real.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(json(real, "$.version")).isEqualTo(2);
        assertThat(staleNoOp.getStatusCode()).isEqualTo(HttpStatus.CONFLICT); // an old version is still refused
    }

    @Test
    void assigningSomeoneWhoIsAlreadyAnAssigneeChangesNothing() { // A16
        long copy = taskId("Write contact page copy"); // Rui and Luís

        ResponseEntity<String> again = post("/tasks/" + copy + "/assignees", sessionOf("ana"), Map.of("userId", userId("rui")));

        assertThat(again.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(JsonPath.<List<String>>read(again.getBody(), "$[*].name")).containsExactly("Rui", "Luís");
        assertThat(assigneeNames(copy)).containsExactly("Rui", "Luís");
    }

    @Test
    void theReasonAndTheOriginalBelongOnlyToBlockedAndDuplicateTasks() { // A17
        long copy = taskId("Write contact page copy");
        String rui = sessionOf("rui");

        ResponseEntity<String> reason = patch("/tasks/" + copy, rui,
                Map.of("version", 1, "status", "In progress", "blockReason", "Waiting for access"));
        ResponseEntity<String> original = patch("/tasks/" + copy, rui,
                Map.of("version", 1, "status", "In progress", "originalTaskId", taskId("Set up hosting")));
        // "Contact page text" is a Duplicate of the copy task; the manager reopens it and the original is cleared
        ResponseEntity<String> reopened = patch("/tasks/" + taskId("Contact page text"), sessionOf("marta"),
                Map.of("version", 1, "status", "To do"));

        for (ResponseEntity<String> r : List.of(reason, original)) {
            assertThat(r.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
            assertThat(json(r, "$.code")).isEqualTo("VALIDATION_ERROR");
        }
        assertThat(JsonPath.<List<String>>read(reason.getBody(), "$.details[*].field")).containsExactly("blockReason");
        assertThat(JsonPath.<List<String>>read(original.getBody(), "$.details[*].field")).containsExactly("originalTaskId");
        assertThat(reopened.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(json(reopened, "$.originalTaskId")).isNull();
        assertThat(status(copy)).isEqualTo("To do");
    }

    @Test
    void anUnchangedDeadlineThatIsNowInThePastIsNotAnError() { // A18
        clock.set("2027-02-01T10:00:00Z"); // after the deadline 2027-01-15
        long copy = taskId("Write contact page copy");

        ResponseEntity<String> sameDeadline = patch("/tasks/" + copy, sessionOf("rui"),
                Map.of("version", 1, "title", "Still fine", "deadline", "2027-01-15"));
        ResponseEntity<String> newPastDeadline = patch("/tasks/" + copy, sessionOf("rui"),
                Map.of("version", 2, "deadline", "2027-01-20"));

        assertThat(sameDeadline.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(newPastDeadline.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
        assertThat(json(newPastDeadline, "$.code")).isEqualTo("DEADLINE_IN_PAST");
    }

    @Test
    void aMemberCannotBlockATaskOfAnotherProjectByUsingItAsAParent() throws Exception {
        long foreign = taskId("Define app navigation"); // a task of Mobile App
        long website = projectId("Website");
        String ana = sessionOf("ana"); // a member of Website only
        ExecutorService pool = Executors.newSingleThreadExecutor();
        try {
            // while another operation holds the row of that task, the request must be answered at once: it is
            // refused before the row is locked
            ResponseEntity<String> response = new TransactionTemplate(transactions).execute(tx -> {
                jdbc.queryForObject("SELECT id FROM task WHERE id = ? FOR UPDATE", Long.class, foreign);
                Future<ResponseEntity<String>> request = pool.submit(
                        () -> post("/projects/" + website + "/tasks", ana, subtask("Probe", "2026-12-20", foreign)));
                try {
                    return request.get(5, TimeUnit.SECONDS);
                } catch (Exception ex) {
                    throw new AssertionError("The request waited for the lock of a task of another project", ex);
                }
            });

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        } finally {
            pool.shutdownNow();
        }
    }

    @Test
    void assigningAPersonAndRemovingThemFromTheProjectAtTheSameTimeNeverLeavesATaskWithANonMember() throws Exception {
        long website = projectId("Website");
        long diana = userId("diana"); // free
        long copy = taskId("Write contact page copy");
        String marta = sessionOf("marta");
        String ana = sessionOf("ana");
        post("/projects/" + website + "/members", marta, Map.of("userId", diana)); // a member without tasks
        ExecutorService pool = Executors.newFixedThreadPool(2);
        try {
            List<Future<ResponseEntity<String>>> requests = new TransactionTemplate(transactions).execute(tx -> {
                jdbc.queryForObject("SELECT id FROM app_user WHERE id = ? FOR UPDATE", Long.class, diana);
                Future<ResponseEntity<String>> removal = pool.submit(() -> post(
                        "/projects/" + website + "/members/" + diana + "/removal", marta, Map.of("reassignments", List.of())));
                Future<ResponseEntity<String>> assignment = pool.submit(
                        () -> post("/tasks/" + copy + "/assignees", ana, Map.of("userId", diana)));
                awaitLockWaiters(2);
                return List.of(removal, assignment);
            });

            for (Future<ResponseEntity<String>> r : requests) {
                r.get(15, TimeUnit.SECONDS);
            }
        } finally {
            pool.shutdownNow();
        }
        // R21: whoever is assigned to a task of the project is a member of it (or its manager)
        assertThat(jdbc.queryForObject("""
                SELECT count(*) FROM task_assignee a JOIN task t ON t.id = a.task_id
                WHERE t.project_id = ? AND a.user_id = ?
                  AND NOT EXISTS (SELECT 1 FROM project_member m WHERE m.project_id = t.project_id AND m.user_id = a.user_id)
                """, Integer.class, website, diana)).isZero();
    }

    // ---- helpers ----

    /** A valid body to create a task; urgency 3 and importance 4 unless the test changes them. */
    private Map<String, Object> task(String title, String deadline, long... assignees) {
        Map<String, Object> body = new HashMap<>();
        body.put("title", title);
        body.put("deadline", deadline);
        body.put("urgency", 3);
        body.put("importance", 4);
        List<Long> ids = new ArrayList<>();
        for (long id : assignees) {
            ids.add(id);
        }
        body.put("assigneeIds", ids);
        return body;
    }

    private Map<String, Object> subtask(String title, String deadline, long parentTaskId) {
        Map<String, Object> body = task(title, deadline, userId("ana"));
        body.put("parentTaskId", parentTaskId);
        return body;
    }

    private String status(long taskId) {
        return jdbc.queryForObject("SELECT status FROM task WHERE id = ?", String.class, taskId);
    }

    private String deadline(long taskId) {
        return jdbc.queryForObject("SELECT deadline::text FROM task WHERE id = ?", String.class, taskId);
    }

    private int version(long taskId) {
        return jdbc.queryForObject("SELECT version FROM task WHERE id = ?", Integer.class, taskId);
    }

    private List<String> assigneeNames(long taskId) {
        return jdbc.queryForList("SELECT u.name FROM task_assignee a JOIN app_user u ON u.id = a.user_id "
                + "WHERE a.task_id = ? ORDER BY u.id", String.class, taskId);
    }
}
