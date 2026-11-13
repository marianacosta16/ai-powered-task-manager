package com.matriz.usecases;

/** Member of the contract: the user plus the open tasks in the project and the red label (P22). */
public record MemberView(long id, String name, String role, int openTaskCount, boolean inOtherOpenProject) {
}
