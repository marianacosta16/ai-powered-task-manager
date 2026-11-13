package com.matriz.testsupport;

import org.springframework.jdbc.core.JdbcTemplate;

/** Empties the application tables, but only in a database whose name ends with _test. */
public final class TestDatabase {

    private TestDatabase() {
    }

    public static void wipe(JdbcTemplate jdbc) {
        requireTestDatabase(jdbc.queryForObject("SELECT current_database()", String.class));
        jdbc.execute("TRUNCATE task_comment, task_assignee, task, project_member, project, app_user "
                + "RESTART IDENTITY CASCADE");
    }

    static void requireTestDatabase(String name) {
        if (name == null || !name.endsWith("_test")) {
            throw new IllegalStateException("Refusing to empty the database '" + name + "': the tests only run "
                    + "against a database whose name ends with _test. Check TEST_DB_URL.");
        }
    }
}
