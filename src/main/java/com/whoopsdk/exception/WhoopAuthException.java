package com.whoopsdk.exception;

/** Thrown when authentication fails: bad credentials, expired refresh token, 401/403 responses. */
public class WhoopAuthException extends WhoopException {

    public WhoopAuthException(String message) {
        super(message);
    }

    public WhoopAuthException(String message, Throwable cause) {
        super(message, cause);
    }
}
