package org.ossgang.commons.utils;

import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Useful methods to interact with system properties
 */
public final class SystemProperties {

    /**
     * Convenience method to wrap with {@link Optional} a call to {@link System#getProperty(String)}.
     *
     * @param propertyName to look for
     * @return an {@link Optional} with the property value or empty
     */
    public static Optional<String> getProperty(String propertyName) {
        return Optional.ofNullable(System.getProperty(propertyName));
    }

    /**
     * Convenience method that applies the mapper function to the system property value in case it is present.
     *
     * @param propertyName to look for
     * @param mapper       to be used to interpret the system property
     * @param <T>          the resulting type
     * @return an {@link Optional} with the property, mapped, value or empty
     */
    public static <T> Optional<T> getProperty(String propertyName, Function<String, T> mapper) {
        return getProperty(propertyName).map(mapper);
    }

    /**
     * Set the system property with the provided value if case the property is not already set.
     * This is just a convenience method that combines {@link System#getProperty(String)} and {@link System#setProperty(String, String)}
     * NOTE: this method is not thread-safe !
     *
     * @param propertyName  to look for
     * @param propertyValue to set in case the specified property is not set
     * @return true in case the property has been set by this method, false otherwise (the property was already set)
     */
    public static boolean setPropertyIfNotSet(String propertyName, String propertyValue) {
        if (System.getProperty(propertyName) == null) {
            System.setProperty(propertyName, propertyValue);
            return true;
        }
        return false;
    }

    /**
     * Set the system property with the provided value if case the property is not already set.
     * This is just a convenience method that combines {@link System#getProperty(String)} and {@link System#setProperty(String, String)}
     * Returns a consumer that can be used to react to the system property value resulted from the method execution.
     * This is useful in case of actions (typically logs) that need to be executed after setting (or not) the property.
     * E.g.:
     * <code>
     * setPropertyIfNotSetAndThen("myProperty", "myValue").accept((p, v) -> System.out.printf("Using %s with %s%n", p, v));
     * </code>
     * NOTE: this method is not thread-safe !
     *
     * @param propertyName  to look for
     * @param propertyValue to set in case the specified property is not set
     * @return a {@link Consumer} accepting a {@link BiConsumer} that will be called with the property name and value as parameters
     */
    public static Consumer<BiConsumer<String, String>> setPropertyIfNotSetAndThen(String propertyName, String propertyValue) {
        setPropertyIfNotSet(propertyName, propertyValue);
        String possiblyUpdatedPropertyValue = System.getProperty(propertyName);
        return (action) -> action.accept(propertyName, possiblyUpdatedPropertyValue);
    }

    private SystemProperties() {
        throw new UnsupportedOperationException("static only");
    }

}
