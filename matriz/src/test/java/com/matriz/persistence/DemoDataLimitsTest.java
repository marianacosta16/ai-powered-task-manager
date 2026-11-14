package com.matriz.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.matriz.shared.Limits;
import com.matriz.shared.Texts;
import com.matriz.testsupport.ApiIntegrationTest;

/**
 * The demonstration data respects the limits that the API enforces (R14, P17, A45, A46), so what the seed puts in the
 * database is something the API would accept and the contract describes. A later edit of the seed with a title of
 * two characters, or a description that is too long, fails here and not for the first reader of the data.
 */
class DemoDataLimitsTest extends ApiIntegrationTest {

    @Test
    void theSeededProjectsRespectTheLimits() {
        List<String> problems = new ArrayList<>();
        jdbc.query("SELECT id, name, description FROM project", rs -> {
            check(problems, "project " + rs.getLong("id") + " name", rs.getString("name"), Limits.PROJECT_NAME_MIN,
                    Limits.PROJECT_NAME_MAX, true);
            check(problems, "project " + rs.getLong("id") + " description", rs.getString("description"), 0,
                    Limits.PROJECT_DESCRIPTION_MAX, false);
        });

        assertThat(count("project")).isPositive();
        assertThat(problems).isEmpty();
    }

    @Test
    void theSeededTasksAndSubtasksRespectTheLimits() {
        List<String> problems = new ArrayList<>();
        jdbc.query("SELECT id, title, description, block_reason FROM task", rs -> {
            check(problems, "task " + rs.getLong("id") + " title", rs.getString("title"), Limits.TASK_TITLE_MIN,
                    Limits.TASK_TITLE_MAX, true);
            check(problems, "task " + rs.getLong("id") + " description", rs.getString("description"), 0,
                    Limits.TASK_DESCRIPTION_MAX, false);
            check(problems, "task " + rs.getLong("id") + " block reason", rs.getString("block_reason"), 0, Limits.BLOCK_REASON_MAX, false);
        });

        assertThat(count("task")).isPositive();
        assertThat(problems).isEmpty();
    }

    @Test
    void theSeededCommentsRespectTheLimits() {
        List<String> problems = new ArrayList<>();
        jdbc.query("SELECT id, content FROM task_comment", rs -> {
            check(problems, "comment " + rs.getLong("id"), rs.getString("content"), 1, Limits.COMMENT_MAX, true);
        });

        assertThat(count("task_comment")).isPositive();
        assertThat(problems).isEmpty();
    }

    private static void check(List<String> problems, String what, String text, int min, int max, boolean notOnlySpaces) {
        if (text == null) {
            if (min > 0) {
                problems.add(what + " is missing");
            }
            return;
        }
        int length = text.codePointCount(0, text.length());
        if (length < min || length > max) {
            problems.add(what + " has " + length + " characters, not " + min + " to " + max);
        }
        if (notOnlySpaces && Texts.isOnlySpaces(text)) {
            problems.add(what + " is only spaces");
        }
    }
}
