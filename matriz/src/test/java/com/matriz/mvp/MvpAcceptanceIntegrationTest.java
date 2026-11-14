package com.matriz.mvp;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import com.jayway.jsonpath.JsonPath;
import com.matriz.testsupport.ApiIntegrationTest;

/**
 * The acceptance scenario of the MVP, from the project to the progress (scenario MVP-1 of docs/user-stories.md),
 * from the first sign in to the dashboard, with nothing but the API and a clock that the test moves.
 *
 * <p>The scenario starts on 2026-01-01 with the accounts of Marta and Ana and Ana in no open project, so the
 * demonstration projects are taken out first. What the Matrix hides (the tasks that are closed, P10) is the work
 * of the interface (plan, section 8, V10): here the API gives the status, and the open tasks are the ones the
 * Matrix shows.
 */
class MvpAcceptanceIntegrationTest extends ApiIntegrationTest {

    @Test
    void fromTheProjectToTheProgress() { // MVP-1
        // Given the date 2026-01-01, the accounts of Marta and Ana, and Ana in no open project
        removeTheDemonstrationProjects();
        clock.set("2026-01-01T09:00:00Z");

        // When Marta signs in, creates the project "Website", adds Ana and moves it to "In progress"
        String marta = sessionOf("marta");
        ResponseEntity<String> created = post("/projects", marta, Map.of("name", "Website", "startDate", "2026-01-01",
                "endDate", "2026-01-31", "phase", "Initiation", "status", "Not started"));
        assertThat(created.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        long website = ((Number) json(created, "$.id")).longValue();
        assertThat(post("/projects/" + website + "/members", marta, Map.of("userId", userId("ana"))).getStatusCode())
                .isEqualTo(HttpStatus.CREATED);
        assertThat(patch("/projects/" + website, marta, Map.of("status", "In progress")).getStatusCode()).isEqualTo(HttpStatus.OK);

        // And Ana signs in and creates the tasks "Escrever relatório" and "Rever texto"
        String ana = sessionOf("ana");
        long report = createTask(ana, website, "Escrever relatório", "2026-01-20", 4, 4);
        long review = createTask(ana, website, "Rever texto", "2026-01-25", 1, 3);

        // And on 2026-01-10 Ana moves the first one to "Done" and comments on the second
        clock.set("2026-01-10T12:00:00Z");
        ResponseEntity<String> done = patch("/tasks/" + report, ana, Map.of("version", 1, "status", "Done"));
        assertThat(done.getStatusCode()).isEqualTo(HttpStatus.OK);
        ResponseEntity<String> comment = post("/tasks/" + review + "/comments", ana, Map.of("content", "Falta o capítulo 2"));
        assertThat(comment.getStatusCode()).isEqualTo(HttpStatus.CREATED);

        // Then the Matrix shows "Rever texto" in the quadrant "Schedule" and not "Escrever relatório"
        ResponseEntity<String> tasks = get("/projects/" + website + "/tasks", ana);
        List<Map<String, Object>> all = JsonPath.read(tasks.getBody(), "$");
        List<Object> openTitles = all.stream().filter(t -> !List.of("Done", "Cancelled", "Duplicate").contains(t.get("status")))
                .map(t -> t.get("title")).toList();
        assertThat(openTitles).containsExactly("Rever texto"); // what the Matrix shows: the tasks that are not closed (P10)
        assertThat(JsonPath.<List<String>>read(tasks.getBody(), "$[?(@.title=='Rever texto')].quadrant")).containsExactly("Schedule");
        assertThat(JsonPath.<List<String>>read(tasks.getBody(), "$[?(@.title=='Escrever relatório')].status")).containsExactly("Done");

        // And the List shows both tasks, with "Rever texto" first, and filtered by "To do" only "Rever texto"
        assertThat(JsonPath.<List<String>>read(tasks.getBody(), "$[*].title")).containsExactly("Rever texto", "Escrever relatório");
        ResponseEntity<String> toDo = get("/projects/" + website + "/tasks?status=To do", ana);
        assertThat(JsonPath.<List<String>>read(toDo.getBody(), "$[*].title")).containsExactly("Rever texto");

        // And the comment is there
        assertThat(JsonPath.<List<String>>read(get("/tasks/" + review + "/comments", ana).getBody(), "$[*].content"))
                .containsExactly("Falta o capítulo 2");

        // And searching the projects for "web" shows the project "Website"
        assertThat(JsonPath.<List<String>>read(get("/projects?search=web", ana).getBody(), "$[*].name")).containsExactly("Website");

        // And Marta sees on the dashboard the project "Website" with 50% progress and the status "In progress"
        // (about 30% of the time has passed: 9 and a half days of 31)
        List<Map<String, Object>> dashboard = JsonPath.read(get("/dashboard", marta).getBody(), "$[?(@.project.name=='Website')]");
        assertThat(dashboard).hasSize(1);
        Map<String, Object> entry = dashboard.get(0);
        assertThat(((Map<?, ?>) entry.get("project")).get("status")).isEqualTo("In progress");
        Map<?, ?> progress = (Map<?, ?>) entry.get("progress");
        assertThat(((Number) progress.get("percent")).doubleValue()).isEqualTo(50.0);
        assertThat(progress.get("calculatedStatus")).isNull(); // not "Attention" nor "Late": the time passed is 30%
        assertThat(entry.get("counts")).isEqualTo(Map.of("toDo", 1, "inProgress", 0, "inReview", 0, "blocked", 0, "done", 1,
                "cancelled", 0, "duplicate", 0));
        // and Ana, a member, sees the counts but not the progress
        List<Map<String, Object>> asMember = JsonPath.read(get("/dashboard", ana).getBody(), "$[?(@.project.name=='Website')]");
        assertThat(asMember.get(0)).containsKey("counts").doesNotContainKey("progress");
    }

    private long createTask(String session, long project, String title, String deadline, int urgency, int importance) {
        ResponseEntity<String> created = post("/projects/" + project + "/tasks", session, Map.of("title", title, "deadline", deadline,
                "urgency", urgency, "importance", importance, "assigneeIds", List.of(userId("ana"))));
        assertThat(created.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        return ((Number) json(created, "$.id")).longValue();
    }

    /** The scenario starts with Ana in no open project: the demonstration projects and what they hold are removed. */
    private void removeTheDemonstrationProjects() {
        for (String table : List.of("task_comment", "task_assignee")) {
            jdbc.update("DELETE FROM " + table);
        }
        jdbc.update("UPDATE task SET original_task_id = NULL, parent_task_id = NULL");
        jdbc.update("DELETE FROM task");
        jdbc.update("DELETE FROM project_member");
        jdbc.update("DELETE FROM project");
    }
}
