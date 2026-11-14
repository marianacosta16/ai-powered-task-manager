package com.matriz.views;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import com.jayway.jsonpath.JsonPath;
import com.matriz.testsupport.ApiIntegrationTest;

/**
 * The List and the Matrix of a project (listProjectTasks): scenarios 3.1 to 3.6, 9.1 to 9.5 and 12.4
 * to 12.6, and the search of projects, 12.1 to 12.3 (listProjects). R15, R18, R23, R25, P25 and P29,
 * through real HTTP requests. Each test builds its own project with the tasks it needs.
 */
class ProjectTaskListIntegrationTest extends ApiIntegrationTest {

    // ---- the Matrix (3.1 to 3.6) ----

    @Test
    void eachTaskIsInTheQuadrantOfItsUrgencyAndImportance() { // 3.1
        long project = newProject("Matrix");
        newTask(project, "A", "In progress", "2026-12-10", 4, 4, null, "ana");
        newTask(project, "B", "In progress", "2026-12-10", 1, 3, null, "ana");
        newTask(project, "C", "In progress", "2026-12-10", 3, 2, null, "ana");
        newTask(project, "D", "In progress", "2026-12-10", 2, 1, null, "ana");

        ResponseEntity<String> list = list("ana", project, Map.of());

        assertThat(quadrantOf(list, "A")).isEqualTo("Do first"); // urgent and important
        assertThat(quadrantOf(list, "B")).isEqualTo("Schedule"); // important, not urgent
        assertThat(quadrantOf(list, "C")).isEqualTo("Delegate"); // urgent, not important
        assertThat(quadrantOf(list, "D")).isEqualTo("Later");
    }

    @Test
    void closedTasksComeWithTheirFinalStatusSoTheMatrixCanHideThem() { // 3.2, P10: the client hides them
        long project = newProject("Matrix");
        newTask(project, "Open", "To do", "2026-12-10", 3, 3, null, "ana");
        newTask(project, "Done", "Done", "2026-12-01", 3, 3, null, "ana");
        newTask(project, "Cancelled", "Cancelled", "2026-12-01", 3, 3, null, "ana");
        newTask(project, "Duplicate", "Duplicate", "2026-12-01", 3, 3, null, "ana");

        ResponseEntity<String> list = list("ana", project, Map.of());

        assertThat(titles(list)).startsWith("Open").hasSize(4); // the open task first
        assertThat(JsonPath.<List<String>>read(list.getBody(), "$[?(@.title!='Open')].status"))
                .containsExactlyInAnyOrder("Done", "Cancelled", "Duplicate");
    }

    @Test
    void myTasksAreTheOnesImResponsibleFor() { // 3.3
        long project = newProject("Mine");
        newTask(project, "Ana's", "To do", "2026-12-10", 3, 3, null, "ana");
        newTask(project, "Rui's", "To do", "2026-12-11", 3, 3, null, "rui");
        newTask(project, "Both", "To do", "2026-12-12", 3, 3, null, "ana", "rui");

        assertThat(titles(list("ana", project, Map.of("mine", "true")))).containsExactly("Ana's", "Both");
        assertThat(titles(list("ana", project, Map.of()))).containsExactly("Ana's", "Rui's", "Both"); // off: everyone's
        assertThat(titles(list("ana", project, Map.of("mine", "false")))).containsExactly("Ana's", "Rui's", "Both");
    }

    @Test
    void subtasksAreInsideTheirParentAndNotListedAlone() { // 3.4, 9.4, R18
        long project = newProject("Subtasks");
        long a = newTask(project, "A", "To do", "2026-12-20", 3, 3, null, "ana");
        newTask(project, "a1", "To do", "2026-12-10", 3, 3, a, "ana");
        newTask(project, "a2", "To do", "2026-12-11", 3, 3, a, "rui");
        newTask(project, "B", "To do", "2026-12-21", 3, 3, null, "ana");

        ResponseEntity<String> list = list("ana", project, Map.of());

        assertThat(titles(list)).containsExactly("A", "B"); // the subtasks are not there on their own
        assertThat(JsonPath.<List<String>>read(list.getBody(), "$[0].subtasks[*].title")).containsExactly("a1", "a2");
        assertThat(JsonPath.<List<Object>>read(list.getBody(), "$[1].subtasks")).isEmpty();
        assertThat(JsonPath.<List<String>>read(list.getBody(), "$[0].subtasks[0].assignees[*].name")).containsExactly("Ana");
        assertThat(list.getBody()).doesNotContain("\"subtasks\":null");
    }

    @Test
    void myTasksIncludeAMainTaskWhenImResponsibleForOneOfItsSubtasks() { // 3.5, P25
        long project = newProject("Subtasks");
        long a = newTask(project, "A", "To do", "2026-12-20", 3, 3, null, "rui");
        newTask(project, "a1", "To do", "2026-12-10", 3, 3, a, "ana");
        newTask(project, "Rui only", "To do", "2026-12-22", 3, 3, null, "rui");

        ResponseEntity<String> list = list("ana", project, Map.of("mine", "true"));

        assertThat(titles(list)).containsExactly("A"); // Ana is responsible for a1, not for A
        assertThat(JsonPath.<List<String>>read(list.getBody(), "$[0].assignees[*].name")).containsExactly("Rui");
        assertThat(JsonPath.<List<String>>read(list.getBody(), "$[0].subtasks[*].title")).containsExactly("a1");
    }

    @Test
    void inAQuadrantTheOrderIsDeadlineThenUrgencyThenImportance() { // 3.6, R23
        long project = newProject("Order");
        newTask(project, "A", "To do", "2026-12-20", 4, 4, null, "ana");
        newTask(project, "B", "To do", "2026-12-10", 3, 3, null, "ana");
        newTask(project, "C", "To do", "2026-12-10", 4, 3, null, "ana");
        newTask(project, "D", "To do", "2026-12-10", 4, 4, null, "ana");

        ResponseEntity<String> list = list("ana", project, Map.of("quadrant", "Do first"));

        assertThat(titles(list)).containsExactly("D", "C", "B", "A");
    }

    @Test
    void tasksThatTieOnEverythingKeepTheOrderOfCreation() { // A29
        long project = newProject("Ties");
        newTask(project, "first", "To do", "2026-12-10", 3, 3, null, "ana");
        newTask(project, "second", "To do", "2026-12-10", 3, 3, null, "ana");
        newTask(project, "third", "To do", "2026-12-10", 3, 3, null, "ana");

        assertThat(titles(list("ana", project, Map.of()))).containsExactly("first", "second", "third");
    }

    // ---- the List (9.1 to 9.5) ----

    @Test
    void theListShowsEveryTaskOfTheProjectWithItsData() { // 9.1, R15
        long project = newProject("List");
        for (int i = 1; i <= 3; i++) {
            newTask(project, "Open " + i, "To do", "2026-12-1" + i, 2, 3, null, "ana", "rui");
        }
        newTask(project, "Closed 1", "Done", "2026-11-01", 1, 1, null, "ana");
        newTask(project, "Closed 2", "Done", "2026-11-02", 1, 1, null, "rui");

        ResponseEntity<String> list = list("rui", project, Map.of());

        assertThat(list.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(titles(list)).hasSize(5);
        assertThat(JsonPath.<String>read(list.getBody(), "$[0].status")).isEqualTo("To do");
        assertThat(JsonPath.<String>read(list.getBody(), "$[0].deadline")).isEqualTo("2026-12-11");
        assertThat(JsonPath.<Integer>read(list.getBody(), "$[0].urgency")).isEqualTo(2);
        assertThat(JsonPath.<Integer>read(list.getBody(), "$[0].importance")).isEqualTo(3);
        assertThat(JsonPath.<List<String>>read(list.getBody(), "$[0].assignees[*].name")).containsExactly("Ana", "Rui");
        assertThat(JsonPath.<String>read(list.getBody(), "$[0].title")).isEqualTo("Open 1");
    }

    @Test
    void openTasksComeBeforeClosedOnes() { // 9.2
        long project = newProject("List");
        newTask(project, "T1", "Done", "2026-11-01", 3, 3, null, "ana");
        newTask(project, "T2", "To do", "2026-12-20", 3, 3, null, "ana");
        newTask(project, "T3", "Cancelled", "2026-11-02", 3, 3, null, "ana");
        newTask(project, "T4", "In progress", "2026-12-21", 3, 3, null, "ana");

        List<String> titles = titles(list("ana", project, Map.of()));

        assertThat(titles.subList(0, 2)).containsExactlyInAnyOrder("T2", "T4");
        assertThat(titles.subList(2, 4)).containsExactlyInAnyOrder("T1", "T3");
    }

    @Test
    void myTasksIncludeTheClosedOnes() { // 9.3
        long project = newProject("List");
        newTask(project, "Ana open", "To do", "2026-12-10", 3, 3, null, "ana");
        newTask(project, "Ana done", "Done", "2026-11-10", 3, 3, null, "ana");
        newTask(project, "Rui open", "To do", "2026-12-11", 3, 3, null, "rui");

        assertThat(titles(list("ana", project, Map.of("mine", "true")))).containsExactly("Ana open", "Ana done");
        assertThat(titles(list("ana", project, Map.of()))).hasSize(3);
    }

    @Test
    void theListOrdersOpenTasksThenClosedOnesByDeadline() { // 9.5, R23
        long project = newProject("List");
        newTask(project, "T1", "To do", "2026-12-20", 3, 3, null, "ana");
        newTask(project, "T2", "In progress", "2026-12-05", 3, 3, null, "ana");
        newTask(project, "T3", "Done", "2026-11-20", 3, 3, null, "ana");
        newTask(project, "T4", "Cancelled", "2026-11-01", 3, 3, null, "ana");

        assertThat(titles(list("ana", project, Map.of()))).containsExactly("T2", "T1", "T4", "T3");
    }

    // ---- filters (12.4 to 12.6) ----

    @Test
    void theListIsFilteredByStatus() { // 12.4
        long project = newProject("Filters");
        newTask(project, "T1", "To do", "2026-12-10", 3, 3, null, "ana");
        newTask(project, "T2", "In progress", "2026-12-11", 3, 3, null, "ana");
        newTask(project, "T3", "To do", "2026-12-12", 3, 3, null, "ana");

        assertThat(titles(list("ana", project, Map.of("status", "To do")))).containsExactly("T1", "T3");
    }

    @Test
    void theListIsFilteredByQuadrant() { // 12.5
        long project = newProject("Filters");
        newTask(project, "A", "To do", "2026-12-10", 4, 4, null, "ana");
        newTask(project, "B", "To do", "2026-12-10", 1, 3, null, "ana");
        newTask(project, "C", "To do", "2026-12-10", 3, 2, null, "ana");

        assertThat(titles(list("ana", project, Map.of("quadrant", "Do first")))).containsExactly("A");
        assertThat(titles(list("ana", project, Map.of("quadrant", "Schedule")))).containsExactly("B");
        assertThat(titles(list("ana", project, Map.of("quadrant", "Delegate")))).containsExactly("C");
        assertThat(titles(list("ana", project, Map.of("quadrant", "Later")))).isEmpty();
    }

    @Test
    void theFiltersCombine() { // 12.6, R25, P29
        long project = newProject("Filters");
        newTask(project, "T1", "To do", "2026-12-10", 4, 4, null, "ana");
        newTask(project, "T2", "In progress", "2026-12-11", 4, 4, null, "ana");
        newTask(project, "T3", "To do", "2026-12-12", 4, 4, null, "rui");

        ResponseEntity<String> list = list("ana", project, Map.of("mine", "true", "status", "To do", "quadrant", "Do first"));

        assertThat(titles(list)).containsExactly("T1");
    }

    @Test
    void theFiltersApplyToMainTasksAndTheSubtasksStayInside() { // P29
        long project = newProject("Filters");
        long a = newTask(project, "A", "To do", "2026-12-20", 4, 4, null, "rui");
        newTask(project, "a1", "Done", "2026-12-10", 1, 1, a, "rui"); // another status and quadrant
        newTask(project, "B", "In progress", "2026-12-21", 4, 4, null, "rui");

        ResponseEntity<String> list = list("rui", project, Map.of("status", "To do", "quadrant", "Do first"));

        assertThat(titles(list)).containsExactly("A"); // a1 does not make A match or not
        assertThat(JsonPath.<List<String>>read(list.getBody(), "$[0].subtasks[*].title")).containsExactly("a1");
    }

    @Test
    void anUnknownFilterValueIsRefused() {
        long project = newProject("Filters");

        for (Map<String, String> query : List.of(Map.of("status", "Party"), Map.of("quadrant", "Soon"), Map.of("mine", "maybe"))) {
            ResponseEntity<String> response = list("ana", project, query);
            assertThat(response.getStatusCode()).as(query.toString()).isEqualTo(HttpStatus.BAD_REQUEST);
            assertThat(json(response, "$.code")).isEqualTo("VALIDATION_ERROR");
        }
    }

    // ---- pages (A30, A31) ----

    @Test
    void theListComesInPagesWithTheTotalInAHeader() {
        long project = newProject("Pages");
        for (int i = 1; i <= 5; i++) {
            newTask(project, "T" + i, "To do", "2026-12-1" + i, 3, 3, null, "ana");
        }

        ResponseEntity<String> first = list("ana", project, Map.of("size", "2"));
        ResponseEntity<String> second = list("ana", project, Map.of("size", "2", "page", "2"));
        ResponseEntity<String> last = list("ana", project, Map.of("size", "2", "page", "3"));
        ResponseEntity<String> beyond = list("ana", project, Map.of("size", "2", "page", "4"));

        assertThat(titles(first)).containsExactly("T1", "T2");
        assertThat(titles(second)).containsExactly("T3", "T4");
        assertThat(titles(last)).containsExactly("T5");
        assertThat(beyond.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(beyond.getBody()).isEqualTo("[]");
        for (ResponseEntity<String> page : List.of(first, second, last, beyond)) {
            assertThat(page.getHeaders().getFirst("X-Total-Count")).isEqualTo("5");
        }
        assertThat(titles(list("ana", project, Map.of()))).hasSize(5); // the default page holds 50
    }

    @Test
    void pagesKeepTheOrderOfTheListAndNeverOverlapOnTies() {
        long project = newProject("Pages");
        newTask(project, "closed", "Done", "2026-11-01", 3, 3, null, "ana");
        for (int i = 1; i <= 4; i++) {
            newTask(project, "tie" + i, "To do", "2026-12-10", 3, 3, null, "ana");
        }

        List<String> all = new ArrayList<>();
        for (int page = 1; page <= 3; page++) {
            all.addAll(titles(list("ana", project, Map.of("size", "2", "page", String.valueOf(page)))));
        }

        assertThat(all).containsExactly("tie1", "tie2", "tie3", "tie4", "closed");
    }

    @Test
    void theTotalCountsTheTasksThatMatchTheFiltersAndNotTheSubtasks() {
        long project = newProject("Pages");
        long a = newTask(project, "A", "To do", "2026-12-10", 3, 3, null, "ana");
        newTask(project, "a1", "To do", "2026-12-10", 3, 3, a, "ana");
        newTask(project, "a2", "To do", "2026-12-10", 3, 3, a, "ana");
        newTask(project, "B", "Done", "2026-12-11", 3, 3, null, "ana");
        newTask(project, "C", "To do", "2026-12-12", 3, 3, null, "rui");

        assertThat(list("ana", project, Map.of()).getHeaders().getFirst("X-Total-Count")).isEqualTo("3");
        assertThat(list("ana", project, Map.of("status", "To do", "size", "1")).getHeaders().getFirst("X-Total-Count"))
                .isEqualTo("2");
        assertThat(list("ana", project, Map.of("mine", "true")).getHeaders().getFirst("X-Total-Count")).isEqualTo("2");
        assertThat(JsonPath.<List<String>>read(list("ana", project, Map.of("size", "1")).getBody(), "$[0].subtasks[*].title"))
                .containsExactly("a1", "a2"); // the subtasks do not count towards the page
    }

    @Test
    void aBadPageOrSizeIsRefusedAfterTheAccessCheck() {
        long project = newProject("Pages");
        for (Map<String, String> query : List.of(Map.of("page", "0"), Map.of("page", "abc"), Map.of("page", "99999999999"),
                Map.of("size", "0"), Map.of("size", "201"), Map.of("size", "-1"), Map.of("size", "abc"))) {
            ResponseEntity<String> response = list("ana", project, query);
            assertThat(response.getStatusCode()).as(query.toString()).isEqualTo(HttpStatus.BAD_REQUEST);
            assertThat(json(response, "$.code")).isEqualTo("VALIDATION_ERROR");
            assertThat(list("sofia", project, query).getStatusCode()).as(query.toString()).isEqualTo(HttpStatus.FORBIDDEN);
        }
        assertThat(list("ana", project, Map.of("size", "200")).getStatusCode()).isEqualTo(HttpStatus.OK); // the largest page
        assertThat(list("ana", project, Map.of("size", "1", "page", "1")).getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    // ---- access and states ----

    @Test
    void onlyThoseWhoCanSeeTheProjectListItsTasks() {
        long project = newProject("Access");
        newTask(project, "Secret", "To do", "2026-12-10", 3, 3, null, "ana");

        ResponseEntity<String> outsider = list("sofia", project, Map.of());

        assertThat(outsider.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(outsider.getBody()).doesNotContain("Secret");
        assertThat(list("marta", project, Map.of()).getStatusCode()).isEqualTo(HttpStatus.OK); // the manager
        assertThat(get("/projects/" + project + "/tasks", null).getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(list("ana", 999999, Map.of()).getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        // 403 comes before 400
        assertThat(list("sofia", project, Map.of("status", "Party")).getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(list("sofia", project, Map.of("mine", "maybe")).getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void deletedTasksAndTheTasksOfOtherProjectsAreNotListed() {
        long project = newProject("Mine");
        long other = newProject("Other");
        long gone = newTask(project, "Deleted", "To do", "2026-12-10", 3, 3, null, "ana");
        newTask(project, "Here", "To do", "2026-12-11", 3, 3, null, "ana");
        newTask(other, "Elsewhere", "To do", "2026-12-12", 3, 3, null, "ana");
        jdbc.update("UPDATE task SET deleted_at = now() WHERE id = ?", gone);

        assertThat(titles(list("ana", project, Map.of()))).containsExactly("Here");
    }

    @Test
    void aDeletedSubtaskIsNotListedInsideItsParent() {
        long project = newProject("Mine");
        long a = newTask(project, "A", "To do", "2026-12-20", 3, 3, null, "ana");
        long gone = newTask(project, "gone", "To do", "2026-12-10", 3, 3, a, "ana");
        newTask(project, "kept", "To do", "2026-12-11", 3, 3, a, "ana");
        jdbc.update("UPDATE task SET deleted_at = now() WHERE id = ?", gone);

        ResponseEntity<String> list = list("ana", project, Map.of());

        assertThat(JsonPath.<List<String>>read(list.getBody(), "$[0].subtasks[*].title")).containsExactly("kept");
    }

    @Test
    void aCompletedProjectCanStillBeRead() { // R28: the data stays visible
        ResponseEntity<String> list = list("diana", projectId("Old Site"), Map.of());

        assertThat(list.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(titles(list)).containsExactlyInAnyOrder("Archive old pages", "Shut down server");
    }

    @Test
    void aProjectWithoutTasksGivesAnEmptyList() {
        assertThat(list("ana", newProject("Empty"), Map.of()).getBody()).isEqualTo("[]");
    }

    // ---- searching projects (12.1 to 12.3) ----

    @Test
    void searchFindsProjectsByPartOfTheName() { // 12.1
        ResponseEntity<String> found = get("/projects?search=web", sessionOf("marta")); // Website and Mobile App

        assertThat(JsonPath.<List<String>>read(found.getBody(), "$[*].name")).containsExactly("Website");
    }

    @Test
    void searchOnlyReturnsProjectsTheUserCanSee() { // 12.2
        assertThat(get("/projects?search=website", sessionOf("sofia")).getBody()).isEqualTo("[]");
        // a completed project is found too (R25)
        assertThat(JsonPath.<List<String>>read(get("/projects?search=old", sessionOf("pedro")).getBody(), "$[*].name"))
                .containsExactly("Old Site");
    }

    @Test
    void searchWithNoMatchGivesAnEmptyList() { // 12.3, EC33
        ResponseEntity<String> none = get("/projects?search=zzz", sessionOf("ana"));

        assertThat(none.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(none.getBody()).isEqualTo("[]");
    }

    // ---- helpers ----

    private ResponseEntity<String> list(String username, long projectId, Map<String, String> query) {
        StringBuilder url = new StringBuilder("/projects/" + projectId + "/tasks");
        String separator = "?";
        for (String key : query.keySet()) {
            url.append(separator).append(key).append("={").append(key).append("}");
            separator = "&";
        }
        // the values are given as URI variables, so the client encodes them once (a "%20" would be encoded twice)
        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.COOKIE, sessionOf(username));
        return http.exchange(url.toString(), HttpMethod.GET, new HttpEntity<>(headers), String.class, query);
    }

    private long newProject(String name) {
        long marta = userId("marta");
        long id = jdbc.queryForObject("""
                INSERT INTO project (name, start_date, end_date, phase, status, manager_id)
                VALUES (?, DATE '2026-01-01', DATE '2027-12-31', 'Execution', 'In progress', ?) RETURNING id
                """, Long.class, name + " " + System.nanoTime(), marta);
        for (String member : List.of("ana", "rui", "luis")) {
            jdbc.update("INSERT INTO project_member (project_id, user_id) VALUES (?, ?)", id, userId(member));
        }
        return id;
    }

    private long newTask(long projectId, String title, String status, String deadline, int urgency, int importance,
            Long parentTaskId, String... assignees) {
        long id = jdbc.queryForObject("""
                INSERT INTO task (project_id, parent_task_id, title, status, deadline, urgency, importance)
                VALUES (?, ?, ?, ?, CAST(? AS date), ?, ?) RETURNING id
                """, Long.class, projectId, parentTaskId, title, status, deadline, urgency, importance);
        for (String username : assignees) {
            jdbc.update("INSERT INTO task_assignee (task_id, user_id) VALUES (?, ?)", id, userId(username));
        }
        return id;
    }

    private static List<String> titles(ResponseEntity<String> list) {
        return JsonPath.read(list.getBody(), "$[*].title");
    }

    private static String quadrantOf(ResponseEntity<String> list, String title) {
        List<String> quadrants = JsonPath.read(list.getBody(), "$[?(@.title=='" + title + "')].quadrant");
        return quadrants.get(0);
    }
}
