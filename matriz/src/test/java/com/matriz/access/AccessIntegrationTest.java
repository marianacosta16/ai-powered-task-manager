package com.matriz.access;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.web.ServerProperties;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import com.jayway.jsonpath.JsonPath;
import com.matriz.testsupport.TestDatabase;
import com.matriz.usecases.DemoDataSeeder;

/**
 * US10 (scenarios 10.1 to 10.6, EC31, EC38) and the project access rules (R8, R19, S1),
 * through real HTTP requests, against the test database. Passwords are generated per run.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class AccessIntegrationTest {

    @Autowired TestRestTemplate http;
    @Autowired JdbcTemplate jdbc;
    @Autowired DemoDataSeeder seeder;
    @Autowired ServerProperties server;

    private final String password = UUID.randomUUID().toString();

    @BeforeEach
    void freshDemoData() {
        TestDatabase.wipe(jdbc);
        seeder.seed(password);
    }

    // ---- sign in (10.1, 10.2) ----

    @Test
    void validCredentialsStartASessionWithTheRoleOfTheUser() {
        // Given Ana's demo account (a team member)
        // When she signs in with the correct credentials
        ResponseEntity<String> response = signIn("ana", password);

        // Then she is in, sees her name and role, and gets a protected session cookie
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(json(response, "$.name")).isEqualTo("Ana");
        assertThat(json(response, "$.role")).isEqualTo("Team member");
        String cookie = response.getHeaders().getFirst(HttpHeaders.SET_COOKIE);
        assertThat(cookie).startsWith("SESSION=").contains("HttpOnly").contains("SameSite=Lax");
        // And nothing about the password is returned
        assertThat(response.getBody()).doesNotContain(password).doesNotContain("$2a$").doesNotContainIgnoringCase("password");
    }

    @Test
    void wrongPasswordAndUnknownUserGetTheSameAnswer() {
        // Given a wrong password for a real user, and a user that does not exist
        ResponseEntity<String> wrongPassword = signIn("ana", "wrong-password-1");
        ResponseEntity<String> unknownUser = signIn("nobody", "wrong-password-1");

        // Then both are 401 with the same code and message, and no session is started
        assertThat(wrongPassword.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(unknownUser.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(json(wrongPassword, "$.code")).isEqualTo("INVALID_CREDENTIALS");
        assertThat(wrongPassword.getBody()).isEqualTo(unknownUser.getBody());
        assertThat(wrongPassword.getHeaders().getFirst(HttpHeaders.SET_COOKIE)).isNull();
        assertThat(unknownUser.getHeaders().getFirst(HttpHeaders.SET_COOKIE)).isNull();
    }

    @Test
    void missingFieldsAreRejectedBeforeAnyCheck() {
        ResponseEntity<String> response = post("/auth/login", null, Map.of("username", "ana"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(json(response, "$.code")).isEqualTo("VALIDATION_ERROR");
    }

    @Test
    void signingInAlwaysIssuesANewSessionIdentifier() {
        // Given a client that already holds some session cookie
        // When it signs in
        ResponseEntity<String> response = post("/auth/login", "SESSION=forged-session-id",
                Map.of("username", "ana", "password", password));

        // Then the server issues its own identifier and does not adopt the one sent (session fixation)
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getHeaders().getFirst(HttpHeaders.SET_COOKIE)).doesNotContain("forged-session-id");
    }

    // ---- profile, sign out, no session (10.3 to 10.6, EC31, EC38) ----

    @Test
    void theAuthenticatedUserIsReturnedForTheSession() {
        String ines = sessionOf("ines");

        ResponseEntity<String> me = get("/auth/me", ines);

        assertThat(me.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(json(me, "$.name")).isEqualTo("Inês");
        assertThat(json(me, "$.role")).isEqualTo("Team member");
        assertThat(json(get("/auth/me", sessionOf("marta")), "$.role")).isEqualTo("Project manager");
    }

    @Test
    void requestsWithoutASessionAreAskedToSignIn() {
        // Given nobody signed in, when a direct link to a project is opened
        ResponseEntity<String> project = get("/projects/" + projectId("Website"), null);
        ResponseEntity<String> me = get("/auth/me", null);
        ResponseEntity<String> projects = get("/projects", null);

        // Then it is 401, and the project is not shown
        for (ResponseEntity<String> r : List.of(project, me, projects)) {
            assertThat(r.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
            assertThat(json(r, "$.code")).isEqualTo("UNAUTHENTICATED");
            assertThat(json(r, "$.message")).isEqualTo("Sign in to continue.");
            assertThat(r.getBody()).doesNotContain("Website");
        }
        assertThat(post("/auth/logout", null, null).getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void afterSigningOutTheProjectsCannotBeOpenedAnymore() {
        // Given Ana signed in and can open her project
        String ana = sessionOf("ana");
        long website = projectId("Website");
        assertThat(get("/projects/" + website, ana).getStatusCode()).isEqualTo(HttpStatus.OK);

        // When she signs out
        ResponseEntity<String> out = post("/auth/logout", ana, null);

        // Then it is 204, the cookie is dropped, and the old session no longer opens projects
        assertThat(out.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        assertThat(out.getHeaders().getFirst(HttpHeaders.SET_COOKIE)).startsWith("SESSION=;").contains("Max-Age=0");
        ResponseEntity<String> again = get("/projects/" + website, ana);
        assertThat(again.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(json(again, "$.message")).isEqualTo("Your session has ended. Sign in again to continue.");
    }

    @Test
    void sessionLastsThreeHoursWithoutActivity() {
        // The server forgets the session after this idle time (S5, EC38); an ended session behaves
        // as in the previous test: 401 with the "session has ended" message.
        assertThat(server.getServlet().getSession().getTimeout()).hasHours(3);
    }

    // ---- access by project, with two users (R8, R19, S1) ----

    @Test
    void eachUserListsOnlyTheirOwnProjects() {
        assertThat(projectNames("ana")).containsExactly("Website");
        assertThat(projectNames("ines")).containsExactly("Mobile App");
        assertThat(projectNames("marta")).containsExactlyInAnyOrder("Website", "Mobile App");
        assertThat(projectNames("pedro")).containsExactlyInAnyOrder("Intranet", "Old Site");
        assertThat(projectNames("diana")).containsExactly("Old Site");
    }

    @Test
    void aMemberOpensTheirProjectButNotSomeoneElses() {
        long website = projectId("Website");
        long mobile = projectId("Mobile App");
        String ana = sessionOf("ana");
        String ines = sessionOf("ines");

        // Ana (Website member) opens Website, but a direct link to Mobile App is refused
        ResponseEntity<String> own = get("/projects/" + website, ana);
        assertThat(own.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(json(own, "$.name")).isEqualTo("Website");
        assertThat(json(own, "$.manager.name")).isEqualTo("Marta");
        ResponseEntity<String> other = get("/projects/" + mobile, ana);
        assertThat(other.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(json(other, "$.code")).isEqualTo("FORBIDDEN");
        assertThat(other.getBody()).doesNotContain("Mobile App");

        // And the other way round for Inês
        assertThat(get("/projects/" + mobile, ines).getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(get("/projects/" + website, ines).getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void aManagerSeesOnlyTheProjectsTheyManage() {
        // Marta manages Website and Mobile App; Intranet belongs to Pedro (R19)
        String marta = sessionOf("marta");

        assertThat(get("/projects/" + projectId("Website"), marta).getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(get("/projects/" + projectId("Intranet"), marta).getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void identityComesFromTheSessionNotFromTheRequest() {
        // Given Ana's session, a client that claims to be someone else in the request
        String ana = sessionOf("ana");
        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.COOKIE, ana);
        headers.add("X-User-Id", String.valueOf(userId("marta")));

        // When it lists projects
        ResponseEntity<String> response = http.exchange("/projects?userId=" + userId("marta") + "&managerId="
                + userId("marta"), HttpMethod.GET, new HttpEntity<>(headers), String.class);

        // Then the answer is still Ana's
        assertThat(JsonPath.<List<String>>read(response.getBody(), "$[*].name")).containsExactly("Website");
    }

    @Test
    void missingAndDeletedProjectsAreNotFound() {
        String ana = sessionOf("ana");
        assertThat(get("/projects/999999", ana).getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(get("/projects/abc", ana).getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);

        jdbc.update("UPDATE project SET deleted_at = now() WHERE name = 'Website'");
        assertThat(get("/projects/" + projectId("Website"), ana).getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(projectNames("ana")).isEmpty();
    }

    @Test
    void searchFindsProjectsByPartOfTheNameIgnoringCase() {
        String marta = sessionOf("marta");

        ResponseEntity<String> found = search("SITE", marta);
        ResponseEntity<String> none = search("xyz", marta);
        ResponseEntity<String> wildcard = search("%", marta);

        assertThat(JsonPath.<List<String>>read(found.getBody(), "$[*].name")).containsExactly("Website");
        assertThat(none.getBody()).isEqualTo("[]");
        assertThat(wildcard.getBody()).isEqualTo("[]"); // "%" is text, not a wildcard
    }

    @Test
    void searchTreatsSpacesAsText() {
        // Given Marta's projects Website and Mobile App, when she searches for a single space
        ResponseEntity<String> found = search(" ", sessionOf("marta"));

        // Then only the name with a space is returned, not every project
        assertThat(JsonPath.<List<String>>read(found.getBody(), "$[*].name")).containsExactly("Mobile App");
    }

    @Test
    void protocolErrorsKeepTheContractErrorBody() {
        String ana = sessionOf("ana");

        // A form instead of JSON
        HttpHeaders form = new HttpHeaders();
        form.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        ResponseEntity<String> wrongType = http.exchange("/auth/login", HttpMethod.POST,
                new HttpEntity<>("username=ana&password=x", form), String.class);
        assertThat(wrongType.getStatusCode()).isEqualTo(HttpStatus.UNSUPPORTED_MEDIA_TYPE);
        assertThat(json(wrongType, "$.code")).isEqualTo("VALIDATION_ERROR");
        assertThat(wrongType.getBody()).doesNotContain("timestamp");

        // A method the path does not accept, and a path that does not exist
        ResponseEntity<String> wrongMethod = http.exchange("/projects/1", HttpMethod.PUT,
                new HttpEntity<>(headers(ana)), String.class);
        assertThat(wrongMethod.getStatusCode()).isEqualTo(HttpStatus.METHOD_NOT_ALLOWED);
        assertThat(wrongMethod.getHeaders().getAllow()).contains(HttpMethod.GET);
        assertThat(json(wrongMethod, "$.code")).isEqualTo("VALIDATION_ERROR");
        ResponseEntity<String> unknown = get("/no-such-path", ana);
        assertThat(unknown.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(json(unknown, "$.code")).isEqualTo("NOT_FOUND");
    }

    @Test
    void healthCheckStaysPublic() {
        assertThat(get("/actuator/health", null).getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    // ---- helpers ----

    private ResponseEntity<String> signIn(String username, String pass) {
        return post("/auth/login", null, Map.of("username", username, "password", pass));
    }

    /** Signs in and returns the cookie to send in the next requests. */
    private String sessionOf(String username) {
        String setCookie = signIn(username, password).getHeaders().getFirst(HttpHeaders.SET_COOKIE);
        assertThat(setCookie).as("session cookie of " + username).isNotNull();
        return setCookie.substring(0, setCookie.indexOf(';'));
    }

    private ResponseEntity<String> get(String path, String cookie) {
        return http.exchange(path, HttpMethod.GET, new HttpEntity<>(headers(cookie)), String.class);
    }

    /** The text goes as a URI variable, so the client encodes it once (a literal "%20" would be encoded twice). */
    private ResponseEntity<String> search(String text, String cookie) {
        return http.exchange("/projects?search={text}", HttpMethod.GET, new HttpEntity<>(headers(cookie)),
                String.class, text);
    }

    private ResponseEntity<String> post(String path, String cookie, Object body) {
        HttpHeaders headers = headers(cookie);
        headers.setContentType(MediaType.APPLICATION_JSON);
        return http.exchange(path, HttpMethod.POST, new HttpEntity<>(body, headers), String.class);
    }

    private static HttpHeaders headers(String cookie) {
        HttpHeaders headers = new HttpHeaders();
        if (cookie != null) {
            headers.add(HttpHeaders.COOKIE, cookie);
        }
        return headers;
    }

    private List<String> projectNames(String username) {
        return JsonPath.read(get("/projects", sessionOf(username)).getBody(), "$[*].name");
    }

    private long projectId(String name) {
        return jdbc.queryForObject("SELECT id FROM project WHERE name = ?", Long.class, name);
    }

    private long userId(String username) {
        return jdbc.queryForObject("SELECT id FROM app_user WHERE username = ?", Long.class, username);
    }

    private static Object json(ResponseEntity<String> response, String path) {
        return JsonPath.read(response.getBody(), path);
    }
}
