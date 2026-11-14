package com.matriz.contract;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
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
 * their comments, alone ("// 7.8", "EC21") or in a range ("scenarios 1.1 to 1.23", "EC28 to EC30"). This does not
 * prove that a test checks the scenario well: it stops one from being forgotten when the documents change.
 */
class ScenarioCoverageTest {

    /** Left to the interface: the plan has an item for each one (section 8, "V"), which the test checks. */
    private static final Set<String> FOR_THE_INTERFACE = Set.of("EC39");

    private static final Pattern SCENARIO_RANGE = Pattern.compile("(\\d+)\\.(\\d+)\\s+(?:to|a)\\s+(?:\\1\\.)?(\\d+)");
    private static final Pattern SCENARIO = Pattern.compile("(?<![\\w.])(\\d+)\\.(\\d+)(?![\\d.]*\\w)");
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
        Matcher range = SCENARIO_RANGE.matcher(text);
        while (range.find()) {
            int from = Integer.parseInt(range.group(2));
            int to = Integer.parseInt(range.group(3));
            for (int i = from; i <= to && i - from < 40; i++) {
                named.add(range.group(1) + "." + i);
            }
        }
        Matcher single = SCENARIO.matcher(text);
        while (single.find()) {
            named.add(single.group(1) + "." + single.group(2));
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
}
