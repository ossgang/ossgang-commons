package org.ossgang.commons.observables.exceptions;

import org.ossgang.commons.observables.Observer;

/**
 * An exception which is thrown by the default implementation of {@link Observer}, in case the onException() method has
 * not been implemented. It will get dispatched to the global uncaught exception handler.
 */
public class UnhandledException extends RuntimeException {
    public UnhandledException(Throwable cause) {
        super("Unhandled exception: " + cause.getClass().getSimpleName() + " : " + cause.getMessage(), cause);
    }
}
