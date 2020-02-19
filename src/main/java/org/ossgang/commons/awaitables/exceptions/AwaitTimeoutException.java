package org.ossgang.commons.awaitables.exceptions;

public class AwaitTimeoutException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    public AwaitTimeoutException(String message) {
        super(message);
    }
}