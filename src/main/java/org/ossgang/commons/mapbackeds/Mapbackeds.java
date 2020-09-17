package org.ossgang.commons.mapbackeds;

import static java.util.Collections.emptyMap;
import static java.util.Objects.requireNonNull;
import static java.util.function.Predicate.not;
import static java.util.stream.Collectors.toMap;
import static java.util.stream.Collectors.toSet;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;
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
     * <p>
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
     * @return a new builder for the mapbacked object
     * @throws IllegalArgumentException if the given class is not an interface
     */
    public static <M> Builder<M> builder(Class<M> backedInterface) {
        return builder(backedInterface, emptyMap());
    }

    /**
     * Creates a builder for a mapbacked object of the given interface, with the given initial values. The passed in map
     * can be bigger than the field actual fields of the object. Unknown fields will simply be ignored. The rest of the
     * behaviour is the same as described in {@link #builder(Class)}.
     * <p>
     * NOTE: Currently, there is no upfront check of the types of the values within the map. So if the map contains
     * incompatible values with the interface, a call to the corresponding interface methods will fail (which might be
     * much later!)
     *
     * @param the the interface which shall be backed by a map of values
     * @param initialFieldValues the initial values for the 'field' methods of the object
     * @throws IllegalArgumentException if the given class is not an interface
     * @throws NullPointerException if the initialFieldValues is {@code null}.
     */
    public static <M> Builder<M> builder(Class<M> backedInterface, Map<String, Object> initialFieldValues) {
        return new Builder<>(backedInterface, initialFieldValues);
    }

    /**
     * Creates a builder, with the field values pre-initialized from the given object, assuming that it is a mapbacked
     * object. This object can in general be of a different type, as long as the return types of the methods, containing
     * in the given interface match. values of fields not contained in the given interface are ignored.
     *
     * @param the the interface which shall be backed by a map of values
     * @param initialFieldValueSource a mapbacked object, which shall be used to pre-initialize the field values
     * @throws IllegalArgumentException if the given class is not an interface
     * @throws IllegalArgumentException if the initialFieldValueSource is not a mapbacked object
     * @throws NullPointerException if the initialFieldValues is {@code null}.
     */
    public static <M> Builder<M> builder(Class<M> backedInterface, Object initialFieldValueSource) {
        return builder(backedInterface, mapOf(initialFieldValueSource));
    }

    /**
     * Creates a mapbacked object with the given interface, backed by the given map of field Values. No Consistency
     * checks are done! This behavior might change in future versions.
     *
     * @param backedInterface the interface which shall be backed by the map
     * @param fieldValues the map containing values for all the fields within the object
     * @return a new instance of the interface with the field values from the given map
     */
    public static <M> M from(Class<M> backedInterface, Map<String, Object> fieldValues) {
        return proxy(backedInterface, new MapbackedObject(backedInterface, fieldValues));
    }

    /**
     * Retrieves the internal map of the given mapbacked object. As this will throw, if the given object is not
     * mapbacked, it is recommended to check before if the object is a mapbacked, by using the
     * {@link #isMapbacked(Object)} method.
     *
     * @param object the mapbacked object from which to retrieve the internal map
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
     * @param object the object to check
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

    /**
     * The builder for a mapbacked object.
     * <p>
     * </p>
     * This Class it NOT Thread safe!
     */
    public static class Builder<M> {

        private final Class<M> backedInterface;
        private final Set<Method> fieldMethods;
        private final Map<String, Object> mapBuilder;

        /**
         * Keeps track of the fields which were already set by {@link #field(Function, Object)} calls on this builder.
         * The reason to not use the keys of the map is, that we might ahv initialized the map with something and still
         * want to allow to chenge the field once.
         */
        private final Set<String> fieldsAlreadySet = new HashSet<>();

        private Builder(Class<M> backedInterface, Map<String, Object> initialFieldValues) {
            this.backedInterface = requireInterface(backedInterface);
            this.fieldMethods = fieldMethods(backedInterface);

            requireNonNull(initialFieldValues, "initialFieldValues must not be null");
            this.mapBuilder = new HashMap<>(filterFor(initialFieldValues, namesOf(fieldMethods)));
        }

        private static Set<String> namesOf(Set<Method> methods) {
            return methods.stream().map(Method::getName).collect(toSet());
        }

        private static Map<String, Object> filterFor(Map<String, Object> initialFieldValues, Set<String> methodNames) {
            return initialFieldValues.entrySet().stream().filter(e -> methodNames.contains(e.getKey()))
                    .collect(toMap(e -> e.getKey(), e -> e.getValue()));
        }

        public <T> Builder<M> field(Function<M, T> fieldAccess, T value) {
            requireNonNull(fieldAccess, "fieldAccess must not be null");
            requireNonNull(value, "value must not be null");

            String key = fieldName(fieldAccess);
            if (fieldsAlreadySet.contains(key)) {
                throw new IllegalStateException("Value for field '" + key + "' was alread set once!");
            }
            mapBuilder.put(key, value);
            fieldsAlreadySet.add(key);

            return this;
        }

        public <T> Builder<M> element(Function<M, List<T>> listAccess, int index, T element) {
            requireNonNull(listAccess, "fieldAccess must not be null");
            requireNonNull(element, "element must not be null");

            String key = fieldName(listAccess);
            if (!mapBuilder.containsKey(key)) {
                throw new IllegalStateException(
                        "Value for list field '" + key + "' not set! Set it before changing any element!");
            }

            List<T> oldList = (List<T>) mapBuilder.get(key);
            List<T> newList = new ArrayList<>(oldList);
            newList.set(index, element);

            mapBuilder.put(key, newList);

            return this;
        }

        private <T> String fieldName(Function<M, T> fieldAccess) {
            MethodCapture capture = new MethodCapture(fieldMethods);
            M proxy = proxy(backedInterface, capture);
            fieldAccess.apply(proxy);
            String key = capture.singleCapturedMethod().getName();
            return key;
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
        return collectIntfcMethods(intfc, not(Method::isDefault));
    }

    public static Optional<Method> toStringMethod(Class<?> intfc) {
        Set<Method> toStringMethods = toStringMethods(intfc);
        if (toStringMethods.size() > 1) {
            throw new IllegalArgumentException(
                    "More than one method with a ToString annotation found in the hierarchy!");
        }
        return toStringMethods.stream().findFirst();
    }

    private static Set<Method> toStringMethods(Class<?> intfc) {
        return collectIntfcMethods(intfc, m -> m.isAnnotationPresent(ToString.class));
    }

    private static Set<Method> collectIntfcMethods(Class<?> intfc, Predicate<Method> methodPredicate) {
        requireInterface(intfc);
        Set<Method> fields = findZeroParamMethodsFrom(intfc, methodPredicate);
        Class<?>[] superinterfaces = intfc.getInterfaces();
        for (Class<?> supi : superinterfaces) {
            fields.addAll(collectIntfcMethods(supi, methodPredicate));
        }
        return fields;
    }

    private static Set<Method> findZeroParamMethodsFrom(Class<?> intfc, Predicate<Method> methodPredicate) {
        return Arrays.stream(intfc.getDeclaredMethods()) //
                .filter(m -> m.getParameterCount() == 0) //
                .filter(m -> methodPredicate.test(m)) //
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
