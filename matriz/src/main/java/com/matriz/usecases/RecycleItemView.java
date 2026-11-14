package com.matriz.usecases;

import java.time.Instant;

/** RecycleItem of the contract. */
public record RecycleItemView(String itemType, long id, String name, String projectName, Instant deletedAt, int daysLeft,
        Integer includedTasks, boolean restorable, String notRestorableReason) {
}
