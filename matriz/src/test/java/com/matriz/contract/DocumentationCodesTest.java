package com.matriz.contract;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;

/**
 * The codes of the documents (AGENTS, section 9): each family is numbered 1, 2, 3... without a repeated or a
 * missing number, and every reference to a code in the documents, the contract and the code points to one that
 * exists. A code is never reused and never renumbered, so this stays true as the documents grow.
 */
class DocumentationCodesTest {

    /** A family of codes: where each code is defined, the first number, and what a reference looks like. */
    private record Family(String name, String file, Pattern definition, int first) {
    }

    private static final List<Family> FAMILIES = List.of(
            new Family("P (assumptions)", "docs/constraints.md", Pattern.compile("^\\| P(\\d+) \\|", Pattern.MULTILINE), 1),
            new Family("T (technical decisions)", "docs/constraints.md", Pattern.compile("^\\| T(\\d+) \\|", Pattern.MULTILINE), 1),
            new Family("R (business rules)", "docs/requirements.md", Pattern.compile("^\\| R(\\d+) \\|", Pattern.MULTILINE), 1),
            new Family("EC (edge cases)", "docs/user-stories.md", Pattern.compile("^\\| EC(\\d+) \\|", Pattern.MULTILINE), 1),
            new Family("M (model decisions)", "docs/database.md", Pattern.compile("^\\| M(\\d+) \\|", Pattern.MULTILINE), 1),
            new Family("V (checks for the interface)", "docs/implementation-plan.md", Pattern.compile("^\\| V(\\d+) \\|", Pattern.MULTILINE), 1),
            // the assumptions of the contract; A1 and A2 belong to other documents that have their own A lists
            new Family("A (assumptions of the contract)", "openapi.yaml", Pattern.compile("^    - A(\\d+) \\(", Pattern.MULTILINE), 3));

    @Test
    void eachFamilyIsNumberedWithoutRepeatsOrGaps() throws IOException {
        for (Family family : FAMILIES) {
            List<Integer> numbers = definitions(family);
            assertThat(numbers).as(family.name() + " is not empty").isNotEmpty();
            assertThat(new TreeSet<>(numbers)).as(family.name() + ": no code is defined twice").hasSameSizeAs(numbers);
            int last = numbers.stream().mapToInt(Integer::intValue).max().orElseThrow();
            List<Integer> expected = new ArrayList<>();
            for (int i = family.first(); i <= last; i++) {
                expected.add(i);
            }
            assertThat(new TreeSet<>(numbers)).as(family.name() + ": no number is missing").containsExactlyElementsOf(expected);
        }
    }

    @Test
    void everyReferenceToACodePointsToOneThatExists() throws IOException {
        Map<String, Integer> highest = new TreeMap<>();
        for (Family family : FAMILIES) {
            highest.put(family.name().split(" ")[0], definitions(family).stream().mapToInt(Integer::intValue).max().orElseThrow());
        }
        Pattern reference = Pattern.compile("(?<![\\w-])(P|T|R|EC|M|V|A)(\\d+)(?![\\w])");
        List<String> dangling = new ArrayList<>();
        for (Path file : filesToRead()) {
            Matcher m = reference.matcher(Files.readString(file));
            while (m.find()) {
                String family = m.group(1);
                int number = Integer.parseInt(m.group(2));
                // A1 and A2 (and the A codes of user-stories.md and review.md) are other lists: only the ones above the
                // contract's own range could be mistakes
                boolean otherList = family.equals("A") && (number < 3 || file.toString().endsWith("user-stories.md")
                        || file.toString().endsWith("review.md"));
                if (!otherList && (number < 1 || number > highest.get(family))) {
                    dangling.add(file + ": " + family + number);
                }
            }
        }
        assertThat(dangling).as("references to a code that does not exist").isEmpty();
    }

    private static List<Integer> definitions(Family family) throws IOException {
        Matcher m = family.definition().matcher(Files.readString(Path.of(family.file())));
        List<Integer> numbers = new ArrayList<>();
        while (m.find()) {
            numbers.add(Integer.parseInt(m.group(1)));
        }
        return numbers;
    }

    /** The documents, the contract and the sources: where the codes are written. */
    private static List<Path> filesToRead() throws IOException {
        List<Path> files = new ArrayList<>(List.of(Path.of("README.md"), Path.of("AGENTS.md"), Path.of("openapi.yaml")));
        for (String directory : List.of("docs", "src/main/java", "src/test/java")) {
            try (Stream<Path> walk = Files.walk(Path.of(directory))) {
                walk.filter(f -> f.toString().endsWith(".md") || f.toString().endsWith(".java")).forEach(files::add);
            }
        }
        files.removeIf(f -> f.endsWith("DocumentationCodesTest.java"));
        return files;
    }
}
