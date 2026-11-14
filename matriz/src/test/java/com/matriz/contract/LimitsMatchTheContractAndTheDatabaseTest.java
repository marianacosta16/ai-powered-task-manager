package com.matriz.contract;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.matriz.shared.Limits;
import com.matriz.testsupport.ApiIntegrationTest;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.parser.OpenAPIV3Parser;
import io.swagger.v3.parser.core.models.ParseOptions;

/**
 * The limits of {@link Limits}, which the application enforces, are the ones the contract (openapi.yaml) says and
 * fit in the columns of the database. A limit changed in one place and not in the others would show here, and not
 * as a 500 from the database or a contract that lies. A column may be wider than the limit (the description of a
 * project is 1000 for the application and the column holds 2000), never narrower.
 */
class LimitsMatchTheContractAndTheDatabaseTest extends ApiIntegrationTest {

    private static final OpenAPI CONTRACT = read();

    @Test
    void theContractSaysTheLimitsOfTheApplication() {
        List<String> differences = new ArrayList<>();
        expect(differences, "ProjectCreate.name", property("ProjectCreate", "name"), Limits.PROJECT_NAME_MIN, Limits.PROJECT_NAME_MAX);
        expect(differences, "ProjectUpdate.name", property("ProjectUpdate", "name"), Limits.PROJECT_NAME_MIN, Limits.PROJECT_NAME_MAX);
        expect(differences, "ProjectCreate.description", property("ProjectCreate", "description"), null, Limits.PROJECT_DESCRIPTION_MAX);
        expect(differences, "ProjectUpdate.description", property("ProjectUpdate", "description"), null, Limits.PROJECT_DESCRIPTION_MAX);
        expect(differences, "Project.description", property("Project", "description"), null, Limits.PROJECT_DESCRIPTION_MAX);
        expect(differences, "TaskCreate.title", property("TaskCreate", "title"), Limits.TASK_TITLE_MIN, Limits.TASK_TITLE_MAX);
        expect(differences, "TaskUpdate.title", property("TaskUpdate", "title"), Limits.TASK_TITLE_MIN, Limits.TASK_TITLE_MAX);
        expect(differences, "Task.title", property("Task", "title"), null, Limits.TASK_TITLE_MAX);
        expect(differences, "TaskCreate.description", property("TaskCreate", "description"), null, Limits.TASK_DESCRIPTION_MAX);
        expect(differences, "TaskUpdate.description", property("TaskUpdate", "description"), null, Limits.TASK_DESCRIPTION_MAX);
        expect(differences, "Task.description", property("Task", "description"), null, Limits.TASK_DESCRIPTION_MAX);
        expect(differences, "CommentWrite.content", property("CommentWrite", "content"), null, Limits.COMMENT_MAX);

        assertThat(differences).as("limits of the contract that differ from Limits").isEmpty();
    }

    @Test
    void theColumnsOfTheDatabaseHoldTheLimits() {
        List<String> narrower = new ArrayList<>();
        column(narrower, "project", "name", Limits.PROJECT_NAME_MAX);
        column(narrower, "project", "description", Limits.PROJECT_DESCRIPTION_MAX);
        column(narrower, "task", "title", Limits.TASK_TITLE_MAX);
        column(narrower, "task", "description", Limits.TASK_DESCRIPTION_MAX);
        column(narrower, "task", "block_reason", Limits.BLOCK_REASON_MAX);
        column(narrower, "task_comment", "content", Limits.COMMENT_MAX);

        assertThat(narrower).as("columns narrower than the limit of the application").isEmpty();
    }

    // ---- helpers ----

    private static void expect(List<String> differences, String where, Schema<?> schema, Integer min, int max) {
        if (schema == null) {
            differences.add(where + " is not in the contract");
            return;
        }
        if (!Integer.valueOf(max).equals(schema.getMaxLength())) {
            differences.add(where + " maxLength is " + schema.getMaxLength() + ", Limits says " + max);
        }
        if (min != null && !min.equals(schema.getMinLength())) {
            differences.add(where + " minLength is " + schema.getMinLength() + ", Limits says " + min);
        }
    }

    private static Schema<?> property(String schema, String name) {
        Schema<?> found = CONTRACT.getComponents().getSchemas().get(schema);
        if (found == null) {
            return null;
        }
        if (found.getProperties() != null && found.getProperties().get(name) != null) {
            return found.getProperties().get(name);
        }
        if (found.getAllOf() != null) { // Project is the summary plus its own properties
            for (Schema<?> part : found.getAllOf()) {
                if (part.getProperties() != null && part.getProperties().get(name) != null) {
                    return part.getProperties().get(name);
                }
            }
        }
        return null;
    }

    /** A column of type text has no length (null): it holds any limit. */
    private void column(List<String> narrower, String table, String column, int limit) {
        List<Integer> lengths = jdbc.query("SELECT character_maximum_length FROM information_schema.columns "
                + "WHERE table_name = ? AND column_name = ? AND table_schema = current_schema()",
                (rs, row) -> (Integer) rs.getObject(1), table, column);
        assertThat(lengths).as(table + "." + column + " exists").hasSize(1);
        Integer length = lengths.get(0);
        if (length != null && length < limit) {
            narrower.add(table + "." + column + " holds " + length + ", the limit is " + limit);
        }
    }

    private static OpenAPI read() {
        ParseOptions options = new ParseOptions();
        options.setResolveFully(true);
        return new OpenAPIV3Parser().readLocation("openapi.yaml", null, options).getOpenAPI();
    }
}
