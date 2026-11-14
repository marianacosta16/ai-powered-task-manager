package com.matriz.shared;

import java.util.Optional;
import java.util.Set;

/** Small rules about the text that users type. */
public final class Texts {

    public static final String ONLY_SPACES_MESSAGE = "Must not be only spaces.";

    /** What can be wrong with a text that has a minimum and a maximum length (R14, A46). */
    public enum Problem {
        TOO_LONG, ONLY_SPACES, TOO_SHORT
    }

    /**
     * The invisible characters most often used to make an empty-looking text: the zero-width space, non-joiner and
     * joiner, the word joiner and the byte order mark (zero-width no-break space). They are not white space for Java.
     */
    private static final Set<Integer> INVISIBLE = Set.of(0x200B, 0x200C, 0x200D, 0x2060, 0xFEFF);

    private Texts() {
    }

    /**
     * The first problem of a text, in this order: too long (the text as written, in characters); only spaces, when
     * {@code visible} is true (a name or a title); too short. With {@code visible} the minimum counts the text
     * without the spaces and the common invisible characters at its two ends, so "  a" does not pass for three
     * characters; the text itself is never changed. This is the one place where the rule is written: the validator of
     * the requests and the parsers of the updates call it.
     */
    public static Optional<Problem> check(String text, int min, int max, boolean visible) {
        int length = text.codePointCount(0, text.length());
        if (length > max) {
            return Optional.of(Problem.TOO_LONG);
        }
        if (visible && length > 0 && isOnlySpaces(text)) {
            return Optional.of(Problem.ONLY_SPACES);
        }
        if ((visible ? visibleLength(text) : length) < min) {
            return Optional.of(Problem.TOO_SHORT);
        }
        return Optional.empty();
    }

    /** The message for a problem found by {@link #check}, for the response of the API. */
    public static String message(Problem problem, int min, int max) {
        return switch (problem) {
            case TOO_LONG -> "Longer than " + max + " characters.";
            case ONLY_SPACES -> ONLY_SPACES_MESSAGE;
            case TOO_SHORT -> min == 1 ? "Must have at least 1 character." : "Must have " + min + " to " + max + " characters.";
        };
    }

    /**
     * True when the text has nothing visible in it: empty, or made only of white space of any kind (including the
     * no-break space and the other space characters of Unicode, which {@link String#isBlank()} does not all count)
     * or of the common invisible characters. A name, a title or a comment like that says nothing, so it is refused
     * (A25, A46). Spaces and these characters between visible ones are kept as they were written.
     */
    public static boolean isOnlySpaces(CharSequence text) {
        return text.codePoints().allMatch(Texts::isSpace);
    }

    /** The number of characters of the text without the spaces and the invisible characters at its two ends. */
    public static int visibleLength(String text) {
        int[] codePoints = text.codePoints().toArray();
        int from = 0;
        int to = codePoints.length;
        while (from < to && isSpace(codePoints[from])) {
            from++;
        }
        while (to > from && isSpace(codePoints[to - 1])) {
            to--;
        }
        return to - from;
    }

    private static boolean isSpace(int codePoint) {
        return Character.isWhitespace(codePoint) || Character.isSpaceChar(codePoint) || INVISIBLE.contains(codePoint);
    }
}
