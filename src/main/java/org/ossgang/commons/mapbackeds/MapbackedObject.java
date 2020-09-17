package org.ossgang.commons.mapbackeds;

import static java.util.Objects.requireNonNull;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * This is the 'Implementation' of a mapbacked object. It shall never instantiated directly by the user, but always
 * factory methods or builders from the Mapbackeds class shall be used.
 * The responsibilities of this class are:
 * <ul>
 * <li>delegate getter calls to appropriate lookups in the internal map</li>
 * <li>delegate calls to equals(), hashCode() and toString() to the internal implementations</li>
 * <li>delegate calls to default methods of the proxied interface to the real implementations</li>
 * </ul>
 */
class MapbackedObject implements InvocationHandler {

    private final Class<?> intfc;
    private final Map<String, Object> fieldValues;
    private final Set<Method> fieldMethods;

    private final Method toStringMethod;

    MapbackedObject(Class<?> intfc, Map<String, Object> fieldValues) {
        this.intfc = requireNonNull(intfc, "interface must not be null");
        this.fieldValues = new HashMap<>(fieldValues);
        fieldMethods = Mapbackeds.fieldMethods(intfc);
        toStringMethod = Mapbackeds.toStringMethod(intfc).orElse(null);
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        if (method.isDefault()) {
            return MethodHandles.lookup()
                    .findSpecial(intfc, method.getName(),
                            MethodType.methodType(method.getReturnType(), method.getParameterTypes()), intfc)
                    .bindTo(proxy).invokeWithArguments(args);
        }
        if (fieldMethods.contains(method)) {
            return resolveOnMap(method);
        }
        if ("toString".equals(method.getName()) && (args == null)) {
            if (toStringMethod != null) {
                return Objects.toString(invoke(proxy, toStringMethod, args));
            }
            return toString();
        }
        if ("hashCode".equals(method.getName()) && (args == null)) {
            return hashCode();
        }
        if ("equals".equals(method.getName()) && (args.length == 1)) {
            return equals(args[0]);
        }

        throw new UnsupportedOperationException(
                "could not invoke method '" + method.getName() + "' with arguments '" + Arrays.toString(args) + "'");

    }

    Map<String, Object> fieldValues() {
        return new HashMap<>(fieldValues);
    }

    private Object resolveOnMap(Method method) {
        return fieldValues.get(method.getName());
    }

    @Override
    public int hashCode() {
        return Objects.hash(fieldMethods, fieldValues, intfc);
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
        MapbackedObject other = (MapbackedObject) obj;
        return Objects.equals(fieldMethods, other.fieldMethods) && Objects.equals(fieldValues, other.fieldValues)
                && Objects.equals(intfc, other.intfc);
    }

    @Override
    public String toString() {
        return "MapbackedObject [intfc=" + intfc + ", fieldValues=" + fieldValues + ", fieldMethods=" + fieldMethods
                + "]";
    }
}
