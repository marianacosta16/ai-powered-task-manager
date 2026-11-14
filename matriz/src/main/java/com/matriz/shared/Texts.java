package com.matriz.shared;

import java.util.Set;

/** Small rules about the text that users type. */
public final class Texts {

    /**
     * The invisible characters most often used to make an empty-looking text: the zero-width space, non-joiner and
     * joiner, the word joiner and the byte order mark (zero-width no-break space). They are not white space for Java.
     */
    private static final Set<Integer> INVISIBLE = Set.of(0x200B, 0x200C, 0x200D, 0x2060, 0xFEFF);

    private Texts() {
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

    /**
     * The number of characters of the text without the spaces and the common invisible characters at its two ends:
     * what counts for the minimum of a name or a title, so "  a" does not pass for a text of three characters. The
     * text itself is kept as written; this only measures it.
     */
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
