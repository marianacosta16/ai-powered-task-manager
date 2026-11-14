package com.matriz.shared;

/** Small rules about the text that users type. */
public final class Texts {

    private Texts() {
    }

    /**
     * True when the text has nothing but spaces: empty, or made only of white space of any kind, including the
     * no-break space and the other space characters of Unicode, which {@link String#isBlank()} does not all
     * count. A name, a title or a comment of only spaces says nothing, so it is refused (A25, A46).
     */
    public static boolean isOnlySpaces(CharSequence text) {
        return text.codePoints().allMatch(cp -> Character.isWhitespace(cp) || Character.isSpaceChar(cp));
    }
}
