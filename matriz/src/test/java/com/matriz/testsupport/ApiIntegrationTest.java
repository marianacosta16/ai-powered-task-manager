package com.matriz.testsupport;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.PlatformTransactionManager;

import com.jayway.jsonpath.JsonPath;
import com.matriz.usecases.DemoDataSeeder;

/**
 * Base of the tests that call the API over real HTTP against the test database. Before each test
 * the database is emptied and filled with the demo data, and the clock is set to 2026-06-01.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Import(ApiIntegrationTest.TestClock.class)
public abstract class ApiIntegrationTest {

    @TestConfiguration
    static class TestClock {
        @Bean
        @Primary
        MutableClock testClock() {
            return new MutableClock();
        }
    }

    @Autowired protected TestRestTemplate http;
    @Autowired protected JdbcTemplate jdbc;
    @Autowired protected DemoDataSeeder seeder;
    @Autowired protected MutableClock clock;
    @Autowired protected PlatformTransactionManager transactions;

    protected final String password = UUID.randomUUID().toString();

    @BeforeEach
    void freshDemoDataAndDate() {
        TestDatabase.wipe(jdbc);
        seeder.seed(password);
        clock.set("2026-06-01T10:00:00Z");
    }

    protected ResponseEntity<String> signIn(String username) {
        return exchange("/auth/login", HttpMethod.POST, null, Map.of("username", username, "password", password));
    }

    /** Signs in and returns the cookie to send in the next requests. */
    protected String sessionOf(String username) {
        String setCookie = signIn(username).getHeaders().getFirst(HttpHeaders.SET_COOKIE);
        assertThat(setCookie).as("session cookie of " + username).isNotNull();
        return setCookie.substring(0, setCookie.indexOf(';'));
    }

    protected ResponseEntity<String> get(String path, String cookie) {
        return exchange(path, HttpMethod.GET, cookie, null);
    }

    protected ResponseEntity<String> post(String path, String cookie, Object body) {
        return exchange(path, HttpMethod.POST, cookie, body);
    }

    protected ResponseEntity<String> patch(String path, String cookie, Object body) {
        return exchange(path, HttpMethod.PATCH, cookie, body);
    }

    protected ResponseEntity<String> delete(String path, String cookie) {
        return exchange(path, HttpMethod.DELETE, cookie, null);
    }

    protected ResponseEntity<String> exchange(String path, HttpMethod method, String cookie, Object body) {
        HttpHeaders headers = new HttpHeaders();
        if (cookie != null) {
            headers.add(HttpHeaders.COOKIE, cookie);
        }
        if (body != null) {
            headers.setContentType(MediaType.APPLICATION_JSON);
        }
        return http.exchange(path, method, new HttpEntity<>(body, headers), String.class);
    }

    protected long projectId(String name) {
        return jdbc.queryForObject("SELECT id FROM project WHERE name = ?", Long.class, name);
    }

    protected long userId(String username) {
        return jdbc.queryForObject("SELECT id FROM app_user WHERE username = ?", Long.class, username);
    }

    protected long taskId(String title) {
        return jdbc.queryForObject("SELECT id FROM task WHERE title = ?", Long.class, title);
    }

    /**
     * Inserts a project straight into the database, so that the rule of one open project per member (R11) does
     * not get in the way of a test set-up. Named "name#unique" so that names never clash; the manager and the
     * members are given by username.
     */
    protected long insertProject(String name, String status, String start, String end, String manager, String... members) {
        long id = jdbc.queryForObject("""
                INSERT INTO project (name, start_date, end_date, phase, status, manager_id)
                VALUES (?, CAST(? AS date), CAST(? AS date), 'Execution', ?, ?) RETURNING id
                """, Long.class, name + "#" + System.nanoTime(), start, end, status, userId(manager));
        for (String member : members) {
            jdbc.update("INSERT INTO project_member (project_id, user_id) VALUES (?, ?)", id, userId(member));
        }
        return id;
    }

    /** Inserts a task, or a subtask when a parent is given, straight into the database, with its assignees (by username). */
    protected long insertTask(long projectId, Long parentTaskId, String title, String status, String deadline, int urgency,
            int importance, String... assignees) {
        long id = jdbc.queryForObject("""
                INSERT INTO task (project_id, parent_task_id, title, status, deadline, urgency, importance)
                VALUES (?, ?, ?, ?, CAST(? AS date), ?, ?) RETURNING id
                """, Long.class, projectId, parentTaskId, title, status, deadline, urgency, importance);
        for (String username : assignees) {
            jdbc.update("INSERT INTO task_assignee (task_id, user_id) VALUES (?, ?)", id, userId(username));
        }
        return id;
    }

    protected int count(String table) {
        return jdbc.queryForObject("SELECT count(*) FROM " + table, Integer.class);
    }

    protected static Object json(ResponseEntity<String> response, String path) {
        return JsonPath.read(response.getBody(), path);
    }

    /**
     * Waits until at least this many database sessions are waiting for a lock, that is, until the
     * requests of the test have reached the row that the test holds. Fails if they never do, so a
     * test cannot pass without having tested the lock.
     */
    protected void awaitLockWaiters(int expected) {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(15);
        while (System.nanoTime() < deadline) {
            Integer waiting = jdbc.queryForObject("SELECT count(*) FROM pg_stat_activity "
                    + "WHERE datname = current_database() AND wait_event_type = 'Lock'", Integer.class);
            if (waiting != null && waiting >= expected) {
                return;
            }
            pause(50);
        }
        throw new AssertionError("The requests did not reach the lock: expected " + expected + " waiting sessions");
    }

    protected static void pause(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
        }
    }
}
