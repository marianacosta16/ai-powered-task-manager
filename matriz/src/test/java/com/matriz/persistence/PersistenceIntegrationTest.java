package com.matriz.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import com.matriz.usecases.DemoDataSeeder;

/** Runs against the separate test database (matriz_test); see README. */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class PersistenceIntegrationTest {

    @Autowired JdbcTemplate jdbc;
    @Autowired DemoDataSeeder seeder;
    @Autowired TestRestTemplate http;

    @BeforeEach
    void emptyTestDatabase() {
        jdbc.execute("TRUNCATE task_comment, task_assignee, task, project_member, project, app_user "
                + "RESTART IDENTITY CASCADE");
    }

    @Test
    void healthCheckReportsDatabaseUp() {
        // Given the application started against the test database
        // When the health endpoint is called
        String body = http.getForObject("/actuator/health", String.class);

        // Then the application and the database are UP
        assertThat(body).contains("\"status\":\"UP\"").contains("\"db\"");
    }

    @Test
    void demoDataHasDistinctAccessesAndStartingAgainDoesNotChangeIt() {
        // Given an empty database
        String password = UUID.randomUUID().toString();

        // When the demo data is created, and then "started" again
        boolean first = seeder.seed(password);
        int users = count("app_user");
        int projects = count("project");
        int tasks = count("task");
        int comments = count("task_comment");
        boolean second = seeder.seed(password);

        // Then there are 10 accounts and 4 projects, and nothing is deleted or duplicated
        assertThat(first).isTrue();
        assertThat(second).isFalse();
        assertThat(users).isEqualTo(10);
        assertThat(projects).isEqualTo(4);
        assertThat(count("app_user")).isEqualTo(users);
        assertThat(count("project")).isEqualTo(projects);
        assertThat(count("task")).isEqualTo(tasks);
        assertThat(count("task_comment")).isEqualTo(comments);

        // And the accounts see different projects (member access) and passwords are not plain text
        assertThat(projectsOf("ana")).containsExactlyInAnyOrder("Website");
        assertThat(projectsOf("ines")).containsExactlyInAnyOrder("Mobile App");
        assertThat(projectsOf("sofia")).containsExactlyInAnyOrder("Intranet");
        assertThat(jdbc.queryForObject("SELECT count(*) FROM app_user WHERE password = ?", Integer.class, password))
                .isZero();
    }

    @Test
    void demoPasswordShorterThanEightCharactersIsRefused() {
        // Given an empty database, when the demo data is created with a 7-character password (S5)
        // Then it is refused and nothing is created
        assertThatThrownBy(() -> seeder.seed("1234567")).isInstanceOf(IllegalArgumentException.class);
        assertThat(count("app_user")).isZero();
    }

    @Test
    void shortDemoPasswordIsIgnoredWhenTheDataAlreadyExists() {
        // Given a database that already has the demo data
        seeder.seed(UUID.randomUUID().toString());
        int users = count("app_user");

        // When the app starts again with a password shorter than 8 characters in the environment
        boolean created = seeder.seed("abc");

        // Then it starts normally: nothing is created, changed or refused
        assertThat(created).isFalse();
        assertThat(count("app_user")).isEqualTo(users);
    }

    private int count(String table) {
        return jdbc.queryForObject("SELECT count(*) FROM " + table, Integer.class);
    }

    private java.util.List<String> projectsOf(String username) {
        return jdbc.queryForList("""
                SELECT p.name FROM project p
                JOIN project_member m ON m.project_id = p.id
                JOIN app_user u ON u.id = m.user_id
                WHERE u.username = ?
                """, String.class, username);
    }
}
