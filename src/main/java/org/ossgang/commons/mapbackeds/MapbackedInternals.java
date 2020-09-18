package org.ossgang.commons.mapbackeds;

import static java.util.Objects.requireNonNull;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

final class MapbackedInternals {

    private MapbackedInternals() {
        throw new UnsupportedOperationException("only static methods");
    }

    static Optional<Method> toStringMethod(Class<?> intfc) {
        Set<Method> toStringMethods = toStringMethods(intfc);
        if (toStringMethods.size() > 1) {
            throw new IllegalArgumentException(
                    "More than one method with a ToString annotation found in the hierarchy!");
        }
        return toStringMethods.stream().findFirst();
    }

    static Set<Method> fieldMethods(Class<?> intfc) {
        return MapbackedInternals.collectInterfaceMethods(intfc, m -> !m.isDefault());
    }

    private static Set<Method> toStringMethods(Class<?> intfc) {
        return collectInterfaceMethods(intfc, m -> m.isAnnotationPresent(ToString.class));
    }

    static Set<Method> collectInterfaceMethods(Class<?> intfc, Predicate<Method> methodPredicate) {
        requireInterface(intfc);
        Set<Method> fields = findZeroParamMethodsFrom(intfc, methodPredicate);
        Class<?>[] superinterfaces = intfc.getInterfaces();
        for (Class<?> supi : superinterfaces) {
            fields.addAll(collectInterfaceMethods(supi, methodPredicate));
        }
        return fields;
    }

    private static Set<Method> findZeroParamMethodsFrom(Class<?> intfc, Predicate<Method> methodPredicate) {
        return Arrays.stream(intfc.getDeclaredMethods()) //
                .filter(m -> m.getParameterCount() == 0) //
                .filter(m -> methodPredicate.test(m)) //
                .collect(Collectors.toSet());
    }

    static <C> Class<C> requireInterface(Class<C> intfc) {
        requireNonNull(intfc, "interface must not be null");
        if (!intfc.isInterface()) {
            throw new IllegalArgumentException("Given class '" + intfc + "'is not an interface!");
        }
        return intfc;
    }

    static boolean isJava8OrLess() {
        String versionString = System.getProperty("java.version");
        String[] parts = versionString.split("\\.");
        int mainVersion = Integer.parseInt(parts[0]);
        return (mainVersion < 9);
    }

}
