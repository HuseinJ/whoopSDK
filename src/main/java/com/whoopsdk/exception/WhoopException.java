package com.whoopsdk.exception;

/** Base type for every failure raised by the SDK. */
public class WhoopException extends RuntimeException {

    public WhoopException(String message) {
        super(message);
    }

    public WhoopException(String message, Throwable cause) {
        super(message, cause);
    }
}
