package org.ossgang.commons.observables;

import static java.util.Objects.requireNonNull;

import java.util.Objects;

/**
 * Represents a transition from an old value of an observable value to a new one. This is used in accumulate operations
 * of
 * properties.
 */
public class Transition<T> {

    private final T oldValue;
    private final T newValue;

    private Transition(T oldValue, T newValue) {
        this.oldValue = oldValue; /* The old value is allowed to be null */
        this.newValue = requireNonNull(newValue, "new value must not be null");
    }

    /**
     * Factory method for a value transition.
     * 
     * @param oldValue the old value (before the transition)
     * @param newValue the new value (after the transition)
     */
    public static <T> Transition<T> fromTo(T oldValue, T newValue) {
        return new Transition<>(oldValue, newValue);
    }

    public T oldValue() {
        return oldValue;
    }

    public T newValue() {
        return newValue;
    }

    @Override
    public int hashCode() {
        return Objects.hash(newValue, oldValue);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        Transition<?> other = (Transition<?>) obj;
        return Objects.equals(newValue, other.newValue) && Objects.equals(oldValue, other.oldValue);
    }

    @Override
    public String toString() {
        return "Transition [oldValue=" + oldValue + ", newValue=" + newValue + "]";
    }

}
