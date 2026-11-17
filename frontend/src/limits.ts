/**
 * The limits of the texts of a project, as the contract says (R14, A45, A46). The server is the one that decides: they
 * are repeated here only to warn the user before the request is made.
 */
export const PROJECT_NAME_MIN = 3
export const PROJECT_NAME_MAX = 100
export const PROJECT_DESCRIPTION_MAX = 1000

/** The limits of the texts of a task (R14, A46). */
export const TASK_TITLE_MIN = 3
export const TASK_TITLE_MAX = 150
export const TASK_DESCRIPTION_MAX = 2000

/** A block reason (R6, R14) and a comment (R24, NF09). */
export const BLOCK_REASON_MAX = 100
export const COMMENT_MAX = 2000
