package org.ossgang.commons.mapbackeds;

import static java.util.Objects.requireNonNull;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * This class provides static methods for working with Mapbacked objects. Mapbacked objects are intended to be used e.g.
 * as very simple immutable domain objects: The only requirement to create a mapbacked object is to define an interface
 * which contains methods corresponding to field names of the objects. Having this, new objects can be used using a
 * builder and the interface methods can be used as getters.
 * NOTE: Only methods with return value and zero arguments are taken into account for storing values. Calling other
 * methods (which are not default methods) will result in errors.
 *
 * @see #builder(Class)
 * @see #from(Class, Map)
 */
public final class Mapbackeds {

    private Mapbackeds() {
        throw new UnsupportedOperationException("only static methods");
    }

    /**
     * Creates a builder for the a mapbacked object, implementing the given interface.
     * <p/>
     * As an example, assuming an domain object which contains 2 fields:
     *
     * <pre>
     * private interface Person {
     *     long id();
     *
     *     String name();
     * }
     * </pre>
     *
     * Then an instance of this person can be created using a builder as follows:
     *
     * <pre>
     * Person misterX = Mapbackeds.builder(Person.class) //
     *         .field(Person::id, 1) //
     *         .field(Person::name, "MisterX") //
     *         .build();
     * </pre>
     *
     * NOTE: Currently, there is no check for null values. Therefore, only partially filled objects (or even empty
     * ones) would be allowed. Fields which are not set, would return {@code null} in this case. However, be aware that
     * methods returning primitive types would throw in this case! This behavior might change in future versions.
     *
     * @param backedInterface the interface which shall be backed by a map of values.
     * @throws IllegalArgumentException if the given class is not an interface
     */
    public static <M> Builder<M> builder(Class<M> backedInterface) {
        return new Builder<>(backedInterface);
    }

    /**
     * Creates a mapbacked object with the given interface, backed by the given map of field Values. No Consistency
     * checks are done! This behavior might change in future versions.
     */
    public static <M> M from(Class<M> backedInterface, Map<String, Object> fieldValues) {
        return proxy(backedInterface, new MapbackedObject(backedInterface, fieldValues));
    }

    /**
     * Retrieves the internal map of the given mapbacked object. As this will throw, if the given object is not
     * mapbacked, it is recommended to check before if the object is a mapbacked, by using the
     * {@link #isMapbacked(Object)} method.
     *
     * @param the mapbacked object from which to retrieve the internal map
     * @return the internal map of the object
     * @throws IllegalArgumentException if the given object is not a mapbacked object
     */
    public static Map<String, Object> mapOf(Object object) {
        Optional<MapbackedObject> handler = handlerFrom(object);
        if (handler.isPresent()) {
            return handler.get().fieldValues();
        }
        throw new IllegalArgumentException("The given object seems not to be a Mapbacked object: " + object);
    }

    /**
     * Checks, if the given object is mapbacked
     *
     * @param the object to check
     * @return {@code true} if the given object is mapbacked, {@code false} otherwise
     */
    public static boolean isMapbacked(Object object) {
        return handlerFrom(object).isPresent();
    }

    private static Optional<MapbackedObject> handlerFrom(Object object) {
        if (!(object instanceof Proxy)) {
            return Optional.empty();
        }

        InvocationHandler handler = Proxy.getInvocationHandler(object);
        if (!(handler instanceof MapbackedObject)) {
            return Optional.empty();
        }

        return Optional.of((MapbackedObject) handler);
    }

    public static class Builder<M> {

        private final Class<M> backedInterface;
        private final Set<Method> fieldMethods;
        private final Map<String, Object> mapBuilder = new HashMap<>();

        private Builder(Class<M> backedInterface) {
            this.backedInterface = requireInterface(backedInterface);
            this.fieldMethods = fieldMethods(backedInterface);
        }

        public <T> Builder<M> field(Function<M, T> fieldAccess, T value) {
            requireNonNull(fieldAccess, "fieldAccess must not be null");
            requireNonNull(value, "value must not be null");

            MethodCapture capture = new MethodCapture(fieldMethods);
            M proxy = proxy(backedInterface, capture);
            fieldAccess.apply(proxy);
            String key = capture.singleCapturedMethod().getName();
            if (mapBuilder.containsKey(key)) {
                throw new IllegalArgumentException("Already a value for method '" + key + "' available!");
            }
            mapBuilder.put(key, value);

            return this;
        }

        public M build() {
            InvocationHandler handler = new MapbackedObject(backedInterface, mapBuilder);
            return proxy(backedInterface, handler);
        }

    }

    private static <M> M proxy(Class<M> intfc, InvocationHandler handler) {
        return (M) Proxy.newProxyInstance(Mapbackeds.class.getClassLoader(), new Class<?>[] { intfc }, handler);
    }

    public static Set<Method> fieldMethods(Class<?> intfc) {
        requireInterface(intfc);
        return Arrays.stream(intfc.getDeclaredMethods()) //
                .filter(m -> m.getParameterCount() == 0) //
                .filter(m -> !m.isDefault()) //
                .collect(Collectors.toSet());
    }

    public static <C extends Class<?>> C requireInterface(C intfc) {
        requireNonNull(intfc, "interface must not be null");
        if (!intfc.isInterface()) {
            throw new IllegalArgumentException("Given class '" + intfc + "'is not an interface!");
        }
        return intfc;
    }

    private final static class MethodCapture implements InvocationHandler {

        private static final Map<Class<?>, Object> RETURN_VALUES = new HashMap<>();

        // load
        static {
            RETURN_VALUES.put(boolean.class, Boolean.FALSE);
            RETURN_VALUES.put(byte.class, (byte) 0);
            RETURN_VALUES.put(short.class, (short) 0);
            RETURN_VALUES.put(int.class, 0);
            RETURN_VALUES.put(long.class, 0L);
            RETURN_VALUES.put(char.class, '\0');
            RETURN_VALUES.put(float.class, 0.0F);
            RETURN_VALUES.put(double.class, 0.0);
        }

        private final Set<Method> interestedMethods;

        public MethodCapture(Set<Method> interestedMethods) {
            this.interestedMethods = interestedMethods;
        }

        private final List<Method> capturedMethods = new ArrayList<>();

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            if (interestedMethods.contains(method)) {
                capturedMethods.add(method);
            }
            return RETURN_VALUES.get(method.getReturnType());
        }

        Method singleCapturedMethod() {
            int size = capturedMethods.size();
            if (size != 1) {
                throw new java.lang.IllegalStateException(
                        "Exactly one field method is exepected to be called in capturing function. However " + size
                                + " were called.");
            }
            return capturedMethods.get(0);
        }
    }

}
