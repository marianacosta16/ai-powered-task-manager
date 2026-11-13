package com.matriz.api;

/** Wrong user or password at sign in: the same answer for both (scenario 10.2). */
class InvalidCredentialsException extends RuntimeException {

    InvalidCredentialsException() {
        super("Invalid credentials");
    }
}
