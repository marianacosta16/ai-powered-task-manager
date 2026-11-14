package com.matriz.contract;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;

/**
 * Every scenario and every edge case of the v1 in docs/user-stories.md is named by at least one test, or is
 * explicitly left to the interface with a verification item in the plan (section 8). The tests name them in
 * their comments: a scenario after the word ("scenarios 1.1 to 1.23 and 1.29") or as "// 7.8", an edge case alone ("EC21") or in a range ("EC28 to EC30"). A bare number such as the "3.1" of "OpenAPI 3.1" names nothing. This does not
 * prove that a test checks the scenario well: it stops one from being forgotten when the documents change.
 */
class ScenarioCoverageTest {

    /** Left to the interface: the plan has an item for each one (section 8, "V"), which the test checks. */
    private static final Set<String> FOR_THE_INTERFACE = Set.of("EC39");

    /** A scenario is named after the word "scenario" (or "cenário"), as in "scenarios 1.1 to 1.23 and 1.29", or in a comment "// 7.8". */
    private static final Pattern AFTER_THE_WORD = Pattern.compile(
            "(?i)\\b(?:scenarios?|cenários?)\\b((?:[\\s,]|\\d+\\.\\d+|\\bto\\b|\\band\\b)+)");
    private static final Pattern IN_A_COMMENT = Pattern.compile("//\\s*(\\d+\\.\\d+(?:\\s*,\\s*\\d+\\.\\d+)*)");
    private static final Pattern RANGE = Pattern.compile("(\\d+)\\.(\\d+)\\s+to\\s+(?:\\1\\.)?(\\d+)");
    private static final Pattern SINGLE = Pattern.compile("(\\d+)\\.(\\d+)");
    private static final Pattern EDGE_RANGE = Pattern.compile("EC(\\d+)\\s+(?:to|a)\\s+EC(\\d+)");
    private static final Pattern EDGE = Pattern.compile("EC(\\d+)");

    @Test
    void everyScenarioAndEdgeCaseOfTheFirstVersionIsNamedByATest() throws IOException {
        String stories = Files.readString(Path.of("docs/user-stories.md"));
        Set<String> wanted = new TreeSet<>();
        Matcher scenarios = Pattern.compile("\\*\\*Cenário (\\d+\\.\\d+)").matcher(stories);
        while (scenarios.find()) {
            wanted.add(scenarios.group(1));
        }
        Matcher edges = Pattern.compile("^\\| (EC\\d+) \\| (.*)$", Pattern.MULTILINE).matcher(stories);
        while (edges.find()) {
            if (!edges.group(2).startsWith("(v2)")) { // the edge cases of the second version are not part of the MVP
                wanted.add(edges.group(1));
            }
        }
        assertThat(wanted).as("the scenarios and edge cases found in user-stories.md").hasSizeGreaterThan(150);

        Set<String> named = namedByTests();
        Set<String> missing = new TreeSet<>(wanted);
        missing.removeAll(named);
        missing.removeAll(FOR_THE_INTERFACE);

        assertThat(missing).as("scenarios and edge cases that no test names").isEmpty();
    }

    @Test
    void whatIsLeftToTheInterfaceHasAVerificationItemInThePlan() throws IOException {
        String plan = Files.readString(Path.of("docs/implementation-plan.md"));
        String itemsOfSection8 = plan.lines().filter(line -> line.matches("^\\| V\\d+ \\|.*")).collect(Collectors.joining("\n"));

        for (String id : FOR_THE_INTERFACE) {
            assertThat(itemsOfSection8).as(id + " is left to the interface, so a V item must name it").contains(id);
        }
    }

    private static Set<String> namedByTests() throws IOException {
        StringBuilder text = new StringBuilder();
        try (Stream<Path> files = Files.walk(Path.of("src/test/java"))) {
            for (Path file : files.filter(f -> f.toString().endsWith(".java") && !f.endsWith("ScenarioCoverageTest.java")).toList()) {
                text.append(Files.readString(file)).append('\n');
            }
        }
        Set<String> named = new TreeSet<>();
        for (Pattern place : List.of(AFTER_THE_WORD, IN_A_COMMENT)) {
            Matcher where = place.matcher(text);
            while (where.find()) {
                addScenarios(where.group(1), named);
            }
        }
        Matcher edgeRange = EDGE_RANGE.matcher(text);
        while (edgeRange.find()) {
            for (int i = Integer.parseInt(edgeRange.group(1)); i <= Integer.parseInt(edgeRange.group(2)); i++) {
                named.add("EC" + i);
            }
        }
        Matcher edge = EDGE.matcher(text);
        while (edge.find()) {
            named.add("EC" + edge.group(1));
        }
        return named;
    }

    /** The numbers in a list such as "1.1 to 1.23 and 1.29 to 1.32, 2.4": the ranges are opened, the others taken as they are. */
    private static void addScenarios(String list, Set<String> named) {
        Matcher range = RANGE.matcher(list);
        while (range.find()) {
            int from = Integer.parseInt(range.group(2));
            int to = Integer.parseInt(range.group(3));
            for (int i = from; i <= to && i - from < 40; i++) {
                named.add(range.group(1) + "." + i);
            }
        }
        Matcher single = SINGLE.matcher(list);
        while (single.find()) {
            named.add(single.group(1) + "." + single.group(2));
        }
    }
}
