package com.matriz.members;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
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
 * Removing a member who has open tasks (R17, P24): scenarios 1.12 to 1.14 and 1.24 to 1.28, EC15,
 * EC18 and EC41, and the assumptions A21 to A23, through real HTTP requests. Each test sets up the
 * open tasks it needs: it closes what the person had and creates the number of open tasks wanted.
 */
class MemberRemovalIntegrationTest extends ApiIntegrationTest {

    // ---- with a substitute (1.12, 1.24, 1.26, 1.27) ----

    @Test
    void theMembersOpenTaskPassesToASubstituteWhoIsAlreadyInTheProject() { // 1.12
        long website = projectId("Website");
        long design = taskId("Design homepage"); // Ana's only open task

        ResponseEntity<String> removed = remove(website, "ana", reassign(design, "rui"));

        assertThat(removed.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        assertThat(isMember(website, "ana")).isFalse();
        assertThat(assigneeNames(design)).containsExactly("Rui");
        // her closed task keeps her as an assignee: it is history (A22)
        assertThat(assigneeNames(taskId("Draft wireframes"))).containsExactly("Ana");
    }

    @Test
    void aSubstituteWhoIsNotInTheProjectIsAddedToIt() { // 1.12, A7
        long website = projectId("Website");
        long design = taskId("Design homepage");

        ResponseEntity<String> removed = remove(website, "ana", reassign(design, "diana")); // Diana is free

        assertThat(removed.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        assertThat(isMember(website, "diana")).isTrue();
        assertThat(isMember(website, "ana")).isFalse();
        assertThat(assigneeNames(design)).containsExactly("Diana");
    }

    @Test
    void aSubstituteCanAlreadyHaveTasksAndTakesTheMembersTasksOnTopOfThem() { // 1.24
        long website = projectId("Website");
        openTasks(website, "ana", 2);
        openTasks(website, "rui", 1);

        ResponseEntity<String> removed = remove(website, "ana", reassignAllOpenTasksOf("ana", "rui"));

        assertThat(removed.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        assertThat(isMember(website, "ana")).isFalse();
        assertThat(openCount(website, "rui")).isEqualTo(3);
    }

    @Test
    void theTasksCanBeSplitAmongSeveralSubstitutes() { // 1.26
        long website = projectId("Website");
        openTasks(website, "ana", 4);
        openTasks(website, "rui", 3);
        openTasks(website, "luis", 3);
        List<Long> tasks = openTaskIds("ana");

        ResponseEntity<String> removed = remove(website, "ana", List.of(
                Map.of("taskId", tasks.get(0), "newAssigneeId", userId("rui")),
                Map.of("taskId", tasks.get(1), "newAssigneeId", userId("rui")),
                Map.of("taskId", tasks.get(2), "newAssigneeId", userId("luis")),
                Map.of("taskId", tasks.get(3), "newAssigneeId", userId("luis"))));

        assertThat(removed.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        assertThat(openCount(website, "rui")).isEqualTo(5);
        assertThat(openCount(website, "luis")).isEqualTo(5);
    }

    @Test
    void aSharedTaskKeepsItsOtherAssigneesAndGetsTheSubstitute() { // 1.27
        long website = projectId("Website");
        openTasks(website, "ana", 0);
        long report = newTask(website, "Write report", null, "ana", "rui");

        ResponseEntity<String> noSubstitute = remove(website, "ana", List.of());
        ResponseEntity<String> removed = remove(website, "ana", reassign(report, "luis"));

        assertThat(noSubstitute.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
        assertThat(json(noSubstitute, "$.code")).isEqualTo("REPLACEMENT_REQUIRED");
        assertThat(removed.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        assertThat(assigneeNames(report)).containsExactly("Rui", "Luís");
    }

    @Test
    void theManagerOfTheProjectCanBeTheSubstituteWithoutBecomingAMember() { // R21
        long website = projectId("Website");
        long design = taskId("Design homepage");

        ResponseEntity<String> removed = remove(website, "ana", reassign(design, "marta"));

        assertThat(removed.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        assertThat(assigneeNames(design)).containsExactly("Marta");
        assertThat(isMember(website, "marta")).isFalse(); // the manager has no row in his own project (M5)
    }

    @Test
    void aProjectManagerCanBeANewSubstituteEvenIfHeManagesAnotherOpenProject() { // R11 does not apply to managers (1.17)
        long website = projectId("Website");
        long design = taskId("Design homepage");
        // Pedro manages Intranet and is also a member of Mobile App, both open: a normal member would be refused
        post("/projects/" + projectId("Mobile App") + "/members", sessionOf("marta"), Map.of("userId", userId("pedro")));

        ResponseEntity<String> removed = remove(website, "ana", reassign(design, "pedro"));

        assertThat(removed.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        assertThat(isMember(website, "pedro")).isTrue();
        assertThat(assigneeNames(design)).containsExactly("Pedro");
    }

    // ---- refused (1.13, 1.14, 1.25, 1.28, EC15, EC18, EC41) ----

    @Test
    void withoutASubstituteTheRemovalIsNotDone() { // 1.13
        long website = projectId("Website");

        ResponseEntity<String> response = remove(website, "ana", List.of());

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
        assertThat(json(response, "$.code")).isEqualTo("REPLACEMENT_REQUIRED");
        assertThat(isMember(website, "ana")).isTrue();
        assertThat(assigneeNames(taskId("Design homepage"))).containsExactly("Ana");
    }

    @Test
    void everyOpenTaskNeedsASubstituteNotJustSome() { // EC15
        long website = projectId("Website");
        openTasks(website, "ana", 2);
        List<Long> tasks = openTaskIds("ana");

        ResponseEntity<String> response = remove(website, "ana", reassign(tasks.get(0), "rui"));

        assertThat(json(response, "$.code")).isEqualTo("REPLACEMENT_REQUIRED");
        assertThat(isMember(website, "ana")).isTrue();
        assertThat(openCount(website, "rui")).isEqualTo(2); // nothing was moved: the seeded two
    }

    @Test
    void aNewSubstituteWhoIsInAnotherOpenProjectIsRefused() { // 1.14, EC18
        long website = projectId("Website");
        long design = taskId("Design homepage");

        ResponseEntity<String> response = remove(website, "ana", reassign(design, "sofia")); // Sofia is in Intranet

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
        assertThat(json(response, "$.code")).isEqualTo("MEMBER_IN_OTHER_OPEN_PROJECT");
        assertThat(isMember(website, "ana")).isTrue();
        assertThat(isMember(website, "sofia")).isFalse();
        assertThat(assigneeNames(design)).containsExactly("Ana");
    }

    @Test
    void inASuspendedProjectANewSubstituteMayBeInAnotherOpenProject() { // P4, R11
        long website = projectId("Website");
        patch("/projects/" + website, sessionOf("marta"), Map.of("status", "Suspended"));

        ResponseEntity<String> removed = remove(website, "ana", reassign(taskId("Design homepage"), "sofia"));

        assertThat(removed.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        assertThat(isMember(website, "sofia")).isTrue();
    }

    @Test
    void aSubstituteWouldNotHaveMoreThanFiveOpenTasks() { // 1.25, EC41
        long website = projectId("Website");
        openTasks(website, "ana", 3);
        openTasks(website, "rui", 3);

        ResponseEntity<String> response = remove(website, "ana", reassignAllOpenTasksOf("ana", "rui"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
        assertThat(json(response, "$.code")).isEqualTo("ASSIGNEE_LIMIT_EXCEEDED");
        assertThat(isMember(website, "ana")).isTrue();
        assertThat(openCount(website, "ana")).isEqualTo(3);
        assertThat(openCount(website, "rui")).isEqualTo(3);
    }

    @Test
    void aNewSubstituteRefusedByTheLimitIsNotAddedToTheProject() {
        long website = projectId("Website");
        openTasks(website, "ana", 6);

        ResponseEntity<String> response = remove(website, "ana", reassignAllOpenTasksOf("ana", "diana")); // 6 tasks, Diana is new

        assertThat(json(response, "$.code")).isEqualTo("ASSIGNEE_LIMIT_EXCEEDED");
        assertThat(isMember(website, "diana")).isFalse();
        assertThat(isMember(website, "ana")).isTrue();
        assertThat(openCount(website, "ana")).isEqualTo(6);
    }

    @Test
    void aSubstituteWhoIsTheManagerIsAlsoWithinTheLimit() {
        long website = projectId("Website");
        openTasks(website, "ana", 1);
        for (int i = 0; i < 5; i++) {
            newTask(website, "Manager task " + i, null, "marta"); // the manager already has 5
        }

        ResponseEntity<String> response = remove(website, "ana", reassignAllOpenTasksOf("ana", "marta"));

        assertThat(json(response, "$.code")).isEqualTo("ASSIGNEE_LIMIT_EXCEEDED");
        assertThat(isMember(website, "ana")).isTrue();
    }

    @Test
    void subtasksCountForTheLimit() { // 1.28
        long website = projectId("Website");
        openTasks(website, "ana", 1);
        openTasks(website, "rui", 0);
        long parent = newTask(website, "Main", null, "rui");
        newTask(website, "Main 2", null, "rui");
        newTask(website, "Main 3", null, "rui");
        newTask(website, "Main 4", null, "rui");
        newTask(website, "A subtask", parent, "rui"); // Rui: 4 tasks and 1 subtask = 5

        ResponseEntity<String> response = remove(website, "ana", reassignAllOpenTasksOf("ana", "rui"));

        assertThat(json(response, "$.code")).isEqualTo("ASSIGNEE_LIMIT_EXCEEDED");
        assertThat(isMember(website, "ana")).isTrue();
    }

    @Test
    void ifOneSubstituteIsRefusedNothingIsMoved() {
        long website = projectId("Website");
        openTasks(website, "ana", 2);
        openTasks(website, "rui", 0);
        openTasks(website, "luis", 5); // Luís is full
        List<Long> tasks = openTaskIds("ana");

        ResponseEntity<String> response = remove(website, "ana", List.of(
                Map.of("taskId", tasks.get(0), "newAssigneeId", userId("rui")),
                Map.of("taskId", tasks.get(1), "newAssigneeId", userId("luis"))));

        assertThat(json(response, "$.code")).isEqualTo("ASSIGNEE_LIMIT_EXCEEDED");
        assertThat(isMember(website, "ana")).isTrue();
        assertThat(openCount(website, "ana")).isEqualTo(2); // the first move was not kept
        assertThat(openCount(website, "rui")).isZero();
    }

    @Test
    void aSubstituteCannotBeAlreadyResponsibleForTheTaskOrBeThePersonRemoved() { // P24
        long website = projectId("Website");
        openTasks(website, "ana", 0);
        long shared = newTask(website, "Shared", null, "ana", "rui");

        ResponseEntity<String> alreadyAssigned = remove(website, "ana", reassign(shared, "rui"));
        ResponseEntity<String> theSamePerson = remove(website, "ana", reassign(shared, "ana"));
        ResponseEntity<String> nobody = remove(website, "ana", List.of(Map.of("taskId", shared, "newAssigneeId", 999999)));

        for (ResponseEntity<String> r : List.of(alreadyAssigned, theSamePerson, nobody)) {
            assertThat(r.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
            assertThat(json(r, "$.code")).isEqualTo("REPLACEMENT_NOT_ELIGIBLE");
        }
        assertThat(isMember(website, "ana")).isTrue();
    }

    @Test
    void theListMustNameOnlyOpenTasksOfThePersonAndEachOnlyOnce() { // A23
        long website = projectId("Website");
        long design = taskId("Design homepage");

        ResponseEntity<String> notHers = remove(website, "ana", List.of(
                Map.of("taskId", design, "newAssigneeId", userId("rui")),
                Map.of("taskId", taskId("Set up hosting"), "newAssigneeId", userId("rui")))); // Luís's task
        ResponseEntity<String> closed = remove(website, "ana", List.of(
                Map.of("taskId", design, "newAssigneeId", userId("rui")),
                Map.of("taskId", taskId("Draft wireframes"), "newAssigneeId", userId("rui")))); // hers, but Done
        ResponseEntity<String> repeated = remove(website, "ana", List.of(
                Map.of("taskId", design, "newAssigneeId", userId("rui")),
                Map.of("taskId", design, "newAssigneeId", userId("luis"))));

        for (ResponseEntity<String> r : List.of(notHers, closed, repeated)) {
            assertThat(r.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
            assertThat(json(r, "$.code")).isEqualTo("VALIDATION_ERROR");
        }
        assertThat(isMember(website, "ana")).isTrue();
    }

    // ---- at the same time ----

    @Test
    void aRequestThatCannotWorkLocksNobody() throws Exception {
        long website = projectId("Website");
        long design = taskId("Design homepage");
        long sofia = userId("sofia"); // she is in Intranet, which is open: refused by R11
        String marta = sessionOf("marta");
        ExecutorService pool = Executors.newSingleThreadExecutor();
        try {
            // while another operation holds the row of Sofia, the request must be answered at once: it is
            // refused before anybody is locked
            ResponseEntity<String> response = new TransactionTemplate(transactions).execute(tx -> {
                jdbc.queryForObject("SELECT id FROM app_user WHERE id = ? FOR UPDATE", Long.class, sofia);
                Future<ResponseEntity<String>> request = pool.submit(() -> post(
                        "/projects/" + website + "/members/" + userId("ana") + "/removal", marta,
                        Map.of("reassignments", reassign(design, "sofia"))));
                try {
                    return request.get(5, TimeUnit.SECONDS);
                } catch (Exception ex) {
                    throw new AssertionError("The request waited for the lock of a person it did not need", ex);
                }
            });

            assertThat(json(response, "$.code")).isEqualTo("MEMBER_IN_OTHER_OPEN_PROJECT");
        } finally {
            pool.shutdownNow();
        }
    }

    @Test
    void aRemovalAndANewTaskForTheSamePersonAtTheSameTimeAreDoneOneAfterTheOther() throws Exception {
        long website = projectId("Website");
        long diana = userId("diana");
        post("/projects/" + website + "/members", sessionOf("marta"), Map.of("userId", diana)); // a member without tasks
        String marta = sessionOf("marta");
        String ana = sessionOf("ana");
        ExecutorService pool = Executors.newFixedThreadPool(2);
        try {
            List<Future<ResponseEntity<String>>> requests = new TransactionTemplate(transactions).execute(tx -> {
                jdbc.queryForObject("SELECT id FROM app_user WHERE id = ? FOR UPDATE", Long.class, diana);
                Future<ResponseEntity<String>> removal = pool.submit(() -> post(
                        "/projects/" + website + "/members/" + diana + "/removal", marta, Map.of("reassignments", List.of())));
                Future<ResponseEntity<String>> creation = pool.submit(() -> post("/projects/" + website + "/tasks", ana,
                        Map.of("title", "For Diana", "deadline", "2026-12-20", "urgency", 2, "importance", 2,
                                "assigneeIds", List.of(diana))));
                awaitLockWaiters(2);
                return List.of(removal, creation);
            });

            List<Integer> statuses = new ArrayList<>();
            for (Future<ResponseEntity<String>> r : requests) {
                statuses.add(r.get(15, TimeUnit.SECONDS).getStatusCode().value());
            }
            assertThat(statuses).doesNotContain(500); // no deadlock between the two orders of locks
            assertThat(statuses.stream().filter(s -> s < 300)).hasSize(1); // exactly one of them goes through
        } finally {
            pool.shutdownNow();
        }
        // R21: nobody is responsible for an open task of the project without being a member of it
        assertThat(jdbc.queryForObject("""
                SELECT count(*) FROM task_assignee a JOIN task t ON t.id = a.task_id
                WHERE t.project_id = ? AND t.status NOT IN ('Done', 'Cancelled', 'Duplicate')
                  AND a.user_id <> (SELECT manager_id FROM project WHERE id = t.project_id)
                  AND NOT EXISTS (SELECT 1 FROM project_member m WHERE m.project_id = t.project_id AND m.user_id = a.user_id)
                """, Integer.class, website)).isZero();
    }

    // ---- helpers ----

    private ResponseEntity<String> remove(long projectId, String username, List<Map<String, Object>> reassignments) {
        return post("/projects/" + projectId + "/members/" + userId(username) + "/removal", sessionOfManagerOf(projectId),
                Map.of("reassignments", reassignments));
    }

    private String sessionOfManagerOf(long projectId) {
        String manager = jdbc.queryForObject("SELECT u.username FROM project p JOIN app_user u ON u.id = p.manager_id WHERE p.id = ?",
                String.class, projectId);
        return sessionOf(manager);
    }

    private List<Map<String, Object>> reassign(long taskId, String substitute) {
        return List.of(Map.of("taskId", taskId, "newAssigneeId", userId(substitute)));
    }

    private List<Map<String, Object>> reassignAllOpenTasksOf(String username, String substitute) {
        List<Map<String, Object>> list = new ArrayList<>();
        for (Long task : openTaskIds(username)) {
            list.add(Map.of("taskId", task, "newAssigneeId", userId(substitute)));
        }
        return list;
    }

    /** Closes the open tasks that the person has and creates as many open tasks as wanted. */
    private void openTasks(long projectId, String username, int wanted) {
        jdbc.update("UPDATE task SET status = 'Done' WHERE project_id = ? AND status NOT IN ('Done', 'Cancelled', 'Duplicate') "
                + "AND id IN (SELECT task_id FROM task_assignee WHERE user_id = ?)", projectId, userId(username));
        for (int i = 0; i < wanted; i++) {
            newTask(projectId, username + " task " + i, null, username);
        }
    }

    private long newTask(long projectId, String title, Long parentTaskId, String... assignees) {
        long id = jdbc.queryForObject("""
                INSERT INTO task (project_id, parent_task_id, title, status, deadline, urgency, importance)
                VALUES (?, ?, ?, 'To do', DATE '2027-01-15', 2, 2) RETURNING id
                """, Long.class, projectId, parentTaskId, title);
        for (String username : assignees) {
            jdbc.update("INSERT INTO task_assignee (task_id, user_id) VALUES (?, ?)", id, userId(username));
        }
        return id;
    }

    private List<Long> openTaskIds(String username) {
        return jdbc.queryForList("SELECT t.id FROM task t JOIN task_assignee a ON a.task_id = t.id "
                + "WHERE a.user_id = ? AND t.status NOT IN ('Done', 'Cancelled', 'Duplicate') ORDER BY t.id", Long.class, userId(username));
    }

    private int openCount(long projectId, String username) {
        return jdbc.queryForObject("SELECT count(*) FROM task t JOIN task_assignee a ON a.task_id = t.id "
                + "WHERE t.project_id = ? AND a.user_id = ? AND t.status NOT IN ('Done', 'Cancelled', 'Duplicate')",
                Integer.class, projectId, userId(username));
    }

    private boolean isMember(long projectId, String username) {
        return jdbc.queryForObject("SELECT count(*) FROM project_member WHERE project_id = ? AND user_id = ?",
                Integer.class, projectId, userId(username)) == 1;
    }

    private List<String> assigneeNames(long taskId) {
        return jdbc.queryForList("SELECT u.name FROM task_assignee a JOIN app_user u ON u.id = a.user_id "
                + "WHERE a.task_id = ? ORDER BY u.id", String.class, taskId);
    }
}
