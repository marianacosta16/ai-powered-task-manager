package com.matriz.views;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import com.jayway.jsonpath.JsonPath;
import com.matriz.testsupport.ApiIntegrationTest;

/**
 * The dashboard (getDashboard): scenarios 6.1, 6.7 to 6.11, 6.13, 13.1 to 13.5 and EC1, EC16, EC24, through
 * real HTTP requests. The limits of the calculation are in ProjectProgressTest. The projects of each test
 * are created here; the seed ones are also in the answer, so each test looks for its own by name.
 */
class DashboardIntegrationTest extends ApiIntegrationTest {

    @Test
    void theCountsAreTheMainTasksByStatus() { // 13.1
        long project = newProject("Counts", "In progress", "2026-01-01", "2026-01-31", "marta", "ana");
        for (String status : List.of("To do", "To do", "In progress", "Done", "Cancelled")) {
            newTask(project, status, null);
        }

        @SuppressWarnings("unchecked")
        Map<String, Object> counts = (Map<String, Object>) entry("ana", "Counts").get("counts");

        assertThat(counts).containsEntry("toDo", 2).containsEntry("inProgress", 1).containsEntry("inReview", 0)
                .containsEntry("blocked", 0).containsEntry("done", 1).containsEntry("cancelled", 1)
                .containsEntry("duplicate", 0);
    }

    @Test
    void subtasksDoNotCountForTheCountsNorTheProgress() { // 13.4, 6.11
        clock.set("2026-01-10T12:00:00Z");
        long project = newProject("Subtasks", "In progress", "2026-01-01", "2026-01-31", "marta", "ana");
        newTask(project, "Done", null);
        long b = newTask(project, "In progress", null);
        newTask(project, "Done", b);
        newTask(project, "Done", b);
        newTask(project, "To do", b);

        Map<String, Object> mine = entry("marta", "Subtasks");

        assertThat(counts(mine)).isEqualTo(2);
        assertThat(((Number) progress(mine).get("percent")).doubleValue()).isEqualTo(50.0);
        assertThat(counts(entry("ana", "Subtasks"))).isEqualTo(2);
    }

    @Test
    void onlyTheProjectsOfTheUserAreShown() { // 13.2
        newProject("Mine", "In progress", "2026-01-01", "2026-01-31", "marta", "ana");
        newProject("Theirs", "In progress", "2026-01-01", "2026-01-31", "marta", "rui");

        assertThat(startingWith(names("ana"), "Mine")).isNotEmpty();
        assertThat(startingWith(names("ana"), "Theirs")).isEmpty();
        assertThat(startingWith(names("rui"), "Theirs")).isNotEmpty();
        assertThat(startingWith(names("rui"), "Mine")).isEmpty();
        assertThat(startingWith(names("marta"), "Mine")).isNotEmpty(); // the manager of both
        assertThat(startingWith(names("marta"), "Theirs")).isNotEmpty();
    }

    @Test
    void aMemberSeesTheCountsButNotTheProgressNorTheCalculatedStatus() { // 6.7
        clock.set("2026-01-16T12:00:00Z");
        long project = newProject("Hidden", "In progress", "2026-01-01", "2026-01-31", "marta", "ana");
        newTask(project, "To do", null);

        Map<String, Object> asMember = entry("ana", "Hidden");

        assertThat(asMember).containsKey("counts").doesNotContainKey("progress");
        assertThat(entry("marta", "Hidden")).containsKey("progress");
    }

    @Test
    void aManagerSeesNoProgressInAProjectHeIsOnlyAMemberOf() { // 6.7, R11: a manager can be a member elsewhere
        newProject("Other", "In progress", "2026-01-01", "2026-01-31", "pedro", "marta");

        assertThat(entry("marta", "Other")).doesNotContainKey("progress");
        assertThat(entry("pedro", "Other")).containsKey("progress");
    }

    @Test
    void theManagerSeesTheBarAndTheCalculatedStatus() { // 6.1, 6.3, 6.5, 6.6
        clock.set("2026-01-16T12:00:00Z");
        long project = newProject("Bar", "In progress", "2026-01-01", "2026-01-31", "marta", "ana");
        for (int i = 0; i < 4; i++) {
            newTask(project, i < 1 ? "Done" : "To do", null); // 25%
        }

        Map<String, Object> progress = progress(entry("marta", "Bar"));

        assertThat(progress.get("hasCountableTasks")).isEqualTo(true);
        assertThat(((Number) progress.get("percent")).doubleValue()).isEqualTo(25.0);
        assertThat(progress.get("calculatedStatus")).isEqualTo("Late"); // 50 - 25 = 25

        clock.set("2026-02-02T10:00:00Z"); // past the end
        assertThat(progress(entry("marta", "Bar")).get("calculatedStatus")).isEqualTo("Late");
        clock.set("2026-01-02T10:00:00Z");
        assertThat(progress(entry("marta", "Bar")).get("calculatedStatus")).isNull();
    }

    @Test
    void theTimeZoneOfTheProjectDecidesWhenTheEndDayEnds() { // 6.15, P26
        long project = newProject("Zone", "In progress", "2026-01-01", "2026-01-31", "marta", "ana");
        jdbc.update("UPDATE project SET timezone = 'America/New_York' WHERE id = ?", project);
        newTask(project, "Done", null); // 100%: only the end of the day can make it late

        clock.set("2026-02-01T04:30:00Z"); // 23:30 of 31 January in New York
        assertThat(progress(entry("marta", "Zone")).get("calculatedStatus")).isNull();
        clock.set("2026-02-01T05:00:00Z"); // midnight in New York
        assertThat(progress(entry("marta", "Zone")).get("calculatedStatus")).isEqualTo("Late");
    }

    @Test
    void aSuspendedProjectKeepsItsBarButGetsNoCalculatedStatus() { // 6.8
        clock.set("2026-02-02T10:00:00Z");
        long project = newProject("Paused", "Suspended", "2026-01-01", "2026-01-31", "marta", "ana");
        newTask(project, "Done", null);
        newTask(project, "To do", null);

        Map<String, Object> entry = entry("marta", "Paused");

        assertThat(((Map<?, ?>) entry.get("project")).get("status")).isEqualTo("Suspended");
        assertThat(((Number) progress(entry).get("percent")).doubleValue()).isEqualTo(50.0);
        assertThat(progress(entry).get("calculatedStatus")).isNull();
    }

    @Test
    void aProjectWithoutCountableTasksIsLabelledAndNeverCalculated() { // 6.2, 6.10, EC1, EC16
        clock.set("2026-02-02T10:00:00Z"); // past the end
        long onlyClosed = newProject("Closed only", "In progress", "2026-01-01", "2026-01-31", "marta", "ana");
        newTask(onlyClosed, "Cancelled", null);
        newTask(onlyClosed, "Duplicate", null);
        newProject("Empty", "In progress", "2026-01-01", "2026-01-31", "marta", "ana");

        for (String name : List.of("Closed only", "Empty")) {
            Map<String, Object> progress = progress(entry("marta", name));
            assertThat(progress.get("hasCountableTasks")).as(name).isEqualTo(false);
            assertThat(progress.get("percent")).as(name).isNull();
            assertThat(progress.get("calculatedStatus")).as(name).isNull();
        }
    }

    @Test
    void aProjectNotStartedKeepsItsStatusAfterTheEndDate() { // 6.13, EC24
        clock.set("2026-02-02T10:00:00Z");
        long project = newProject("Waiting", "Not started", "2026-01-01", "2026-01-31", "marta", "ana");
        newTask(project, "To do", null);

        Map<String, Object> entry = entry("marta", "Waiting");

        assertThat(((Map<?, ?>) entry.get("project")).get("status")).isEqualTo("Not started");
        assertThat(progress(entry).get("calculatedStatus")).isNull();
    }

    @Test
    void aCompletedProjectIsNotOnTheDashboard() { // 6.9, 13.5
        newProject("Finished", "Completed", "2026-01-01", "2026-01-31", "marta", "ana");

        assertThat(startingWith(names("marta"), "Finished")).isEmpty();
        assertThat(startingWith(names("ana"), "Finished")).isEmpty();
        assertThat(JsonPath.<List<String>>read(get("/projects?search=Finished", sessionOf("ana")).getBody(), "$[*].name"))
                .hasSize(1); // but the search finds it
    }

    @Test
    void deletedTasksAndDeletedProjectsDoNotCount() {
        long project = newProject("Trash", "In progress", "2026-01-01", "2026-01-31", "marta", "ana");
        long gone = newTask(project, "To do", null);
        newTask(project, "To do", null);
        jdbc.update("UPDATE task SET deleted_at = now() WHERE id = ?", gone);
        long deleted = newProject("Deleted", "In progress", "2026-01-01", "2026-01-31", "marta", "ana");
        jdbc.update("UPDATE project SET deleted_at = now() WHERE id = ?", deleted);

        assertThat(counts(entry("ana", "Trash"))).isEqualTo(1);
        assertThat(startingWith(names("ana"), "Deleted")).isEmpty();
    }

    @Test
    void theProjectsComeOrderedByName() { // A34
        newProject("Zz second", "In progress", "2026-01-01", "2026-01-31", "marta", "ana");
        newProject("Zz first", "In progress", "2026-01-01", "2026-01-31", "marta", "ana");
        newProject("zz third", "In progress", "2026-01-01", "2026-01-31", "marta", "ana");

        List<String> names = names("ana").stream().filter(n -> n.toLowerCase().startsWith("zz")).toList();

        assertThat(names).extracting(n -> n.substring(0, n.indexOf('#'))).containsExactly("Zz first", "Zz second", "zz third");
    }

    @Test
    void theDashboardNeedsASession() {
        assertThat(get("/dashboard", null).getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    // ---- helpers ----

    /** A project, named "name#unique" so that names never clash; manager and members given by username. */
    private long newProject(String name, String status, String start, String end, String manager, String... members) {
        long id = jdbc.queryForObject("""
                INSERT INTO project (name, start_date, end_date, phase, status, manager_id)
                VALUES (?, CAST(? AS date), CAST(? AS date), 'Execution', ?, ?) RETURNING id
                """, Long.class, name + "#" + System.nanoTime(), start, end, status, userId(manager));
        for (String member : members) {
            jdbc.update("INSERT INTO project_member (project_id, user_id) VALUES (?, ?)", id, userId(member));
        }
        return id;
    }

    private long newTask(long projectId, String status, Long parentTaskId) {
        long id = jdbc.queryForObject("""
                INSERT INTO task (project_id, parent_task_id, title, status, deadline, urgency, importance)
                VALUES (?, ?, 'T', ?, DATE '2026-12-31', 3, 3) RETURNING id
                """, Long.class, projectId, parentTaskId, status);
        jdbc.update("INSERT INTO task_assignee (task_id, user_id) VALUES (?, ?)", id, userId("ana"));
        return id;
    }

    private List<Map<String, Object>> dashboard(String username) {
        ResponseEntity<String> response = get("/dashboard", sessionOf(username));
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        return JsonPath.read(response.getBody(), "$");
    }

    /** The dashboard line of the project whose name starts with the given one. */
    private Map<String, Object> entry(String username, String name) {
        return dashboard(username).stream()
                .filter(e -> ((String) ((Map<?, ?>) e.get("project")).get("name")).startsWith(name + "#"))
                .findFirst().orElseThrow(() -> new AssertionError(name + " is not on the dashboard of " + username));
    }

    private List<String> names(String username) {
        return dashboard(username).stream().map(e -> (String) ((Map<?, ?>) e.get("project")).get("name")).toList();
    }

    private static List<String> startingWith(List<String> names, String prefix) {
        return names.stream().filter(n -> n.startsWith(prefix + "#")).toList();
    }

    private static int counts(Map<String, Object> entry) {
        return ((Map<?, ?>) entry.get("counts")).values().stream().mapToInt(v -> ((Number) v).intValue()).sum();
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> progress(Map<String, Object> entry) {
        return (Map<String, Object>) entry.get("progress");
    }
}
