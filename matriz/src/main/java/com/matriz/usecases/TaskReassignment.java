package com.matriz.usecases;

/** One open task of a member who is being removed, and the person who takes it over (R17, P24). */
public record TaskReassignment(long taskId, long newAssigneeId) {
}
