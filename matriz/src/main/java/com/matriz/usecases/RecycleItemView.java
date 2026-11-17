package com.matriz.usecases;

import java.time.Instant;

/** RecycleItem of the contract. {@code projectId} is the project of the item; for a project, itself. */
public record RecycleItemView(String itemType, long id, String name, long projectId, String projectName, Instant deletedAt, int daysLeft,
        Integer includedTasks, boolean restorable, String notRestorableReason) {
}
