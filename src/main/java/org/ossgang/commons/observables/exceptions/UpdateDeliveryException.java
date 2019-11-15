package org.ossgang.commons.observables.exceptions;

import org.ossgang.commons.observables.Observer;

/**
 * An exception wrapping any exception thrown by the onNext or onException methods of {@link Observer}. It also provides
 * access to the value that failed to be dispatched through getValue().
 */
public class UpdateDeliveryException extends RuntimeException {
    private final Object value;

    public UpdateDeliveryException(Object value, Throwable cause) {
        super("Error delivering update : " + cause.getMessage() + "\n -> value: " + value, cause);
        this.value = value;
    }

    public Object getValue() {
        return value;
    }
}
