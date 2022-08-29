package org.ossgang.commons.types;

import static java.util.Objects.requireNonNull;

import java.util.Objects;

/**
 * Represents some named object that would have a value of a certain type. This object does not store the value, but
 * only its type. E.g. a field of a mapbacked object could be a typed name...
 * 
 * @param <T> the type for the name
 */
public class TypedName<T> {

    public static <T> TypedName<T> of(String name, Class<T> type) {
        return new TypedName<>(name, type);
    }

    private final String name;
    private final Class<T> type;

    private TypedName(String name, Class<T> type) {
        this.name = requireNonNull(name, "name must not be null!");
        this.type = requireNonNull(type, "type must not be null!");
    }

    public String name() {
        return name;
    }

    public Class<T> type() {
        return type;
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, type);
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
        TypedName<?> other = (TypedName<?>) obj;
        return Objects.equals(name, other.name) && Objects.equals(type, other.type);
    }

    @Override
    public String toString() {
        return "TypedName [name=" + name + ", type=" + type + "]";
    }

}
