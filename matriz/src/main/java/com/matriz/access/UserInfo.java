package com.matriz.access;

/** Public data of a user: never includes the password. */
public record UserInfo(long id, String name, String role) {
}
