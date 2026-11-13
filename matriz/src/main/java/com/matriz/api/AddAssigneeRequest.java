package com.matriz.api;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

record AddAssigneeRequest(@NotNull(message = "Is required.") @Min(value = 1, message = "Must be 1 or more.") Long userId) {
}
