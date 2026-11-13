package com.matriz.api;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/** Body of removeMember: one reassignment for each open task of the member (empty when there are none). */
record MemberRemovalRequest(@NotNull(message = "Is required.") List<@Valid Reassignment> reassignments) {

    record Reassignment(
            @NotNull(message = "Is required.") @Min(value = 1, message = "Must be 1 or more.") Long taskId,
            @NotNull(message = "Is required.") @Min(value = 1, message = "Must be 1 or more.") Long newAssigneeId) {
    }
}
