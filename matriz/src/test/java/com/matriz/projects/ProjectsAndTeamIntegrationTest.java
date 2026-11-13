package com.matriz.projects;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import com.jayway.jsonpath.JsonPath;
import com.matriz.testsupport.MutableClock;
import com.matriz.testsupport.TestDatabase;
import com.matriz.usecases.DemoDataSeeder;

/**
 * US01 (scenarios 1.1 to 1.23, 1.29, 1.30) and US08, through real HTTP requests. Increment 2:
 * removing a member who has open tasks needs substitutes and arrives with the tasks (increment 3).
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Import(ProjectsAndTeamIntegrationTest.TestClock.class)
class ProjectsAndTeamIntegrationTest {

    @TestConfiguration
    static class TestClock {
        @Bean
        @Primary
        MutableClock testClock() {
            return new MutableClock();
        }
    }

    @Autowired TestRestTemplate http;
    @Autowired JdbcTemplate jdbc;
    @Autowired DemoDataSeeder seeder;
    @Autowired MutableClock clock;

    private final String password = UUID.randomUUID().toString();

    @BeforeEach
    void freshDemoDataAndDate() {
        TestDatabase.wipe(jdbc);
        seeder.seed(password);
        clock.set("2026-06-01T10:00:00Z");
    }

    // ---- creating projects (1.1, 1.2, 1.9, 1.10, 1.15, 1.19, 1.20, 1.23, M10) ----

    @Test
    void aManagerCreatesAProjectThatShowsInTheirList() {
        ResponseEntity<String> created = post("/projects", sessionOf("marta"), project("Brand new", "2026-06-10", "2026-09-30"));

        assertThat(created.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(json(created, "$.name")).isEqualTo("Brand new");
        assertThat(json(created, "$.phase")).isEqualTo("Initiation");
        assertThat(json(created, "$.status")).isEqualTo("Not started");
        assertThat(json(created, "$.manager.name")).isEqualTo("Marta");
        assertThat(projectNames("marta")).contains("Brand new");
    }

    @Test
    void aTeamMemberCannotCreateProjects() {
        ResponseEntity<String> response = post("/projects", sessionOf("ana"), project("Nope", "2026-06-10", "2026-09-30"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(count("project")).isEqualTo(4);
    }

    @Test
    void theCreatorChoosesTheManagerAndStopsSeeingTheProject() {
        Map<String, Object> body = project("For Pedro", "2026-06-10", "2026-09-30");
        body.put("managerId", userId("pedro"));

        ResponseEntity<String> created = post("/projects", sessionOf("marta"), body);

        assertThat(created.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(json(created, "$.manager.name")).isEqualTo("Pedro");
        assertThat(projectNames("pedro")).contains("For Pedro");
        assertThat(projectNames("marta")).doesNotContain("For Pedro");
    }

    @Test
    void theManagerMustHaveTheRoleProjectManager() {
        Map<String, Object> body = project("Bad manager", "2026-06-10", "2026-09-30");
        body.put("managerId", userId("ana"));

        ResponseEntity<String> response = post("/projects", sessionOf("marta"), body);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
        assertThat(json(response, "$.code")).isEqualTo("MANAGER_NOT_A_MANAGER");
    }

    @Test
    void bothDatesAreRequired() {
        Map<String, Object> body = project("No dates", null, null);
        body.remove("startDate");
        body.remove("endDate");

        ResponseEntity<String> response = post("/projects", sessionOf("marta"), body);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(json(response, "$.code")).isEqualTo("VALIDATION_ERROR");
        assertThat(JsonPath.<List<String>>read(response.getBody(), "$.details[*].field"))
                .contains("startDate", "endDate");
    }

    @Test
    void theEndIsNotBeforeTheStartButASingleDayWithoutTimesIsAccepted() {
        clock.set("2026-01-01T10:00:00Z");
        String marta = sessionOf("marta");

        ResponseEntity<String> backwards = post("/projects", marta, project("Backwards", "2026-01-31", "2026-01-01"));
        ResponseEntity<String> oneDay = post("/projects", marta, project("One day", "2026-01-31", "2026-01-31"));
        Map<String, Object> sameDayEarlier = project("Earlier", "2026-01-31", "2026-01-31");
        sameDayEarlier.put("startTime", "10:00:00");
        sameDayEarlier.put("endTime", "09:00:00");

        assertThat(backwards.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
        assertThat(json(backwards, "$.code")).isEqualTo("END_BEFORE_START");
        assertThat(oneDay.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(post("/projects", marta, sameDayEarlier).getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
    }

    @Test
    void theStartDateIsNotInThePastButTodayIsAccepted() {
        clock.set("2026-01-10T12:00:00Z");
        String marta = sessionOf("marta");

        ResponseEntity<String> yesterday = post("/projects", marta, project("Yesterday", "2026-01-09", "2026-03-01"));
        ResponseEntity<String> today = post("/projects", marta, project("Today", "2026-01-10", "2026-03-01"));

        assertThat(yesterday.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
        assertThat(json(yesterday, "$.code")).isEqualTo("START_DATE_IN_PAST");
        assertThat(today.getStatusCode()).isEqualTo(HttpStatus.CREATED);
    }

    @Test
    void theTimeZoneDefaultsToLisbon() {
        ResponseEntity<String> created = post("/projects", sessionOf("marta"), project("Zone", "2026-06-10", "2026-09-30"));

        assertThat(json(created, "$.timezone")).isEqualTo("Europe/Lisbon");
        Map<String, Object> bad = project("Bad zone", "2026-06-10", "2026-09-30");
        bad.put("timezone", "Mars/Olympus");
        assertThat(post("/projects", sessionOf("marta"), bad).getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void nameAndDescriptionHaveMaximumLengths() {
        String marta = sessionOf("marta");
        Map<String, Object> ok = project("n".repeat(100), "2026-06-10", "2026-09-30");
        ok.put("description", "d".repeat(2000));
        assertThat(post("/projects", marta, ok).getStatusCode()).isEqualTo(HttpStatus.CREATED);

        ResponseEntity<String> longName = post("/projects", marta, project("n".repeat(101), "2026-06-10", "2026-09-30"));
        Map<String, Object> longDescription = project("Long description", "2026-06-10", "2026-09-30");
        longDescription.put("description", "d".repeat(2001));

        assertThat(longName.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(JsonPath.<List<String>>read(longName.getBody(), "$.details[*].field")).contains("name");
        assertThat(post("/projects", marta, longDescription).getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    // ---- updating projects (1.3, 1.18, 1.21, 1.22) ----

    @Test
    void onlyTheManagerChangesPhaseAndStatus() {
        long website = projectId("Website");

        ResponseEntity<String> byMember = patch("/projects/" + website, sessionOf("ana"), Map.of("phase", "Planning"));
        ResponseEntity<String> byOtherManager = patch("/projects/" + website, sessionOf("pedro"), Map.of("phase", "Planning"));
        ResponseEntity<String> byManager = patch("/projects/" + website, sessionOf("marta"),
                Map.of("phase", "Planning", "status", "Suspended"));

        assertThat(byMember.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(byOtherManager.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(byManager.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(json(byManager, "$.phase")).isEqualTo("Planning");
        assertThat(json(byManager, "$.status")).isEqualTo("Suspended");
    }

    @Test
    void attentionAndLateCannotBeSet() {
        long website = projectId("Website");
        String marta = sessionOf("marta");

        for (String calculated : List.of("Attention", "Late")) {
            ResponseEntity<String> response = patch("/projects/" + website, marta, Map.of("status", calculated));
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        }
        assertThat(jdbc.queryForObject("SELECT status FROM project WHERE id = ?", String.class, website))
                .isEqualTo("In progress");
    }

    @Test
    void anUpdateKeepsWhatIsAbsentAndClearsWhatIsNull() {
        long website = projectId("Website");
        Map<String, Object> body = new HashMap<>();
        body.put("name", "Website v2");
        body.put("description", null);

        ResponseEntity<String> response = patch("/projects/" + website, sessionOf("marta"), body);

        assertThat(json(response, "$.name")).isEqualTo("Website v2");
        assertThat(json(response, "$.description")).isNull();
        assertThat(json(response, "$.startDate")).isEqualTo("2026-10-01");
        assertThat(patch("/projects/" + website, sessionOf("marta"), Map.of("colour", "red")).getStatusCode())
                .isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(patch("/projects/" + website, sessionOf("marta"), Map.of()).getStatusCode())
                .isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void theStartDateIsCheckedOnlyWhenItIsChanged() {
        long website = projectId("Website"); // starts 2026-10-01, which is before "today" below
        clock.set("2026-11-12T10:00:00Z");
        String marta = sessionOf("marta");

        ResponseEntity<String> rename = patch("/projects/" + website, marta, Map.of("name", "Renamed"));
        ResponseEntity<String> sameDate = patch("/projects/" + website, marta, Map.of("startDate", "2026-10-01"));
        ResponseEntity<String> pastDate = patch("/projects/" + website, marta, Map.of("startDate", "2026-11-01"));

        assertThat(rename.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(sameDate.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(pastDate.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
        assertThat(json(pastDate, "$.code")).isEqualTo("START_DATE_IN_PAST");
    }

    @Test
    void aCompletedProjectIsFrozen() {
        // Old Site (Pedro) is completed, with Diana as member
        long oldSite = projectId("Old Site");
        String pedro = sessionOf("pedro");
        long ana = userId("ana");

        ResponseEntity<String> rename = patch("/projects/" + oldSite, pedro, Map.of("name", "Reopened"));
        ResponseEntity<String> reopen = patch("/projects/" + oldSite, pedro, Map.of("status", "In progress"));
        ResponseEntity<String> add = post("/projects/" + oldSite + "/members", pedro, Map.of("userId", ana));
        ResponseEntity<String> remove = post("/projects/" + oldSite + "/members/" + userId("diana") + "/removal", pedro,
                Map.of("reassignments", List.of()));

        for (ResponseEntity<String> r : List.of(rename, reopen, add, remove)) {
            assertThat(r.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
            assertThat(json(r, "$.code")).isEqualTo("PROJECT_COMPLETED");
        }
        // everything is still there, and still visible
        assertThat(json(get("/projects/" + oldSite, pedro), "$.name")).isEqualTo("Old Site");
        assertThat(json(get("/projects/" + oldSite, pedro), "$.status")).isEqualTo("Completed");
        assertThat(memberNames(oldSite, pedro)).containsExactly("Diana");
    }

    // ---- the team (1.4 to 1.8, 1.11, 1.16, 1.17, 1.29, 1.30, R11, R17) ----

    @Test
    void theManagerAddsAMemberWhoThenSeesTheProject() {
        // Diana's only project, Old Site, is completed, so she is free (P20)
        long website = projectId("Website");

        ResponseEntity<String> added = post("/projects/" + website + "/members", sessionOf("marta"),
                Map.of("userId", userId("diana")));

        assertThat(added.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(json(added, "$.name")).isEqualTo("Diana");
        assertThat(json(added, "$.openTaskCount")).isEqualTo(0);
        assertThat(projectNames("diana")).contains("Website");
    }

    @Test
    void aMemberAlreadyInAnotherOpenProjectIsRefused() {
        long intranet = projectId("Intranet"); // Ana is in Website, which is open
        int before = memberNames(intranet, sessionOf("pedro")).size();

        ResponseEntity<String> response = post("/projects/" + intranet + "/members", sessionOf("pedro"),
                Map.of("userId", userId("ana")));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
        assertThat(json(response, "$.code")).isEqualTo("MEMBER_IN_OTHER_OPEN_PROJECT");
        assertThat(memberNames(intranet, sessionOf("pedro"))).hasSize(before);
    }

    @Test
    void someoneAlreadyInTheProjectCannotBeAddedAgain() {
        long website = projectId("Website");
        String marta = sessionOf("marta");

        ResponseEntity<String> member = post("/projects/" + website + "/members", marta, Map.of("userId", userId("ana")));
        ResponseEntity<String> manager = post("/projects/" + website + "/members", marta, Map.of("userId", userId("marta")));

        for (ResponseEntity<String> r : List.of(member, manager)) {
            assertThat(r.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
            assertThat(json(r, "$.code")).isEqualTo("MEMBER_ALREADY_IN_PROJECT");
        }
        assertThat(memberNames(website, marta)).containsExactly("Ana", "Rui", "Luís");
    }

    @Test
    void onlyTheManagerAddsMembers() {
        long website = projectId("Website");

        ResponseEntity<String> response = post("/projects/" + website + "/members", sessionOf("rui"),
                Map.of("userId", userId("diana")));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(memberNames(website, sessionOf("marta"))).doesNotContain("Diana");
    }

    @Test
    void aSuspendedProjectFreesItsMembersAndResumingItShowsTheRedLabel() {
        long website = projectId("Website");
        long intranet = projectId("Intranet");
        String marta = sessionOf("marta");
        String pedro = sessionOf("pedro");

        // 1.7: Website is suspended, so Ana can be added to Intranet
        patch("/projects/" + website, marta, Map.of("status", "Suspended"));
        ResponseEntity<String> moved = post("/projects/" + intranet + "/members", pedro, Map.of("userId", userId("ana")));
        assertThat(moved.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(memberFlag(website, marta, "Ana")).isFalse(); // not resumed yet

        // 1.8: Website is resumed; Ana stays in the list, with the red label
        patch("/projects/" + website, marta, Map.of("status", "In progress"));
        assertThat(memberNames(website, marta)).contains("Ana");
        assertThat(memberFlag(website, marta, "Ana")).isTrue();

        // 1.16: when Intranet is completed, the label goes away
        patch("/projects/" + intranet, pedro, Map.of("status", "Completed"));
        assertThat(memberFlag(website, marta, "Ana")).isFalse();
    }

    @Test
    void aMemberCanBeAddedToASuspendedProjectEvenIfHeIsInAnotherOpenOne() {
        long website = projectId("Website");
        String marta = sessionOf("marta");
        patch("/projects/" + website, marta, Map.of("status", "Suspended"));

        // Sofia is in Intranet, which is open
        ResponseEntity<String> added = post("/projects/" + website + "/members", marta, Map.of("userId", userId("sofia")));
        patch("/projects/" + website, marta, Map.of("status", "In progress"));

        assertThat(added.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(memberFlag(website, marta, "Sofia")).isTrue();
    }

    @Test
    void theOneOpenProjectRuleDoesNotApplyToManagers() {
        // Pedro manages Intranet (open) and is added as a member of Marta's Website (1.17)
        ResponseEntity<String> added = post("/projects/" + projectId("Website") + "/members", sessionOf("marta"),
                Map.of("userId", userId("pedro")));

        assertThat(added.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(json(added, "$.role")).isEqualTo("Project manager");
        assertThat(projectNames("pedro")).contains("Website", "Intranet");
    }

    @Test
    void aMemberWithoutOpenTasksIsRemovedAndFreed_evenFromASuspendedProject() {
        long website = projectId("Website");
        String marta = sessionOf("marta");
        post("/projects/" + website + "/members", marta, Map.of("userId", userId("diana")));
        patch("/projects/" + website, marta, Map.of("status", "Suspended")); // 1.30

        ResponseEntity<String> removed = post("/projects/" + website + "/members/" + userId("diana") + "/removal", marta,
                Map.of("reassignments", List.of()));

        assertThat(removed.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        assertThat(memberNames(website, marta)).doesNotContain("Diana");
        assertThat(get("/projects/" + website, sessionOf("diana")).getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void aMemberWithOpenTasksCannotBeRemovedUntilSubstitutesExist() {
        long website = projectId("Website");

        ResponseEntity<String> response = post("/projects/" + website + "/members/" + userId("ana") + "/removal",
                sessionOf("marta"), Map.of("reassignments", List.of()));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
        assertThat(json(response, "$.code")).isEqualTo("REPLACEMENT_REQUIRED");
        assertThat(memberNames(website, sessionOf("marta"))).contains("Ana");
    }

    @Test
    void onlyTheManagerRemovesMembers() {
        ResponseEntity<String> response = post("/projects/" + projectId("Website") + "/members/" + userId("rui") + "/removal",
                sessionOf("luis"), Map.of("reassignments", List.of()));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void membersAreListedWithTheirOpenTasksAndWithoutTheManager() {
        long website = projectId("Website");

        ResponseEntity<String> asManager = get("/projects/" + website + "/members", sessionOf("marta"));
        ResponseEntity<String> asMember = get("/projects/" + website + "/members", sessionOf("ana"));

        assertThat(asManager.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(JsonPath.<List<String>>read(asManager.getBody(), "$[*].name")).containsExactly("Ana", "Rui", "Luís");
        // Ana: 1 open task (her done subtask does not count); Rui and Luís: 2 each (a duplicate is final)
        assertThat(JsonPath.<List<Integer>>read(asManager.getBody(), "$[*].openTaskCount")).containsExactly(1, 2, 2);
        assertThat(asMember.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(get("/projects/" + website + "/members", sessionOf("sofia")).getStatusCode())
                .isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void candidatesShowWhoIsAvailable() {
        long website = projectId("Website");

        ResponseEntity<String> response = get("/projects/" + website + "/candidate-members", sessionOf("marta"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        List<String> names = JsonPath.read(response.getBody(), "$[*].user.name");
        assertThat(names).doesNotContain("Marta", "Ana", "Rui", "Luís"); // the manager and the team are not candidates
        assertThat(names).contains("Diana", "Pedro", "Sofia");
        assertThat(JsonPath.<List<Boolean>>read(response.getBody(), "$[?(@.user.name=='Sofia')].available"))
                .containsExactly(false); // in Intranet, which is open
        assertThat(JsonPath.<List<String>>read(response.getBody(), "$[?(@.user.name=='Sofia')].unavailableReason"))
                .containsExactly("IN_OTHER_OPEN_PROJECT");
        assertThat(JsonPath.<List<Boolean>>read(response.getBody(), "$[?(@.user.name=='Diana')].available"))
                .containsExactly(true);
        assertThat(JsonPath.<List<Boolean>>read(response.getBody(), "$[?(@.user.name=='Pedro')].available"))
                .containsExactly(true); // managers are exempt (R11)
        assertThat(get("/projects/" + website + "/candidate-members", sessionOf("ana")).getStatusCode())
                .isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void theNewOperationsNeedASession() {
        long website = projectId("Website");
        assertThat(post("/projects", null, project("X", "2026-06-10", "2026-09-30")).getStatusCode())
                .isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(patch("/projects/" + website, null, Map.of("name", "X")).getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(get("/projects/" + website + "/members", null).getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(get("/projects/" + website + "/candidate-members", null).getStatusCode())
                .isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void missingProjectsAndPeopleAreNotFound() {
        String marta = sessionOf("marta");
        assertThat(get("/projects/999999/members", marta).getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(patch("/projects/999999", marta, Map.of("name", "X")).getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(post("/projects/" + projectId("Website") + "/members", marta, Map.of("userId", 999999)).getStatusCode())
                .isEqualTo(HttpStatus.NOT_FOUND);
    }

    // ---- helpers ----

    /** A valid body to create a project; the manager is Marta unless the test changes it. */
    private Map<String, Object> project(String name, String start, String end) {
        Map<String, Object> body = new HashMap<>();
        body.put("name", name);
        body.put("startDate", start);
        body.put("endDate", end);
        body.put("phase", "Initiation");
        body.put("status", "Not started");
        body.put("managerId", userId("marta"));
        return body;
    }

    private ResponseEntity<String> signIn(String username) {
        return exchange("/auth/login", HttpMethod.POST, null, Map.of("username", username, "password", password));
    }

    private String sessionOf(String username) {
        String setCookie = signIn(username).getHeaders().getFirst(HttpHeaders.SET_COOKIE);
        assertThat(setCookie).as("session cookie of " + username).isNotNull();
        return setCookie.substring(0, setCookie.indexOf(';'));
    }

    private ResponseEntity<String> get(String path, String cookie) {
        return exchange(path, HttpMethod.GET, cookie, null);
    }

    private ResponseEntity<String> post(String path, String cookie, Object body) {
        return exchange(path, HttpMethod.POST, cookie, body);
    }

    private ResponseEntity<String> patch(String path, String cookie, Object body) {
        return exchange(path, HttpMethod.PATCH, cookie, body);
    }

    private ResponseEntity<String> exchange(String path, HttpMethod method, String cookie, Object body) {
        HttpHeaders headers = new HttpHeaders();
        if (cookie != null) {
            headers.add(HttpHeaders.COOKIE, cookie);
        }
        if (body != null) {
            headers.setContentType(MediaType.APPLICATION_JSON);
        }
        return http.exchange(path, method, new HttpEntity<>(body, headers), String.class);
    }

    private List<String> projectNames(String username) {
        return JsonPath.read(get("/projects", sessionOf(username)).getBody(), "$[*].name");
    }

    private List<String> memberNames(long projectId, String cookie) {
        return JsonPath.read(get("/projects/" + projectId + "/members", cookie).getBody(), "$[*].name");
    }

    private boolean memberFlag(long projectId, String cookie, String name) {
        List<Boolean> flags = JsonPath.read(get("/projects/" + projectId + "/members", cookie).getBody(),
                "$[?(@.name=='" + name + "')].inOtherOpenProject");
        return flags.get(0);
    }

    private long projectId(String name) {
        return jdbc.queryForObject("SELECT id FROM project WHERE name = ?", Long.class, name);
    }

    private long userId(String username) {
        return jdbc.queryForObject("SELECT id FROM app_user WHERE username = ?", Long.class, username);
    }

    private int count(String table) {
        return jdbc.queryForObject("SELECT count(*) FROM " + table, Integer.class);
    }

    private static Object json(ResponseEntity<String> response, String path) {
        return JsonPath.read(response.getBody(), path);
    }
}
