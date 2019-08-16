package org.ossgang.commons.observable;

import java.util.Optional;

/**
 * Static support class for dealing with {@link Observable} and {@link ObservableValue}.
 */
public class Observables {
    private Observables() {
        throw new UnsupportedOperationException("static only");
    }

    /**
     * Create an {@link ObservableValue} from any {@link Observable}. If the observable passed in is an instance of
     * {@link ObservableValue}, it is returned unchanged. Otherwise, a derived observable value is created which
     * subscribes to the upstream observable and caches its latest value.
     *
     * @param observable the observable
     * @param <T> the type
     * @return an {@link ObservableValue} reflecting the observable provided
     */
    public static <T> ObservableValue<T> observableValueOf(Observable<T> observable) {
        if (observable instanceof ObservableValue) {
            return (ObservableValue<T>) observable;
        }
        return new DerivedObservableValue<>(observable, Optional::of);
    }

    /**
     * Create a constant {@link ObservableValue} holding any value. This observable is immutable, and will always
     * return the given value on get(). On subscribe(), it will immediately send the given value to the listener.
     *
     * @param value the value
     * @param <T> the value type
     * @return a constant ObservableValue
     */
    public static <T> ObservableValue<T> constant(T value) {
        return new ConstantObservableValue<>(value);
    }
}
