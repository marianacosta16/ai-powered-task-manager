package com.matriz.shared;

import static org.assertj.core.api.Assertions.assertThat;


import org.junit.jupiter.api.Test;

import com.matriz.shared.Texts.Problem;

/** The one rule for a name or a title (R14, A46), which the validator of the requests and the update parsers share. */
class TextsTest {

    @Test
    void aTextOfTheRightSizeHasNoProblem() {
        assertThat(Texts.check("abc", 3, 150, true)).isEmpty();
        assertThat(Texts.check("a".repeat(150), 3, 150, true)).isEmpty();
        assertThat(Texts.check("  abc  ", 3, 150, true)).isEmpty(); // kept as written
        assertThat(Texts.check(" a b ", 3, 150, true)).isEmpty();
        assertThat(Texts.check("", 0, 100, false)).isEmpty(); // a description may be empty
    }

    @Test
    void theProblemsAreFoundInTheOrderTooLongOnlySpacesTooShort() {
        assertThat(Texts.check("a".repeat(151), 3, 150, true)).contains(Problem.TOO_LONG);
        assertThat(Texts.check(" ".repeat(151), 3, 150, true)).contains(Problem.TOO_LONG); // too long first
        assertThat(Texts.check("   ", 3, 150, true)).contains(Problem.ONLY_SPACES);
        assertThat(Texts.check(" ​﻿", 3, 150, true)).contains(Problem.ONLY_SPACES);
        assertThat(Texts.check("", 3, 150, true)).contains(Problem.TOO_SHORT); // empty is too short, not "only spaces"
        assertThat(Texts.check("ab", 3, 150, true)).contains(Problem.TOO_SHORT);
        assertThat(Texts.check("  a", 3, 150, true)).contains(Problem.TOO_SHORT); // the ends do not count
        assertThat(Texts.check(" ab ", 3, 150, true)).contains(Problem.TOO_SHORT);
    }

    @Test
    void withoutTheVisibleRuleTheSpacesCountAsCharacters() {
        assertThat(Texts.check("   ", 3, 150, false)).isEmpty();
        assertThat(Texts.check("ab", 3, 150, false)).contains(Problem.TOO_SHORT);
    }

    @Test
    void charactersAreCountedNotUtf16Units() {
        String emoji = "👍";

        assertThat(Texts.check(emoji.repeat(150), 3, 150, true)).isEmpty();
        assertThat(Texts.check(emoji.repeat(151), 3, 150, true)).contains(Problem.TOO_LONG);
        assertThat(Texts.visibleLength("  " + emoji.repeat(3) + "​")).isEqualTo(3);
    }

    @Test
    void theMessagesSayWhatIsWrong() {
        assertThat(Texts.message(Problem.TOO_LONG, 3, 150)).isEqualTo("Longer than 150 characters.");
        assertThat(Texts.message(Problem.ONLY_SPACES, 3, 150)).isEqualTo("Must not be only spaces.");
        assertThat(Texts.message(Problem.TOO_SHORT, 3, 150)).isEqualTo("Must have 3 to 150 characters.");
        assertThat(Texts.message(Problem.TOO_SHORT, 1, 100)).isEqualTo("Must have at least 1 character.");
    }
}
