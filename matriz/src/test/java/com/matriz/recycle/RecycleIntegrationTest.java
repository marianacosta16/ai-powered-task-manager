package com.matriz.recycle;

import static org.assertj.core.api.Assertions.assertThat;

import java.sql.Timestamp;
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
import com.matriz.testsupport.ApiIntegrationTest;
import com.matriz.usecases.RecyclePurgeJob;

/**
 * Deleting, the recycle bin and restoring (US07): scenarios 7.1 to 7.8, 11.7 (the second half), EC7, EC21,
 * EC23 and EC36, and P36, through real HTTP requests with a clock that the test moves. Each test builds its
 * own projects; the people are the seeded ones. The projects are made by hand in the database, so that the
 * rule of one open project per member (R11) does not get in the way of the setup.
 */
class RecycleIntegrationTest extends ApiIntegrationTest {

    @Autowired RecyclePurgeJob purgeJob;

    // ---- deleting and restoring a project (7.1, 7.2, EC7) ----

    @Test
    void deletingAProjectSendsItWithItsTasksToTheBin() { // 7.1, EC7
        clock.set("2026-03-01T10:00:00Z");
        long project = newProject("Web", "In progress", "marta", "ana");
        for (int i = 1; i <= 5; i++) {
            newTask(project, "T" + i, "To do", null, "ana");
        }
        newTask(project, "sub", "To do", taskId("T1"), "ana");

        ResponseEntity<String> deleted = delete("/projects/" + project, sessionOf("marta"));

        assertThat(deleted.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        assertThat(get("/projects/" + project, sessionOf("marta")).getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(JsonPath.<List<String>>read(get("/projects", sessionOf("marta")).getBody(), "$[*].name"))
                .noneMatch(n -> n.startsWith("Web#"));
        List<Map<String, Object>> items = bin("marta");
        assertThat(items).hasSize(1); // the tasks go with the project: they are not listed on their own
        assertThat(items.get(0)).containsEntry("itemType", "Project").containsEntry("includedTasks", 5)
                .containsEntry("restorable", true).containsEntry("daysLeft", 30);
        assertThat(bin("ana")).isEmpty(); // a member does not see the project (EC36)
    }

    @Test
    void restoringAProjectBringsBackItsTasksWithTheirStates() { // 7.2
        long project = newProject("Web", "Suspended", "marta", "ana");
        long a = newTask(project, "A", "In progress", null, "ana");
        long b = newTask(project, "B", "Done", null, "ana");
        delete("/projects/" + project, sessionOf("marta"));

        ResponseEntity<String> restored = post("/recycle-bin/projects/" + project + "/restore", sessionOf("marta"), null);

        assertThat(restored.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        assertThat(json(get("/projects/" + project, sessionOf("marta")), "$.status")).isEqualTo("Suspended");
        assertThat(statusOf(a)).isEqualTo("In progress");
        assertThat(statusOf(b)).isEqualTo("Done");
        assertThat(JsonPath.<List<String>>read(get("/projects/" + project + "/tasks", sessionOf("ana")).getBody(), "$[*].title"))
                .containsExactlyInAnyOrder("A", "B");
        assertThat(bin("marta")).isEmpty();
    }

    @Test
    void whatEachRoleSeesAndRestores() { // 7.3, EC36
        long website = newProject("Website", "In progress", "marta", "ana");
        long intranet = newProject("Intranet", "In progress", "marta", "rui");
        long t1 = newTask(website, "T1", "To do", null, "ana");
        delete("/tasks/" + t1, sessionOf("ana"));
        delete("/projects/" + intranet, sessionOf("marta"));

        List<Map<String, Object>> forAna = bin("ana");
        assertThat(forAna).hasSize(1);
        assertThat(forAna.get(0)).containsEntry("itemType", "Task").containsEntry("name", "T1").containsEntry("restorable", true);
        assertThat(post("/recycle-bin/projects/" + intranet + "/restore", sessionOf("ana"), null).getStatusCode())
                .isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(post("/recycle-bin/projects/" + intranet + "/restore", sessionOf("rui"), null).getStatusCode())
                .isEqualTo(HttpStatus.FORBIDDEN); // a member of the deleted project cannot restore it either

        assertThat(post("/recycle-bin/tasks/" + t1 + "/restore", sessionOf("ana"), null).getStatusCode())
                .isEqualTo(HttpStatus.NO_CONTENT);
        assertThat(get("/tasks/" + t1, sessionOf("ana")).getStatusCode()).isEqualTo(HttpStatus.OK);
        List<Map<String, Object>> forMarta = bin("marta");
        assertThat(forMarta).hasSize(1);
        assertThat(forMarta.get(0)).containsEntry("itemType", "Project").containsEntry("restorable", true);
        assertThat(post("/recycle-bin/projects/" + intranet + "/restore", sessionOf("marta"), null).getStatusCode())
                .isEqualTo(HttpStatus.NO_CONTENT);
    }

    @Test
    void aTaskDeletedBeforeTheProjectStaysInTheBinWhenTheProjectComesBack() { // 7.7
        long project = newProject("Web", "In progress", "marta", "ana");
        long t1 = newTask(project, "T1", "To do", null, "ana");
        newTask(project, "T2", "To do", null, "ana");
        newTask(project, "T3", "To do", null, "ana");
        clock.set("2026-03-01T10:00:00Z");
        delete("/tasks/" + t1, sessionOf("ana"));
        clock.set("2026-03-02T10:00:00Z");
        delete("/projects/" + project, sessionOf("marta"));

        List<Map<String, Object>> whileDeleted = bin("marta"); // the project, and T1 that cannot come back yet
        assertThat(whileDeleted).extracting(i -> i.get("itemType")).containsExactlyInAnyOrder("Project", "Task");
        Map<String, Object> inTheBin = whileDeleted.stream().filter(i -> i.get("itemType").equals("Task")).findFirst().orElseThrow();
        assertThat(inTheBin).containsEntry("restorable", false).containsEntry("notRestorableReason", "PROJECT_IN_RECYCLE_BIN");
        ResponseEntity<String> refused = post("/recycle-bin/tasks/" + t1 + "/restore", sessionOf("marta"), null);
        assertThat(refused.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
        assertThat(json(refused, "$.code")).isEqualTo("CANNOT_RESTORE");

        post("/recycle-bin/projects/" + project + "/restore", sessionOf("marta"), null);

        assertThat(JsonPath.<List<String>>read(get("/projects/" + project + "/tasks", sessionOf("ana")).getBody(), "$[*].title"))
                .containsExactly("T2", "T3");
        List<Map<String, Object>> after = bin("ana");
        assertThat(after).hasSize(1);
        assertThat(after.get(0)).containsEntry("name", "T1").containsEntry("restorable", true);
        assertThat(post("/recycle-bin/tasks/" + t1 + "/restore", sessionOf("ana"), null).getStatusCode())
                .isEqualTo(HttpStatus.NO_CONTENT);
    }

    // ---- deleting tasks and subtasks (7.4, 7.5, EC21) ----

    @Test
    void aTaskWithOpenSubtasksIsNotDeleted() { // 7.4, EC21
        long project = newProject("Web", "In progress", "marta", "ana");
        long parent = newTask(project, "Parent", "To do", null, "ana");
        long sub = newTask(project, "Sub", "In progress", parent, "ana");

        ResponseEntity<String> refused = delete("/tasks/" + parent, sessionOf("ana"));

        assertThat(refused.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
        assertThat(json(refused, "$.code")).isEqualTo("PARENT_HAS_OPEN_SUBTASKS");
        assertThat(get("/tasks/" + parent, sessionOf("ana")).getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(bin("ana")).isEmpty();

        jdbc.update("UPDATE task SET status = 'Done' WHERE id = ?", sub); // closed: now the parent can go, with it
        assertThat(delete("/tasks/" + parent, sessionOf("ana")).getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        assertThat(bin("ana")).hasSize(1); // the closed subtask goes with its parent
    }

    @Test
    void aSubtaskIsDeletedAloneAndComesBackAlone() { // 7.5
        long project = newProject("Web", "In progress", "marta", "ana");
        long parent = newTask(project, "Parent", "To do", null, "ana");
        long sub = newTask(project, "Sub", "To do", parent, "ana");

        assertThat(delete("/tasks/" + sub, sessionOf("ana")).getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);

        assertThat(JsonPath.<List<Object>>read(get("/tasks/" + parent, sessionOf("ana")).getBody(), "$.subtasks")).isEmpty();
        List<Map<String, Object>> items = bin("ana");
        assertThat(items).hasSize(1);
        assertThat(items.get(0)).containsEntry("itemType", "Subtask").containsEntry("name", "Sub");
        assertThat(post("/recycle-bin/tasks/" + sub + "/restore", sessionOf("ana"), null).getStatusCode())
                .isEqualTo(HttpStatus.NO_CONTENT);
        assertThat(JsonPath.<List<String>>read(get("/tasks/" + parent, sessionOf("ana")).getBody(), "$.subtasks[*].title"))
                .containsExactly("Sub");
    }

    @Test
    void restoringATaskBringsBackTheSubtasksDeletedWithItAndNotTheOnesDeletedBefore() { // R9
        long project = newProject("Web", "In progress", "marta", "ana");
        long parent = newTask(project, "Parent", "To do", null, "ana");
        long early = newTask(project, "Early", "To do", parent, "ana");
        long late = newTask(project, "Late", "Done", parent, "ana");
        clock.set("2026-03-01T10:00:00Z");
        delete("/tasks/" + early, sessionOf("ana"));
        jdbc.update("UPDATE task SET status = 'Done' WHERE id = ?", parent); // closed, so nothing is open under it
        clock.set("2026-03-02T10:00:00Z");
        delete("/tasks/" + parent, sessionOf("ana"));

        assertThat(bin("ana")).extracting(i -> i.get("name")).containsExactlyInAnyOrder("Early", "Parent");
        post("/recycle-bin/tasks/" + parent + "/restore", sessionOf("ana"), null);

        assertThat(JsonPath.<List<String>>read(get("/tasks/" + parent, sessionOf("ana")).getBody(), "$.subtasks[*].title"))
                .containsExactly("Late"); // Early stays in the bin
        assertThat(bin("ana")).extracting(i -> i.get("name")).containsExactly("Early");
        assertThat(late).isPositive();
    }

    @Test
    void aSubtaskCannotComeBackWhileItsParentIsInTheBin() {
        long project = newProject("Web", "In progress", "marta", "ana");
        long parent = newTask(project, "Parent", "To do", null, "ana");
        long sub = newTask(project, "Sub", "To do", parent, "ana");
        clock.set("2026-03-01T10:00:00Z");
        delete("/tasks/" + sub, sessionOf("ana"));
        jdbc.update("UPDATE task SET status = 'Done' WHERE id = ?", parent);
        clock.set("2026-03-02T10:00:00Z");
        delete("/tasks/" + parent, sessionOf("ana"));

        Map<String, Object> item = bin("ana").stream().filter(i -> i.get("name").equals("Sub")).findFirst().orElseThrow();
        ResponseEntity<String> refused = post("/recycle-bin/tasks/" + sub + "/restore", sessionOf("ana"), null);

        assertThat(item).containsEntry("restorable", false).containsEntry("notRestorableReason", "PARENT_IN_RECYCLE_BIN");
        assertThat(refused.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
        assertThat(json(refused, "$.code")).isEqualTo("CANNOT_RESTORE");
    }

    @Test
    void anOpenSubtaskDoesNotComeBackUnderAParentThatWasClosedMeanwhile() { // P35, R4
        long project = newProject("Web", "In progress", "marta", "ana");
        long parent = newTask(project, "Parent", "To do", null, "ana");
        long sub = newTask(project, "Sub", "In progress", parent, "ana");
        delete("/tasks/" + sub, sessionOf("ana"));
        jdbc.update("UPDATE task SET status = 'Done' WHERE id = ?", parent);

        ResponseEntity<String> refused = post("/recycle-bin/tasks/" + sub + "/restore", sessionOf("ana"), null);

        assertThat(refused.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
        assertThat(json(refused, "$.code")).isEqualTo("PARENT_TASK_CLOSED");
        jdbc.update("UPDATE task SET status = 'To do' WHERE id = ?", parent); // the manager reopens the parent
        assertThat(post("/recycle-bin/tasks/" + sub + "/restore", sessionOf("ana"), null).getStatusCode())
                .isEqualTo(HttpStatus.NO_CONTENT);
    }

    @Test
    void aSubtaskDoesNotComeBackLaterThanItsParentDeadline() { // R2
        long project = newProject("Web", "In progress", "marta", "ana");
        long parent = newTask(project, "Parent", "To do", null, "ana");
        long sub = newTask(project, "Sub", "To do", parent, "ana");
        delete("/tasks/" + sub, sessionOf("ana"));
        jdbc.update("UPDATE task SET deadline = DATE '2026-11-01' WHERE id = ?", parent); // earlier than the subtask's

        ResponseEntity<String> refused = post("/recycle-bin/tasks/" + sub + "/restore", sessionOf("ana"), null);

        assertThat(refused.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
        assertThat(json(refused, "$.code")).isEqualTo("SUBTASK_DEADLINE_AFTER_PARENT");
    }

    @Test
    void theCommentsGoAndComeBackWithTheirTask() { // 11.7
        long project = newProject("Web", "In progress", "marta", "ana");
        long task = newTask(project, "T", "To do", null, "ana");
        post("/tasks/" + task + "/comments", sessionOf("ana"), Map.of("content", "Falta o capítulo 2"));

        delete("/tasks/" + task, sessionOf("ana"));
        assertThat(get("/tasks/" + task + "/comments", sessionOf("ana")).getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);

        post("/recycle-bin/tasks/" + task + "/restore", sessionOf("ana"), null);
        assertThat(JsonPath.<List<String>>read(get("/tasks/" + task + "/comments", sessionOf("ana")).getBody(), "$[*].content"))
                .containsExactly("Falta o capítulo 2");
    }

    // ---- who may and when ----

    @Test
    void onlyThoseWhoReachTheProjectDeleteAndRestoreItsTasks() {
        long project = newProject("Web", "In progress", "marta", "ana");
        long task = newTask(project, "T", "To do", null, "ana");

        assertThat(delete("/tasks/" + task, sessionOf("sofia")).getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(delete("/tasks/" + task, null).getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(delete("/tasks/999999", sessionOf("ana")).getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(delete("/tasks/" + task, sessionOf("marta")).getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT); // the manager too (A35)

        assertThat(post("/recycle-bin/tasks/" + task + "/restore", sessionOf("sofia"), null).getStatusCode())
                .isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(post("/recycle-bin/tasks/" + task + "/restore", null, null).getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(post("/recycle-bin/tasks/999999/restore", sessionOf("ana"), null).getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(bin("sofia")).isEmpty();
        assertThat(get("/recycle-bin", null).getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(delete("/tasks/" + task, sessionOf("ana")).getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND); // already deleted
    }

    @Test
    void onlyTheManagerDeletesAProject() {
        long project = newProject("Web", "In progress", "marta", "ana");

        assertThat(delete("/projects/" + project, sessionOf("ana")).getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(delete("/projects/" + project, sessionOf("pedro")).getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(delete("/projects/" + project, sessionOf("sofia")).getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(delete("/projects/" + project, null).getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(delete("/projects/999999", sessionOf("marta")).getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(get("/projects/" + project, sessionOf("marta")).getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void aCompletedProjectIsNotDeletedAndNothingInItIsDeleted() { // R28
        long project = newProject("Old", "Completed", "marta", "ana");
        long task = newTask(project, "T", "Done", null, "ana");

        ResponseEntity<String> deletedProject = delete("/projects/" + project, sessionOf("marta"));
        ResponseEntity<String> deletedTask = delete("/tasks/" + task, sessionOf("ana"));

        assertThat(deletedProject.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
        assertThat(json(deletedProject, "$.code")).isEqualTo("PROJECT_COMPLETED");
        assertThat(deletedTask.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
        assertThat(json(deletedTask, "$.code")).isEqualTo("PROJECT_COMPLETED");
        assertThat(bin("marta")).isEmpty();
    }

    @Test
    void aSuspendedProjectLetsItsTasksBeDeleted() { // R27
        long project = newProject("Paused", "Suspended", "marta", "ana");
        long task = newTask(project, "T", "To do", null, "ana");

        assertThat(delete("/tasks/" + task, sessionOf("ana")).getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        assertThat(delete("/projects/" + project, sessionOf("marta")).getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
    }

    @Test
    void aTaskDeletedFromAProjectThatIsCompletedLaterCannotBeRestored() { // R28
        long project = newProject("Web", "In progress", "marta", "ana");
        long task = newTask(project, "T", "To do", null, "ana");
        delete("/tasks/" + task, sessionOf("ana"));
        jdbc.update("UPDATE project SET status = 'Completed' WHERE id = ?", project);

        Map<String, Object> item = bin("ana").get(0);
        ResponseEntity<String> refused = post("/recycle-bin/tasks/" + task + "/restore", sessionOf("ana"), null);

        assertThat(item).containsEntry("restorable", false).containsEntry("notRestorableReason", "PROJECT_COMPLETED");
        assertThat(refused.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
        assertThat(json(refused, "$.code")).isEqualTo("PROJECT_COMPLETED");
    }

    @Test
    void aDeletedProjectDoesNotCountForTheRuleOfOneOpenProject() { // R11
        long project = newProject("Gone", "In progress", "marta", "diana"); // diana is in no other open project
        long other = newProject("Other", "In progress", "pedro");
        assertThat(post("/projects/" + other + "/members", sessionOf("pedro"), Map.of("userId", userId("diana"))).getStatusCode())
                .isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY); // while Gone is open, she is taken (R11)
        delete("/projects/" + project, sessionOf("marta"));

        ResponseEntity<String> added = post("/projects/" + other + "/members", sessionOf("pedro"), Map.of("userId", userId("diana")));

        assertThat(added.getStatusCode()).isEqualTo(HttpStatus.CREATED);
    }

    // ---- the 30 days (7.6, 7.8, EC23) ----

    @Test
    void anItemStaysThirtyDaysInTheBin() { // 7.6, EC23
        long project = newProject("Web", "In progress", "marta", "ana");
        clock.set("2026-03-01T10:00:00Z");
        delete("/projects/" + project, sessionOf("marta"));

        clock.set("2026-03-30T10:00:00Z"); // 29 days
        Map<String, Object> at29 = bin("marta").get(0);
        assertThat(at29).containsEntry("restorable", true).containsEntry("daysLeft", 1);

        clock.set("2026-03-31T09:59:59Z"); // a second before the 30 days
        assertThat(bin("marta")).hasSize(1);
        clock.set("2026-03-31T10:00:00Z"); // exactly 30 days: gone (A39)
        assertThat(bin("marta")).isEmpty();
        clock.set("2026-04-01T10:00:00Z"); // 31 days
        assertThat(bin("marta")).isEmpty();
        assertThat(post("/recycle-bin/projects/" + project + "/restore", sessionOf("marta"), null).getStatusCode())
                .isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(get("/projects/" + project, sessionOf("marta")).getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void aTaskInTheBinCanBeRestoredAt29DaysAndNotAt31() { // 7.6
        long project = newProject("Web", "In progress", "marta", "ana");
        long keep = newTask(project, "Keep", "To do", null, "ana");
        long lose = newTask(project, "Lose", "To do", null, "ana");
        clock.set("2026-03-01T10:00:00Z");
        delete("/tasks/" + keep, sessionOf("ana"));
        delete("/tasks/" + lose, sessionOf("ana"));

        clock.set("2026-03-30T10:00:00Z");
        assertThat(post("/recycle-bin/tasks/" + keep + "/restore", sessionOf("ana"), null).getStatusCode())
                .isEqualTo(HttpStatus.NO_CONTENT);
        clock.set("2026-04-01T10:00:00Z");
        assertThat(post("/recycle-bin/tasks/" + lose + "/restore", sessionOf("ana"), null).getStatusCode())
                .isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void theDaysLeftCountDownAndRoundUp() {
        long project = newProject("Web", "In progress", "marta", "ana");
        clock.set("2026-03-01T10:00:00Z");
        delete("/projects/" + project, sessionOf("marta"));

        clock.set("2026-03-11T10:00:00Z"); // 10 days in: 20 left
        assertThat(bin("marta").get(0)).containsEntry("daysLeft", 20);
        clock.set("2026-03-11T11:00:00Z"); // 19 days and 23 hours left: 20 days, rounded up
        assertThat(bin("marta").get(0)).containsEntry("daysLeft", 20);
        clock.set("2026-03-12T09:00:00Z"); // 19 days and 1 hour left
        assertThat(bin("marta").get(0)).containsEntry("daysLeft", 20);
        clock.set("2026-03-12T11:00:00Z"); // 18 days and 23 hours left
        assertThat(bin("marta").get(0)).containsEntry("daysLeft", 19);
    }

    @Test
    void aSubtaskDeletedBeforeItsParentExpiresBeforeIt() { // 7.8, as A40 says
        long project = newProject("Web", "In progress", "marta", "ana");
        long parent = newTask(project, "Parent", "To do", null, "ana");
        long sub = newTask(project, "Sub", "To do", parent, "ana");
        clock.set("2026-03-01T10:00:00Z");
        delete("/tasks/" + sub, sessionOf("ana"));
        jdbc.update("UPDATE task SET status = 'Done' WHERE id = ?", parent);
        clock.set("2026-03-11T10:00:00Z");
        delete("/tasks/" + parent, sessionOf("ana"));

        clock.set("2026-03-16T10:00:00Z"); // the parent has been in the bin 5 days
        post("/recycle-bin/tasks/" + parent + "/restore", sessionOf("ana"), null);
        assertThat(JsonPath.<List<Object>>read(get("/tasks/" + parent, sessionOf("ana")).getBody(), "$.subtasks")).isEmpty();
        assertThat(bin("ana")).extracting(i -> i.get("name")).containsExactly("Sub"); // still there, and it can come back

        delete("/tasks/" + parent, sessionOf("ana")); // the parent goes again, now with Sub out of its way
        clock.set("2026-04-02T10:00:00Z"); // Sub: 32 days in the bin; the parent, 17
        assertThat(post("/recycle-bin/tasks/" + sub + "/restore", sessionOf("ana"), null).getStatusCode())
                .isEqualTo(HttpStatus.NOT_FOUND); // gone with its own 30 days, not because of the parent
        assertThat(bin("ana")).extracting(i -> i.get("name")).containsExactly("Parent");
    }

    // ---- P36: someone left the project while the task was in the bin ----

    @Test
    void aTaskWhoseAssigneeLeftAsksForANewOneBeforeItComesBack() { // P36
        long project = newProject("Web", "In progress", "marta", "ana", "rui");
        long task = newTask(project, "T", "To do", null, "rui", "ana");
        delete("/tasks/" + task, sessionOf("ana"));
        jdbc.update("DELETE FROM project_member WHERE project_id = ? AND user_id = ?", project, userId("rui"));

        ResponseEntity<String> asked = post("/recycle-bin/tasks/" + task + "/restore", sessionOf("ana"), null);

        assertThat(asked.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
        assertThat(json(asked, "$.code")).isEqualTo("ASSIGNEE_REQUIRED");
        assertThat(json(asked, "$.message").toString()).contains("Rui");
        assertThat(bin("ana")).hasSize(1); // still in the bin

        ResponseEntity<String> restored = post("/recycle-bin/tasks/" + task + "/restore", sessionOf("ana"),
                Map.of("assigneeIds", List.of(userId("luis"))));
        assertThat(restored.getStatusCode()).as("luis is not a member").isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
        assertThat(json(restored, "$.code")).isEqualTo("ASSIGNEE_NOT_MEMBER");

        jdbc.update("INSERT INTO project_member (project_id, user_id) VALUES (?, ?)", project, userId("luis"));
        restored = post("/recycle-bin/tasks/" + task + "/restore", sessionOf("ana"), Map.of("assigneeIds", List.of(userId("luis"))));
        assertThat(restored.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        assertThat(JsonPath.<List<String>>read(get("/tasks/" + task, sessionOf("ana")).getBody(), "$.assignees[*].name"))
                .containsExactlyInAnyOrder("Ana", "Luís"); // Rui is replaced, Ana stays
    }

    @Test
    void theNewAssigneesAreCheckedAfterTheAccess() {
        long project = newProject("Web", "In progress", "marta", "ana");
        long task = newTask(project, "T", "To do", null, "ana");
        delete("/tasks/" + task, sessionOf("ana"));

        assertThat(post("/recycle-bin/tasks/" + task + "/restore", sessionOf("sofia"), Map.of("assigneeIds", "x")).getStatusCode())
                .isEqualTo(HttpStatus.FORBIDDEN); // 403 before 400
        for (Object body : List.of(Map.of("assigneeIds", List.of()), Map.of("assigneeIds", "x"), Map.of("other", 1),
                Map.of("assigneeIds", List.of(0)), Map.of("assigneeIds", List.of(userId("ana"), userId("ana"))))) {
            assertThat(post("/recycle-bin/tasks/" + task + "/restore", sessionOf("ana"), body).getStatusCode())
                    .as(body.toString()).isEqualTo(HttpStatus.BAD_REQUEST);
        }
        assertThat(bin("ana")).hasSize(1); // nothing was restored
    }

    @Test
    void theSubtasksThatComeBackWithATaskAlsoGetTheNewAssignees() { // P36
        long project = newProject("Web", "In progress", "marta", "ana", "rui");
        long parent = newTask(project, "Parent", "Done", null, "ana");
        long sub = newTask(project, "Sub", "Done", parent, "rui");
        delete("/tasks/" + parent, sessionOf("ana"));
        jdbc.update("DELETE FROM project_member WHERE project_id = ? AND user_id = ?", project, userId("rui"));

        ResponseEntity<String> asked = post("/recycle-bin/tasks/" + parent + "/restore", sessionOf("ana"), null);
        assertThat(json(asked, "$.code")).isEqualTo("ASSIGNEE_REQUIRED");

        post("/recycle-bin/tasks/" + parent + "/restore", sessionOf("ana"), Map.of("assigneeIds", List.of(userId("ana"))));

        assertThat(JsonPath.<List<String>>read(get("/tasks/" + sub, sessionOf("ana")).getBody(), "$.assignees[*].name"))
                .containsExactly("Ana");
        assertThat(JsonPath.<List<String>>read(get("/tasks/" + parent, sessionOf("ana")).getBody(), "$.assignees[*].name"))
                .containsExactly("Ana");
    }

    // ---- eliminating for good ----

    @Test
    void theJobRemovesWhatExpiredWithItsCommentsAssigneesAndMembers() { // R20
        long project = newProject("Web", "In progress", "marta", "ana");
        long original = newTask(project, "Original", "To do", null, "ana");
        long duplicate = newTask(project, "Duplicate", "To do", null, "ana");
        jdbc.update("UPDATE task SET status = 'Duplicate', original_task_id = ? WHERE id = ?", original, duplicate);
        post("/tasks/" + original + "/comments", sessionOf("ana"), Map.of("content", "x"));
        long keepProject = newProject("Keep", "In progress", "marta", "rui");
        long keepTask = newTask(keepProject, "Keep", "To do", null, "rui");
        clock.set("2026-03-01T10:00:00Z");
        delete("/tasks/" + original, sessionOf("ana"));
        clock.set("2026-03-02T10:00:00Z");
        delete("/projects/" + project, sessionOf("marta"));

        clock.set("2026-03-31T09:00:00Z"); // the task has 29 days and 23 hours: nothing yet
        assertThat(purgeJob.purge()).isZero();
        assertThat(taskRows(original)).isEqualTo(1);

        clock.set("2026-04-01T10:00:00Z"); // the task has 31 days, the project 30 days
        int removed = purgeJob.purge();

        assertThat(removed).isEqualTo(3); // the two tasks of the project and the project
        assertThat(taskRows(original)).isZero();
        assertThat(taskRows(duplicate)).isZero(); // it went with its project
        assertThat(jdbc.queryForObject("SELECT count(*) FROM project WHERE id = ?", Integer.class, project)).isZero();
        assertThat(jdbc.queryForObject("SELECT count(*) FROM project_member WHERE project_id = ?", Integer.class, project)).isZero();
        assertThat(jdbc.queryForObject("SELECT count(*) FROM task_comment WHERE task_id = ?", Integer.class, original)).isZero();
        assertThat(jdbc.queryForObject("SELECT count(*) FROM task_assignee WHERE task_id IN (?, ?)", Integer.class, original, duplicate)).isZero();
        assertThat(taskRows(keepTask)).isEqualTo(1); // what is not in the bin stays
        assertThat(purgeJob.purge()).isZero(); // and it has nothing left to do
    }

    @Test
    void theJobAlsoRemovesTheTasksThatAppearedUnderSomethingExpired() { // R20: a safety net for two requests that crossed
        long project = newProject("Web", "In progress", "marta", "ana");
        long parent = newTask(project, "Parent", "Done", null, "ana");
        long stray = newTask(project, "Stray", "To do", parent, "ana"); // not deleted, under a parent that will expire
        long other = newProject("Other", "In progress", "marta", "ana");
        long strayInProject = newTask(other, "StrayInProject", "To do", null, "ana"); // not deleted, in a project that will expire
        clock.set("2026-03-01T10:00:00Z");
        jdbc.update("UPDATE task SET deleted_at = ? WHERE id = ?", Timestamp.from(clock.instant()), parent);
        jdbc.update("UPDATE project SET deleted_at = ? WHERE id = ?", Timestamp.from(clock.instant()), other);

        clock.set("2026-04-01T10:00:00Z");
        purgeJob.purge(); // no foreign key breaks, and the job does not fail every night

        assertThat(taskRows(parent)).isZero();
        assertThat(taskRows(stray)).isZero();
        assertThat(taskRows(strayInProject)).isZero();
        assertThat(jdbc.queryForObject("SELECT count(*) FROM project WHERE id = ?", Integer.class, other)).isZero();
        assertThat(jdbc.queryForObject("SELECT count(*) FROM project WHERE id = ?", Integer.class, project)).isEqualTo(1);
    }

    @Test
    void aDuplicateKeepsItsStatusWhenItsOriginalIsEliminated() { // A41
        long project = newProject("Web", "In progress", "marta", "ana");
        long original = newTask(project, "Original", "To do", null, "ana");
        long duplicate = newTask(project, "Duplicate", "To do", null, "ana");
        jdbc.update("UPDATE task SET status = 'Duplicate', original_task_id = ? WHERE id = ?", original, duplicate);
        clock.set("2026-03-01T10:00:00Z");
        delete("/tasks/" + original, sessionOf("ana"));

        clock.set("2026-04-01T10:00:00Z");
        purgeJob.purge();

        assertThat(statusOf(duplicate)).isEqualTo("Duplicate");
        assertThat(jdbc.queryForObject("SELECT original_task_id FROM task WHERE id = ?", Long.class, duplicate)).isNull();
        ResponseEntity<String> read = get("/tasks/" + duplicate, sessionOf("ana"));
        assertThat(read.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(json(read, "$.originalEliminated")).isEqualTo(true); // the client shows a note
        assertThat(JsonPath.<Object>read(read.getBody(), "$.originalTaskId")).isNull();
        ResponseEntity<String> edited = patch("/tasks/" + duplicate, sessionOf("ana"), Map.of("version", 1, "title", "Renamed"));
        assertThat(edited.getStatusCode()).isEqualTo(HttpStatus.OK); // it can still be edited
        assertThat(json(edited, "$.title")).isEqualTo("Renamed");
        assertThat(json(edited, "$.originalEliminated")).isEqualTo(true);
    }

    @Test
    void aDuplicateIsStillEditedWhileItsOriginalIsInTheBin() { // A41
        long project = newProject("Web", "In progress", "marta", "ana");
        long original = newTask(project, "Original", "To do", null, "ana");
        long duplicate = newTask(project, "Duplicate", "To do", null, "ana");
        jdbc.update("UPDATE task SET status = 'Duplicate', original_task_id = ? WHERE id = ?", original, duplicate);
        delete("/tasks/" + original, sessionOf("ana"));

        ResponseEntity<String> edited = patch("/tasks/" + duplicate, sessionOf("ana"), Map.of("version", 1, "title", "Renamed"));

        assertThat(edited.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(json(edited, "$.originalEliminated")).isEqualTo(false); // it is only in the bin: the link stays
        assertThat(json(edited, "$.originalInRecycleBin")).isEqualTo(true); // and the client is told it leads nowhere for now
        assertThat(((Number) json(edited, "$.originalTaskId")).longValue()).isEqualTo(original);
        assertThat(JsonPath.<List<Boolean>>read(get("/projects/" + project + "/tasks", sessionOf("ana")).getBody(),
                "$[?(@.title=='Renamed')].originalInRecycleBin")).containsExactly(true); // also in the list
        ResponseEntity<String> noOriginal = patch("/tasks/" + duplicate, sessionOf("ana"), Map.of("version", 2, "originalTaskId", original));
        assertThat(noOriginal.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY); // choosing a deleted original is still refused
        assertThat(json(noOriginal, "$.code")).isEqualTo("ORIGINAL_TASK_NOT_VALID");

        post("/recycle-bin/tasks/" + original + "/restore", sessionOf("ana"), null);
        assertThat(json(get("/tasks/" + duplicate, sessionOf("ana")), "$.originalInRecycleBin")).isEqualTo(false); // back
    }

    @Test
    void whatIsDeletedStopsCountingEverywhere() {
        long project = newProject("Web", "In progress", "marta", "ana");
        long task = newTask(project, "T", "To do", null, "ana");
        newTask(project, "Other", "To do", null, "ana");
        delete("/tasks/" + task, sessionOf("ana"));

        assertThat(JsonPath.<List<String>>read(get("/projects/" + project + "/tasks", sessionOf("ana")).getBody(), "$[*].title"))
                .containsExactly("Other");
        assertThat(get("/tasks/" + task, sessionOf("ana")).getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(patch("/tasks/" + task, sessionOf("ana"), Map.of("version", 1, "title", "x")).getStatusCode())
                .isEqualTo(HttpStatus.NOT_FOUND);
        Map<String, Object> entry = JsonPath.<List<Map<String, Object>>>read(get("/dashboard", sessionOf("marta")).getBody(),
                "$[?(@.project.name =~ /Web#.*/)]").get(0);
        assertThat(((Map<?, ?>) entry.get("counts")).get("toDo")).isEqualTo(1);
    }

    // ---- at the same time ----

    @Test
    void twoDeletionsOfTheSameTaskAreDoneOneAfterTheOther() throws Exception {
        long project = newProject("Web", "In progress", "marta", "ana");
        long task = newTask(project, "T", "To do", null, "ana");
        String ana = sessionOf("ana");
        ExecutorService pool = Executors.newSingleThreadExecutor();
        try {
            // the second request reaches the task while the first one has it locked: it waits, and then finds it gone
            Future<ResponseEntity<String>> second = new TransactionTemplate(transactions).execute(tx -> {
                jdbc.queryForObject("SELECT id FROM task WHERE id = ? FOR UPDATE", Long.class, task);
                Future<ResponseEntity<String>> pending = pool.submit(() -> delete("/tasks/" + task, ana));
                awaitLockWaiters(1);
                jdbc.update("UPDATE task SET deleted_at = now() WHERE id = ?", task); // the first deletion
                return pending;
            });

            assertThat(second.get(15, TimeUnit.SECONDS).getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        } finally {
            pool.shutdownNow();
        }
    }

    @Test
    void aSubtaskIsNotCreatedUnderAParentThatIsBeingDeleted() throws Exception {
        long project = newProject("Web", "In progress", "marta", "ana");
        long parent = newTask(project, "Parent", "To do", null, "ana");
        int before = count("task");
        String ana = sessionOf("ana");
        ExecutorService pool = Executors.newSingleThreadExecutor();
        try {
            Future<ResponseEntity<String>> create = new TransactionTemplate(transactions).execute(tx -> {
                jdbc.queryForObject("SELECT id FROM task WHERE id = ? FOR UPDATE", Long.class, parent); // the deletion
                Future<ResponseEntity<String>> pending = pool.submit(() -> post("/projects/" + project + "/tasks", ana,
                        Map.of("title", "Sub", "deadline", "2026-12-01", "urgency", 3, "importance", 3,
                                "assigneeIds", List.of(userId("ana")), "parentTaskId", parent)));
                awaitLockWaiters(1);
                jdbc.update("UPDATE task SET deleted_at = now() WHERE id = ?", parent);
                return pending;
            });

            assertThat(create.get(15, TimeUnit.SECONDS).getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        } finally {
            pool.shutdownNow();
        }
        assertThat(count("task")).isEqualTo(before); // no subtask appeared under the deleted parent
    }

    @Test
    void aRestoreAndTheEliminationOfTheSameTaskAreDoneOneAfterTheOther() throws Exception {
        long project = newProject("Web", "In progress", "marta", "ana");
        long task = newTask(project, "T", "To do", null, "ana");
        delete("/tasks/" + task, sessionOf("ana"));
        String ana = sessionOf("ana");
        ExecutorService pool = Executors.newSingleThreadExecutor();
        try {
            // the purge has the row (it removes it) when the restore arrives: the restore waits, and then finds nothing
            Future<ResponseEntity<String>> restore = new TransactionTemplate(transactions).execute(tx -> {
                jdbc.queryForObject("SELECT id FROM task WHERE id = ? FOR UPDATE", Long.class, task);
                Future<ResponseEntity<String>> pending = pool.submit(() -> post("/recycle-bin/tasks/" + task + "/restore", ana, null));
                awaitLockWaiters(1);
                jdbc.update("DELETE FROM task_assignee WHERE task_id = ?", task);
                jdbc.update("DELETE FROM task WHERE id = ?", task);
                return pending;
            });

            assertThat(restore.get(15, TimeUnit.SECONDS).getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        } finally {
            pool.shutdownNow();
        }
    }

    @Test
    void aSubtaskIsNotRestoredUnderAParentThatIsClosedWhileTheRestoreWaits() throws Exception { // R4, P35
        long project = newProject("Web", "In progress", "marta", "ana");
        long parent = newTask(project, "Parent", "To do", null, "ana");
        long sub = newTask(project, "Sub", "In progress", parent, "ana");
        delete("/tasks/" + sub, sessionOf("ana"));
        String ana = sessionOf("ana");
        ExecutorService pool = Executors.newSingleThreadExecutor();
        try {
            // the manager is closing the parent (its row is locked) when the restore arrives: the restore waits for it
            Future<ResponseEntity<String>> restore = new TransactionTemplate(transactions).execute(tx -> {
                jdbc.queryForObject("SELECT id FROM task WHERE id = ? FOR UPDATE", Long.class, parent);
                Future<ResponseEntity<String>> pending = pool.submit(() -> post("/recycle-bin/tasks/" + sub + "/restore", ana, null));
                awaitLockWaiters(1);
                jdbc.update("UPDATE task SET status = 'Done' WHERE id = ?", parent);
                return pending;
            });

            ResponseEntity<String> response = restore.get(15, TimeUnit.SECONDS);
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
            assertThat(json(response, "$.code")).isEqualTo("PARENT_TASK_CLOSED");
        } finally {
            pool.shutdownNow();
        }
        assertThat(bin("ana")).hasSize(1); // still in the bin, and no open subtask under a closed parent
    }

    @Test
    void aTaskIsNotRestoredIntoAProjectThatIsBeingDeleted() throws Exception { // R9
        long project = newProject("Web", "In progress", "marta", "ana");
        long task = newTask(project, "T", "To do", null, "ana");
        delete("/tasks/" + task, sessionOf("ana"));
        String ana = sessionOf("ana");
        ExecutorService pool = Executors.newSingleThreadExecutor();
        try {
            Future<ResponseEntity<String>> restore = new TransactionTemplate(transactions).execute(tx -> {
                jdbc.queryForObject("SELECT id FROM project WHERE id = ? FOR UPDATE", Long.class, project); // the deletion
                Future<ResponseEntity<String>> pending = pool.submit(() -> post("/recycle-bin/tasks/" + task + "/restore", ana, null));
                awaitLockWaiters(1);
                jdbc.update("UPDATE project SET deleted_at = now() WHERE id = ?", project);
                return pending;
            });

            ResponseEntity<String> response = restore.get(15, TimeUnit.SECONDS);
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
            assertThat(json(response, "$.code")).isEqualTo("CANNOT_RESTORE");
        } finally {
            pool.shutdownNow();
        }
        assertThat(jdbc.queryForObject("SELECT deleted_at IS NOT NULL FROM task WHERE id = ?", Boolean.class, task)).isTrue();
    }

    // ---- helpers ----

    private List<Map<String, Object>> bin(String username) {
        ResponseEntity<String> response = get("/recycle-bin", sessionOf(username));
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        return JsonPath.read(response.getBody(), "$");
    }

    private String statusOf(long taskId) {
        return jdbc.queryForObject("SELECT status FROM task WHERE id = ?", String.class, taskId);
    }

    private int taskRows(long taskId) {
        return jdbc.queryForObject("SELECT count(*) FROM task WHERE id = ?", Integer.class, taskId);
    }

    /** A project, named "name#unique" so that names never clash; manager and members given by username. */
    private long newProject(String name, String status, String manager, String... members) {
        long id = jdbc.queryForObject("""
                INSERT INTO project (name, start_date, end_date, phase, status, manager_id)
                VALUES (?, DATE '2026-01-01', DATE '2027-12-31', 'Execution', ?, ?) RETURNING id
                """, Long.class, name + "#" + System.nanoTime(), status, userId(manager));
        for (String member : members) {
            jdbc.update("INSERT INTO project_member (project_id, user_id) VALUES (?, ?)", id, userId(member));
        }
        return id;
    }

    /** A task (or a subtask when a parent is given) with a deadline in December 2026 and the given assignees. */
    private long newTask(long projectId, String title, String status, Long parentTaskId, String... assignees) {
        long id = jdbc.queryForObject("""
                INSERT INTO task (project_id, parent_task_id, title, status, deadline, urgency, importance)
                VALUES (?, ?, ?, ?, DATE '2026-12-31', 3, 3) RETURNING id
                """, Long.class, projectId, parentTaskId, title, status);
        for (String username : assignees) {
            jdbc.update("INSERT INTO task_assignee (task_id, user_id) VALUES (?, ?)", id, userId(username));
        }
        return id;
    }
}
