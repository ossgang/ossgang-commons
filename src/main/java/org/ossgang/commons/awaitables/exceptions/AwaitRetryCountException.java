package org.ossgang.commons.awaitables.exceptions;

public class AwaitRetryCountException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    public AwaitRetryCountException(String message) {
        super(message);
    }
}