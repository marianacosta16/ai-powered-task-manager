package com.matriz.recycle;

import java.time.Instant;

/** A project in the recycle bin; {@code includedTasks} is the number of main tasks deleted with it (R9). */
public record DeletedProject(long id, String name, long managerId, Instant deletedAt, int includedTasks) {
}
