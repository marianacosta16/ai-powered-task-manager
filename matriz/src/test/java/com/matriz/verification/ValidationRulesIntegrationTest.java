package com.matriz.verification;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.LongFunction;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import com.matriz.testsupport.ApiIntegrationTest;

/**
 * Exercise 8, case 04 (data validation): required fields that are empty or hold only spaces, and invalid values, are
 * refused with 400 or 422 and the error body of the contract, and <b>nothing is stored</b>, for a project, a task and a
 * comment, through real HTTP requests. One table of bad inputs for the three entities, checked against the rows of the
 * database before and after.
 */
class ValidationRulesIntegrationTest extends ApiIntegrationTest {

    // ---- the inputs that must be refused ----

    /** The texts that cannot be a name, a title or a comment: empty and with nothing visible (A25, A46). */
    static Stream<String> emptyOrNothingVisible() {
        return Stream.concat(Stream.of("", " "), NOTHING_VISIBLE.stream());
    }

    @ParameterizedTest(name = "a project named [{0}] is refused and not stored")
    @MethodSource("emptyOrNothingVisible")
    void aProjectNameThatIsEmptyOrOnlySpacesIsRefused(String name) {
        int projects = count("project");

        ResponseEntity<String> response = post("/projects", sessionOf("marta"), project(name, "2026-06-10", "2026-07-10"));

        assertRefused(response);
        assertThat(count("project")).isEqualTo(projects);
    }

    @ParameterizedTest(name = "a task titled [{0}] is refused and not stored")
    @MethodSource("emptyOrNothingVisible")
    void aTaskTitleThatIsEmptyOrOnlySpacesIsRefused(String title) {
        long project = newProject();
        int tasks = count("task");

        ResponseEntity<String> response = post("/projects/" + project + "/tasks", sessionOf("ana"), task(title, "2026-12-15"));

        assertRefused(response);
        assertThat(count("task")).isEqualTo(tasks);
    }

    @ParameterizedTest(name = "a comment [{0}] is refused and not stored")
    @MethodSource("emptyOrNothingVisible")
    void aCommentThatIsEmptyOrOnlySpacesIsRefused(String content) {
        long project = newProject();
        long task = insertTask(project, null, "Existing", "To do", "2026-12-15", 3, 3, "ana");
        int comments = count("task_comment");

        ResponseEntity<String> response = post("/tasks/" + task + "/comments", sessionOf("ana"), Map.of("content", content));

        assertRefused(response);
        assertThat(count("task_comment")).isEqualTo(comments);
    }

    @Test
    void aMissingRequiredFieldIsRefusedInEachEntity() {
        long project = newProject();
        long task = insertTask(project, null, "Existing", "To do", "2026-12-15", 3, 3, "ana");
        int projects = count("project");
        int tasks = count("task");
        int comments = count("task_comment");

        Map<String, Object> projectWithoutName = project("x", "2026-06-10", "2026-07-10");
        projectWithoutName.remove("name");
        Map<String, Object> projectWithoutEnd = project("Valid name", "2026-06-10", "2026-07-10");
        projectWithoutEnd.remove("endDate");
        Map<String, Object> taskWithoutTitle = task("x", "2026-12-15");
        taskWithoutTitle.remove("title");
        Map<String, Object> taskWithoutDeadline = task("Valid title", "2026-12-15");
        taskWithoutDeadline.remove("deadline");

        assertRefused(post("/projects", sessionOf("marta"), projectWithoutName));
        assertRefused(post("/projects", sessionOf("marta"), projectWithoutEnd));
        assertRefused(post("/projects/" + project + "/tasks", sessionOf("ana"), taskWithoutTitle));
        assertRefused(post("/projects/" + project + "/tasks", sessionOf("ana"), taskWithoutDeadline));
        assertRefused(post("/tasks/" + task + "/comments", sessionOf("ana"), Map.of()));
        assertThat(List.of(count("project"), count("task"), count("task_comment"))).containsExactly(projects, tasks, comments);
    }

    @ParameterizedTest(name = "{0} is refused")
    @MethodSource("invalidValues")
    void anInvalidValueIsRefusedAndNothingIsStored(String description, String path, LongFunction<Map<String, Object>> body) {
        long project = newProject();
        int projects = count("project");
        int tasks = count("task");

        ResponseEntity<String> response = post(path.replace("{project}", String.valueOf(project)), sessionOf("marta"),
                body.apply(userId("ana")));

        assertRefused(response);
        assertThat(count("project")).isEqualTo(projects);
        assertThat(count("task")).isEqualTo(tasks);
    }

    /** Description, path and the body (which needs the id of the assignee, known only when the test runs). */
    static Stream<Arguments> invalidValues() {
        return Stream.of(
                bad("a project ending before it starts", "/projects", ana -> project("Valid name", "2026-07-10", "2026-06-10")),
                bad("a project starting in the past", "/projects", ana -> project("Valid name", "2025-01-01", "2026-07-10")),
                bad("a project name of 2 characters", "/projects", ana -> project("ab", "2026-06-10", "2026-07-10")),
                bad("a project name of 101 characters", "/projects", ana -> project("n".repeat(101), "2026-06-10", "2026-07-10")),
                bad("a project phase that does not exist", "/projects",
                        ana -> with(project("Valid name", "2026-06-10", "2026-07-10"), "phase", "Retirement")),
                bad("a project status that is calculated, not chosen", "/projects",
                        ana -> with(project("Valid name", "2026-06-10", "2026-07-10"), "status", "Late")),
                bad("a task with a deadline in the past", "/projects/{project}/tasks", ana -> taskBody("Valid title", "2026-05-01", ana)),
                bad("a task with urgency 0", "/projects/{project}/tasks",
                        ana -> with(taskBody("Valid title", "2026-12-15", ana), "urgency", 0)),
                bad("a task with importance 5", "/projects/{project}/tasks",
                        ana -> with(taskBody("Valid title", "2026-12-15", ana), "importance", 5)),
                bad("a task with a title of 2 characters", "/projects/{project}/tasks", ana -> taskBody("ab", "2026-12-15", ana)),
                bad("a task with a title of 151 characters", "/projects/{project}/tasks",
                        ana -> taskBody("t".repeat(151), "2026-12-15", ana)),
                bad("a task with no assignee", "/projects/{project}/tasks",
                        ana -> with(taskBody("Valid title", "2026-12-15", ana), "assigneeIds", List.of())),
                bad("a task with a description of 2001 characters", "/projects/{project}/tasks",
                        ana -> with(taskBody("Valid title", "2026-12-15", ana), "description", "d".repeat(2001))));
    }

    private static Arguments bad(String description, String path, LongFunction<Map<String, Object>> body) {
        return Arguments.of(description, path, body);
    }

    @Test
    void aCommentOfMoreThan2000CharactersIsRefusedAndOneOf2000IsAccepted() {
        long project = newProject();
        long task = insertTask(project, null, "Existing", "To do", "2026-12-15", 3, 3, "ana");
        int comments = count("task_comment"); // the demo data has comments of its own

        ResponseEntity<String> tooLong = post("/tasks/" + task + "/comments", sessionOf("ana"), Map.of("content", "c".repeat(2001)));
        ResponseEntity<String> longest = post("/tasks/" + task + "/comments", sessionOf("ana"), Map.of("content", "c".repeat(2000)));

        assertRefused(tooLong);
        assertThat(longest.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(count("task_comment")).isEqualTo(comments + 1); // only the one of 2000
    }

    @Test
    void aValidInputIsAcceptedSoTheTableAboveDoesNotRefuseEverything() {
        long project = newProject();

        ResponseEntity<String> createdProject = post("/projects", sessionOf("marta"), project("A valid project", "2026-06-10", "2026-07-10"));
        ResponseEntity<String> createdTask = post("/projects/" + project + "/tasks", sessionOf("ana"), task("A valid task", "2026-12-15"));

        assertThat(createdProject.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(createdTask.getStatusCode()).isEqualTo(HttpStatus.CREATED);
    }

    // ---- helpers ----

    /** The API answers a refusal with 400 (the request is not valid) or 422 (it breaks a rule), and the error body of the contract. */
    private void assertRefused(ResponseEntity<String> response) {
        assertThat(response.getStatusCode()).isIn(HttpStatus.BAD_REQUEST, HttpStatus.UNPROCESSABLE_ENTITY);
        assertThat(json(response, "$.code")).isInstanceOf(String.class);
        assertThat((String) json(response, "$.message")).isNotBlank();
    }

    private long newProject() {
        return insertProject("Validation", "In progress", "2026-01-01", "2027-12-31", "marta", "ana");
    }

    private static Map<String, Object> project(String name, String start, String end) {
        Map<String, Object> body = new HashMap<>();
        body.put("name", name);
        body.put("startDate", start);
        body.put("endDate", end);
        body.put("phase", "Initiation");
        body.put("status", "Not started");
        return body;
    }

    private Map<String, Object> task(String title, String deadline) {
        return taskBody(title, deadline, userId("ana"));
    }

    private static Map<String, Object> taskBody(String title, String deadline, long assignee) {
        Map<String, Object> body = new HashMap<>();
        body.put("title", title);
        body.put("deadline", deadline);
        body.put("urgency", 3);
        body.put("importance", 3);
        body.put("assigneeIds", List.of(assignee));
        return body;
    }

    private static Map<String, Object> with(Map<String, Object> body, String key, Object value) {
        body.put(key, value);
        return body;
    }
}
