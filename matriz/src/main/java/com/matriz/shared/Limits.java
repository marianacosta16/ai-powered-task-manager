package com.matriz.shared;

/**
 * The limits of the text that users type, in characters (R14, P17, A45, A46), in one place: the requests, the
 * parsers of the updates and the messages use them, so a limit is changed once. For the names and the titles the
 * minimum counts the text without the spaces at its ends, so padding does not get round it. The title of a task and the name of
 * a project also may not be only spaces ({@link Texts#isOnlySpaces}). The contract (openapi.yaml) and the database
 * columns say the same numbers and are changed with these; LimitsMatchTheContractAndTheDatabaseTest checks it for the names, titles, descriptions, comments, block reason and search; LABEL_MAX is only a guard of the update parsers, because the contract has no length for the time zone, the phase and the status (the phase and the status are enumerations, and the time zone is checked as a zone).
 */
public final class Limits {

    public static final int PROJECT_NAME_MIN = 3;
    public static final int PROJECT_NAME_MAX = 100;
    public static final int PROJECT_DESCRIPTION_MAX = 1000;
    public static final int TASK_TITLE_MIN = 3;
    public static final int TASK_TITLE_MAX = 150;
    public static final int TASK_DESCRIPTION_MAX = 2000;
    public static final int COMMENT_MAX = 2000;
    public static final int BLOCK_REASON_MAX = 100;
    /** The search text of the projects, and the technical labels sent by the client (time zone, phase and status). */
    public static final int SEARCH_MAX = 100;
    public static final int LABEL_MAX = 100;

    public static final String PROJECT_NAME_MESSAGE = "Must have " + PROJECT_NAME_MIN + " to " + PROJECT_NAME_MAX + " characters.";
    public static final String PROJECT_DESCRIPTION_MESSAGE = "Longer than " + PROJECT_DESCRIPTION_MAX + " characters.";
    public static final String TASK_TITLE_MESSAGE = "Must have " + TASK_TITLE_MIN + " to " + TASK_TITLE_MAX + " characters.";
    public static final String TASK_DESCRIPTION_MESSAGE = "Longer than " + TASK_DESCRIPTION_MAX + " characters.";

    private Limits() {
    }
}
