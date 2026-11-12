package com.matriz.api;

import jakarta.validation.constraints.NotBlank;

record SignInRequest(@NotBlank String username, @NotBlank String password) {

    /** Never print the password. */
    @Override
    public String toString() {
        return "SignInRequest[username=" + username + ", password=***]";
    }
}
