package com.matriz.shared;

/**
 * Which page of a list is asked for: {@code page} starts at 1 and {@code size} is 1 to {@value #MAX_SIZE}
 * (A30). The values come from the query string as text, so a bad one is a 400 that the use case raises
 * after the access check (403 before 400).
 */
public record PageRequest(int page, int size) {

    public static final int DEFAULT_SIZE = 50;
    public static final int MAX_SIZE = 200;

    /** Reads {@code page} and {@code size}; a null is the default (first page, 50 items). */
    public static PageRequest parse(String page, String size) {
        int parsedPage = number("page", page, 1, 1, Integer.MAX_VALUE, "Must be a whole number from 1.");
        int parsedSize = number("size", size, DEFAULT_SIZE, 1, MAX_SIZE, "Must be a whole number from 1 to " + MAX_SIZE + ".");
        return new PageRequest(parsedPage, parsedSize);
    }

    /** How many items come before this page. */
    public long offset() {
        return (long) (page - 1) * size;
    }

    private static int number(String field, String text, int fallback, int min, int max, String issue) {
        if (text == null) {
            return fallback;
        }
        try {
            int value = Integer.parseInt(text);
            if (value >= min && value <= max) {
                return value;
            }
        } catch (NumberFormatException e) {
            // falls through to the same answer as a number out of range
        }
        throw new InvalidRequestException("The page is not valid.", field, issue);
    }
}
