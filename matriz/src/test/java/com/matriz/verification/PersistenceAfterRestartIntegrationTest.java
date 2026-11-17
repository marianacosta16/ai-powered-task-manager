package com.matriz.verification;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import com.jayway.jsonpath.JsonPath;
import com.matriz.MatrizApplication;
import com.matriz.testsupport.ApiIntegrationTest;

/**
 * Exercise 8, case 03 (persistence of tasks): a task created through the API is found by a later query, and it is still
 * there, with the same data, <b>after the backend is restarted</b>.
 *
 * The restart is real in what matters: a second application is started on the same database, with its own memory (so
 * nothing the first one kept in memory can be what is read) while the first one, which created the task, stays as it
 * was. The sessions live in memory (P32): the cookie of the first application is not accepted by the second, and the
 * user signs in again, as after a real restart.
 */
class PersistenceAfterRestartIntegrationTest extends ApiIntegrationTest {

    @Test
    void aTaskAndItsCommentCreatedThroughTheApiSurviveARestartOfTheBackend() {
        long project = insertProject("Restart", "In progress", "2026-01-01", "2027-12-31", "marta", "ana");
        String ana = sessionOf("ana");

        // 1. create, and find it with a query right away (the same application)
        ResponseEntity<String> created = post("/projects/" + project + "/tasks", ana, Map.of(
                "title", "Written before the restart", "description", "Kept in the database", "deadline", "2026-12-15",
                "urgency", 4, "importance", 2, "assigneeIds", List.of(userId("ana"))));
        assertThat(created.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        int task = JsonPath.read(created.getBody(), "$.id");
        assertThat(post("/tasks/" + task + "/comments", ana, Map.of("content", "A comment before the restart")).getStatusCode())
                .isEqualTo(HttpStatus.CREATED);
        ResponseEntity<String> beforeRestart = get("/tasks/" + task, ana);
        assertThat(JsonPath.<String>read(beforeRestart.getBody(), "$.title")).isEqualTo("Written before the restart");
        assertThat(JsonPath.<List<String>>read(get("/projects/" + project + "/tasks", ana).getBody(), "$[*].title"))
                .contains("Written before the restart");

        // 2. "restart": another application on the same database, with nothing in its memory
        try (ConfigurableApplicationContext restarted = new SpringApplicationBuilder(MatrizApplication.class)
                .profiles("test").properties("server.port=0").run()) {
            String root = "http://localhost:" + restarted.getEnvironment().getProperty("local.server.port");
            TestRestTemplate client = new TestRestTemplate();

            // the sessions are in memory (P32): the cookie of before is not accepted, and the user signs in again
            assertThat(call(client, root + "/tasks/" + task, HttpMethod.GET, ana, null).getStatusCode())
                    .isEqualTo(HttpStatus.UNAUTHORIZED);
            ResponseEntity<String> login = call(client, root + "/auth/login", HttpMethod.POST, null,
                    Map.of("username", "ana", "password", password));
            assertThat(login.getStatusCode()).isEqualTo(HttpStatus.OK);
            String cookie = login.getHeaders().getFirst(HttpHeaders.SET_COOKIE).split(";")[0];

            // 3. the task is there, with the same data, in the list and by itself, and so is its comment
            ResponseEntity<String> afterRestart = call(client, root + "/tasks/" + task, HttpMethod.GET, cookie, null);
            assertThat(afterRestart.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(JsonPath.<String>read(afterRestart.getBody(), "$.title")).isEqualTo("Written before the restart");
            assertThat(JsonPath.<String>read(afterRestart.getBody(), "$.description")).isEqualTo("Kept in the database");
            assertThat(JsonPath.<String>read(afterRestart.getBody(), "$.status")).isEqualTo("To do");
            assertThat(JsonPath.<String>read(afterRestart.getBody(), "$.deadline")).isEqualTo("2026-12-15");
            assertThat(JsonPath.<Integer>read(afterRestart.getBody(), "$.urgency")).isEqualTo(4);
            assertThat(JsonPath.<Integer>read(afterRestart.getBody(), "$.importance")).isEqualTo(2);
            assertThat(JsonPath.<String>read(afterRestart.getBody(), "$.assignees[0].name")).isEqualTo("Ana");
            assertThat(JsonPath.<List<String>>read(
                    call(client, root + "/projects/" + project + "/tasks", HttpMethod.GET, cookie, null).getBody(), "$[*].title"))
                    .contains("Written before the restart");
            assertThat(JsonPath.<List<String>>read(
                    call(client, root + "/tasks/" + task + "/comments", HttpMethod.GET, cookie, null).getBody(), "$[*].content"))
                    .containsExactly("A comment before the restart");
        }
    }

    private static ResponseEntity<String> call(TestRestTemplate client, String url, HttpMethod method, String cookie, Object body) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        if (cookie != null) {
            headers.add(HttpHeaders.COOKIE, cookie);
        }
        return client.exchange(url, method, new HttpEntity<>(body, headers), String.class);
    }
}
