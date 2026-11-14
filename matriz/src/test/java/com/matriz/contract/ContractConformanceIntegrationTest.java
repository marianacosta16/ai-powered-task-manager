package com.matriz.contract;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.matriz.testsupport.ApiIntegrationTest;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.parser.OpenAPIV3Parser;
import io.swagger.v3.parser.core.models.ParseOptions;

/**
 * The contract and the application say the same (the closing of the MVP, increment 8). Two checks:
 * the routes of the application are exactly the paths and methods of the contract, and every one of the 28
 * operations, called for real on the demonstration data, answers with a status the contract lists and a body
 * that fits the schema of that status, with no property the contract does not know, and with the headers the
 * contract lists for it. An error always has a code of the contract and a message that says what to do (NF08).
 * The request bodies are not checked against their schemas: only the responses are.
 */
class ContractConformanceIntegrationTest extends ApiIntegrationTest {

    private static final ObjectMapper JSON = new ObjectMapper();
    private static OpenAPI contract;
    private static final Map<String, Found> OPERATIONS = new LinkedHashMap<>();

    @Autowired @Qualifier("requestMappingHandlerMapping") RequestMappingHandlerMapping handlerMapping;

    /** An operation of the contract: where it is and what it answers. */
    private record Found(String id, HttpMethod method, String path, Operation operation) {
    }

    @BeforeAll
    static void readTheContract() {
        ParseOptions options = new ParseOptions();
        options.setResolveFully(true);
        contract = new OpenAPIV3Parser().readLocation("openapi.yaml", null, options).getOpenAPI();
        for (var path : contract.getPaths().entrySet()) {
            for (var operation : path.getValue().readOperationsMap().entrySet()) {
                OPERATIONS.put(operation.getValue().getOperationId(), new Found(operation.getValue().getOperationId(),
                        HttpMethod.valueOf(operation.getKey().name()), path.getKey(), operation.getValue()));
            }
        }
    }

    @Test
    void theRoutesOfTheApplicationAreTheOperationsOfTheContract() {
        Set<String> inTheApplication = new TreeSet<>();
        for (RequestMappingInfo info : handlerMapping.getHandlerMethods().keySet()) {
            var patterns = info.getPathPatternsCondition().getPatternValues();
            for (String pattern : patterns) {
                for (var method : info.getMethodsCondition().getMethods()) {
                    inTheApplication.add(method.name() + " " + pattern.replaceAll("\\{[^}/]+}", "{}"));
                }
            }
        }
        inTheApplication.removeIf(route -> route.contains("/error") || route.contains("/actuator"));
        Set<String> inTheContract = new TreeSet<>();
        OPERATIONS.values().forEach(o -> inTheContract.add(o.method().name() + " " + o.path().replaceAll("\\{[^}/]+}", "{}")));

        assertThat(inTheContract).hasSize(28);
        assertThat(inTheApplication).as("the routes of the application and those of the contract").isEqualTo(inTheContract);
    }

    @Test
    void everyOperationAnswersAsTheContractSays() {
        Calls calls = new Calls();
        long marta = userId("marta");
        long ana = userId("ana");
        long website = projectId("Website");
        clock.set("2026-11-14T10:00:00Z");

        // ---- sign in, who am I ----
        ResponseEntity<String> login = calls.call("signIn", null, Map.of(), Map.of("username", "marta", "password", password));
        String martaCookie = cookie(login);
        String anaCookie = cookie(calls.call("signIn", null, Map.of(), Map.of("username", "ana", "password", password)));
        String sofiaCookie = cookie(calls.call("signIn", null, Map.of(), Map.of("username", "sofia", "password", password)));
        calls.call("getAuthenticatedUser", martaCookie, Map.of(), null);
        calls.call("signIn", null, Map.of(), Map.of("username", "marta", "password", "wrong password")); // 401

        // ---- projects and team ----
        calls.call("listProjects", martaCookie, Map.of(), null);
        ResponseEntity<String> created = calls.call("createProject", martaCookie, Map.of(), Map.of("name", "Conformance",
                "description", "Checks the contract", "startDate", "2026-12-01", "endDate", "2027-03-31", "phase", "Initiation",
                "status", "Not started"));
        long project = id(created);
        calls.call("createProject", anaCookie, Map.of(), Map.of("name", "No", "startDate", "2026-12-01", "endDate", "2027-03-31",
                "phase", "Initiation", "status", "Not started")); // 403
        calls.call("createProject", martaCookie, Map.of(), Map.of("name", "Chosen manager", "startDate", "2026-12-01",
                "endDate", "2027-03-31", "phase", "Initiation", "status", "Not started", "managerId", ana)); // 400: the manager is the creator
        calls.call("getProject", martaCookie, Map.of("projectId", project), null);
        calls.call("getProject", sofiaCookie, Map.of("projectId", website), null); // 403
        calls.call("getProject", martaCookie, Map.of("projectId", 999999), null); // 404
        calls.call("updateProject", martaCookie, Map.of("projectId", project), Map.of("description", "Updated"));
        calls.call("listCandidateMembers", martaCookie, Map.of("projectId", project), null);
        calls.call("addMember", martaCookie, Map.of("projectId", project), Map.of("userId", userId("diana")));
        calls.call("addMember", martaCookie, Map.of("projectId", project), Map.of("userId", userId("diana"))); // 422
        calls.call("listMembers", martaCookie, Map.of("projectId", project), null);
        calls.call("listAssignableUsers", martaCookie, Map.of("projectId", website), null);

        // ---- tasks, subtasks and assignees ----
        ResponseEntity<String> task = calls.call("createTask", anaCookie, Map.of("projectId", website), Map.of("title", "Conformance",
                "deadline", "2026-12-20", "urgency", 3, "importance", 4, "assigneeIds", List.of(ana)));
        long taskId = id(task);
        ResponseEntity<String> subtask = calls.call("createTask", anaCookie, Map.of("projectId", website), Map.of("title", "Sub",
                "deadline", "2026-12-10", "urgency", 1, "importance", 1, "assigneeIds", List.of(ana), "parentTaskId", taskId));
        long subtaskId = id(subtask);
        calls.call("createTask", anaCookie, Map.of("projectId", website), Map.of("title", "", "deadline", "2026-12-20",
                "urgency", 3, "importance", 4, "assigneeIds", List.of(ana))); // 400
        calls.call("createTask", anaCookie, Map.of("projectId", website), Map.of("title", "   ", "deadline", "2026-12-20",
                "urgency", 3, "importance", 4, "assigneeIds", List.of(ana))); // 400: only spaces
        calls.call("listProjectTasks", anaCookie, Map.of("projectId", website), null);
        calls.call("listProjectTasks", anaCookie, Map.of("projectId", website), null, "mine=true&status=To%20do&quadrant=Do%20first&size=10");
        calls.call("listProjectTasks", anaCookie, Map.of("projectId", website), null, "size=500"); // 400
        calls.call("getTask", anaCookie, Map.of("taskId", taskId), null);
        calls.call("updateTask", anaCookie, Map.of("taskId", taskId), Map.of("version", 1, "status", "In progress"));
        calls.call("updateTask", anaCookie, Map.of("taskId", taskId), Map.of("version", 1, "title", "Stale")); // 409
        calls.call("addAssignee", anaCookie, Map.of("taskId", taskId), Map.of("userId", userId("rui")));
        calls.call("addAssignee", anaCookie, Map.of("taskId", taskId), Map.of("userId", userId("sofia"))); // 422
        calls.call("addAssignee", anaCookie, Map.of("taskId", taskId), Map.of("userId", userId("rui"))); // 409: already assigned
        calls.call("removeAssignee", anaCookie, Map.of("taskId", taskId, "userId", userId("rui")), null);
        calls.call("removeAssignee", anaCookie, Map.of("taskId", taskId, "userId", ana), null); // 422: the last one

        // ---- comments ----
        ResponseEntity<String> comment = calls.call("addComment", anaCookie, Map.of("taskId", taskId), Map.of("content", "Falta o capítulo 2"));
        long commentId = id(comment);
        calls.call("listComments", anaCookie, Map.of("taskId", taskId), null);
        calls.call("listComments", anaCookie, Map.of("taskId", taskId), null, "page=1&size=1");
        calls.call("editComment", anaCookie, Map.of("commentId", commentId), Map.of("content", "Capítulo 2 revisto"));
        calls.call("editComment", martaCookie, Map.of("commentId", commentId), Map.of("content", "Not mine")); // 403
        calls.call("deleteComment", anaCookie, Map.of("commentId", commentId), null);

        // ---- dashboard ----
        calls.call("getDashboard", martaCookie, Map.of(), null);
        calls.call("getDashboard", anaCookie, Map.of(), null);

        // ---- recycle bin ----
        calls.call("deleteTask", anaCookie, Map.of("taskId", taskId), null); // 422: it has an open subtask
        calls.call("deleteTask", anaCookie, Map.of("taskId", subtaskId), null);
        calls.call("listRecycleBin", anaCookie, Map.of(), null);
        calls.call("restoreTask", anaCookie, Map.of("taskId", subtaskId), null);
        calls.call("deleteTask", anaCookie, Map.of("taskId", subtaskId), null);
        calls.call("restoreTask", anaCookie, Map.of("taskId", subtaskId), Map.of("assigneeIds", List.of(ana))); // 400: nobody left
        calls.call("restoreTask", anaCookie, Map.of("taskId", subtaskId), null);
        calls.call("removeMember", martaCookie, Map.of("projectId", project, "userId", userId("diana")), Map.of("reassignments", List.of()));
        calls.call("deleteProject", martaCookie, Map.of("projectId", project), null);
        calls.call("listRecycleBin", martaCookie, Map.of(), null);
        calls.call("restoreProject", anaCookie, Map.of("projectId", project), null); // 403
        calls.call("restoreProject", martaCookie, Map.of("projectId", project), null);
        calls.call("deleteProject", cookieOfPedro(), Map.of("projectId", projectId("Old Site")), null); // 422: completed

        // ---- the three reasons why an item in the bin cannot come back for now (A38) ----
        String inesCookie = cookie(calls.call("signIn", null, Map.of(), Map.of("username", "ines", "password", password)));
        long mobile = projectId("Mobile App");
        long parent = insertTask(mobile, null, "Parent", "To do", "2026-12-31", 3, 3, "ines");
        long child = insertTask(mobile, parent, "Child", "To do", "2026-12-30", 3, 3, "ines");
        clock.set("2026-11-15T10:00:00Z");
        calls.call("deleteTask", inesCookie, Map.of("taskId", child), null);
        clock.set("2026-11-15T11:00:00Z");
        calls.call("deleteTask", inesCookie, Map.of("taskId", parent), null);
        ResponseEntity<String> parentInBin = calls.call("listRecycleBin", inesCookie, Map.of(), null);
        assertThat(parentInBin.getBody()).contains("PARENT_IN_RECYCLE_BIN");
        clock.set("2026-11-15T12:00:00Z");
        calls.call("deleteProject", martaCookie, Map.of("projectId", mobile), null);
        ResponseEntity<String> projectInBin = calls.call("listRecycleBin", inesCookie, Map.of(), null);
        assertThat(projectInBin.getBody()).contains("PROJECT_IN_RECYCLE_BIN");
        long intranet = projectId("Intranet");
        long onlyTask = insertTask(intranet, null, "Gone", "To do", "2026-12-31", 3, 3, "sofia");
        calls.call("deleteTask", sofiaCookie, Map.of("taskId", onlyTask), null);
        jdbc.update("UPDATE project SET status = 'Completed' WHERE id = ?", intranet);
        ResponseEntity<String> completed = calls.call("listRecycleBin", sofiaCookie, Map.of(), null);
        assertThat(completed.getBody()).contains("PROJECT_COMPLETED");
        calls.call("restoreTask", sofiaCookie, Map.of("taskId", onlyTask), null); // 422

        calls.call("signOut", martaCookie, Map.of(), null);
        calls.call("getAuthenticatedUser", martaCookie, Map.of(), null); // 401 after signing out

        assertThat(calls.problems).as("what does not fit the contract").isEmpty();
        assertThat(calls.succeeded).as("the operations that answered with success").containsExactlyInAnyOrderElementsOf(OPERATIONS.keySet());
        assertThat(calls.statuses).as("the error statuses seen").contains(400, 401, 403, 404, 409, 422);
    }

    // ---- helpers ----

    private String cookieOfPedro() {
        return sessionOf("pedro");
    }

    private static String cookie(ResponseEntity<String> login) {
        String setCookie = login.getHeaders().getFirst(HttpHeaders.SET_COOKIE);
        assertThat(setCookie).isNotNull();
        return setCookie.substring(0, setCookie.indexOf(';'));
    }

    private static long id(ResponseEntity<String> response) {
        try {
            return JSON.readTree(response.getBody()).get("id").asLong();
        } catch (Exception e) {
            throw new AssertionError("no id in " + response.getBody(), e);
        }
    }

    /** Makes the requests of the test and keeps what does not fit the contract. */
    private class Calls {

        final List<String> problems = new ArrayList<>();
        final Set<String> succeeded = new TreeSet<>();
        final Set<Integer> statuses = new TreeSet<>();

        ResponseEntity<String> call(String operationId, String cookie, Map<String, Object> pathVariables, Object body) {
            return call(operationId, cookie, pathVariables, body, null);
        }

        ResponseEntity<String> call(String operationId, String cookie, Map<String, Object> pathVariables, Object body, String query) {
            Found found = OPERATIONS.get(operationId);
            String path = found.path();
            for (var variable : pathVariables.entrySet()) {
                path = path.replace("{" + variable.getKey() + "}", String.valueOf(variable.getValue()));
            }
            ResponseEntity<String> response = exchange(query == null ? path : path + "?" + query, found.method(), cookie, body);
            int status = response.getStatusCode().value();
            statuses.add(status);
            check(found, status, response);
            if (status >= 200 && status < 300) {
                succeeded.add(operationId);
            }
            return response;
        }

        private void check(Found found, int status, ResponseEntity<String> response) {
            String where = found.id() + " (" + found.method() + " " + found.path() + ") answered " + status;
            ApiResponse documented = found.operation().getResponses().get(String.valueOf(status));
            if (documented == null) {
                problems.add(where + ", which the contract does not list for it");
                return;
            }
            if (documented.getHeaders() != null) { // the headers the contract lists for this answer, such as X-Total-Count
                documented.getHeaders().keySet().stream().filter(name -> !response.getHeaders().containsKey(name))
                        .forEach(name -> problems.add(where + " without the header " + name + ", which the contract lists"));
            }
            String text = response.getBody();
            var content = documented.getContent() == null ? null : documented.getContent().get("application/json");
            if (content == null) {
                if (text != null && !text.isEmpty()) {
                    problems.add(where + " with a body, and the contract lists none: " + text);
                }
                return;
            }
            try {
                JsonNode json = JSON.readTree(text);
                SchemaChecker.check(json, content.getSchema(), contract.getComponents().getSchemas()).forEach(p -> problems.add(where + ": " + p));
                if (status >= 400) { // NF08: it says why and what to do
                    assertThat(json.path("message").asText()).as(where + ": message").isNotBlank();
                }
            } catch (Exception e) {
                problems.add(where + " with a body that is not JSON: " + text);
            }
        }
    }
}
