package org.ossgang.commons.observables;

import org.ossgang.commons.observables.exceptions.UnhandledException;
import org.ossgang.commons.observables.exceptions.UpdateDeliveryException;
import org.ossgang.commons.observables.operators.Operators;
import org.ossgang.commons.observables.operators.connectors.ConnectorObservableValue;
import org.ossgang.commons.observables.operators.connectors.ConnectorObservables;
import org.ossgang.commons.observables.operators.connectors.DynamicConnectorObservableValue;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Static support class for dealing with {@link Observable} and {@link ObservableValue}.
 */
public final class Observables {
    private Observables() {
        throw new UnsupportedOperationException("static only");
    }

    /**
     * Create an {@link Dispatcher} with the specified initial value.
     *
     * @param initial the initial value of the {@link Dispatcher}
     * @param <T>     the type of the observable
     * @return an {@link Dispatcher} with the specified initial value
     */
    public static <T> Dispatcher<T> dispatcher(T initial) {
        return new SimpleDispatcher<>(initial);
    }

    /**
     * Create an {@link Dispatcher}.
     *
     * @param <T> the type of the observable
     * @return an {@link Dispatcher}
     */
    public static <T> Dispatcher<T> dispatcher() {
        return new SimpleDispatcher<>(null);
    }

    /**
     * Creates a {@link ConnectorObservableValue} that on each connection will subscribe to the upstream {@link ObservableValue}
     * produced by the specified {@link Supplier}
     *
     * @param upstreamSupplier the supplier of upstream {@link ObservableValue} to be used when connecting
     * @param <T>              the type of the observable
     * @return a {@link ConnectorObservableValue} that uses the specified {@link Supplier} for connecting upstream
     * @see ConnectorObservables#connectorObservableValue(Supplier)
     */
    public static <T> ConnectorObservableValue<T> connectorObservableValue(Supplier<ObservableValue<T>> upstreamSupplier) {
        return ConnectorObservables.connectorObservableValue(upstreamSupplier);
    }

    /**
     * Creates a {@link ConnectorObservableValue} that will connect to the specified upstream {@link ObservableValue}.
     *
     * @param upstream the upstream {@link ObservableValue} to connect to
     * @param <T>      the type of the observable
     * @return a {@link ConnectorObservableValue} that connects to the specified {@link ObservableValue}
     * @see ConnectorObservables#connectorTo(ObservableValue)
     */
    public static <T> ConnectorObservableValue<T> connectorTo(ObservableValue<T> upstream) {
        return ConnectorObservables.connectorTo(upstream);
    }

    /**
     * Creates a {@link DynamicConnectorObservableValue}
     *
     * @param <T> the type of the observable
     * @return a {@link DynamicConnectorObservableValue}
     * @see ConnectorObservables#dynamicConnectorObservableValue()
     */
    public static <T> DynamicConnectorObservableValue<T> dynamicConnectorObservableValue() {
        return ConnectorObservables.dynamicConnectorObservableValue();
    }

    /**
     * Create a constant {@link ObservableValue} holding any value. This observable is immutable, and will always
     * return the given value on get(). On subscribe(), it will immediately send the given value to the listener.
     *
     * @param value the value
     * @param <T>   the value type
     * @return a constant ObservableValue
     */
    public static <T> ObservableValue<T> constant(T value) {
        return new ConstantObservableValue<>(value);
    }

    /**
     * Create a constant {@link ObservableValue} holding an exception. This observable is immutable, will always return
     * null on get(), and it will immediately send the exception to the consumer on subscribe().
     *
     * @param throwable the exception to throw
     * @param <T>   any type, to be compatible with any ObservableValue signature
     * @return a constant ObservableValue
     */
    public static <T> ObservableValue<T> constantException(Throwable throwable) {
        return new ConstantExceptionObservableValue<>(throwable);
    }

    /**
     * Create an {@link ObservableValue} from any {@link Observable}. If the observable passed in is an instance of
     * {@link ObservableValue}, it is returned unchanged. Otherwise, a derived observable value is created which
     * subscribes to the upstream observable and caches its latest value.
     *
     * @param observable the observable
     * @param <T>        the type
     * @return an {@link ObservableValue} reflecting the observable provided
     */
    public static <T> ObservableValue<T> observableValueOf(Observable<T> observable) {
        return Operators.observableValueOf(observable);
    }

    /**
     * Produces an {@link ObservableValue} that zips the values of the provided {@link ObservableValue}. The {@link Map}
     * parameter provides the indexes of the {@link Map} that will be passed to the specified mapper {@link Function}.
     * <br>
     * Note: this operator will wait until all the source {@link ObservableValue}s have a value to match.
     *
     * @param sourcesMap the input {@link ObservableValue}s indexed
     * @param combiner   the combining function that will produce the result
     * @param <K>        the indexing type
     * @param <I>        the input type
     * @param <O>        the output type
     * @return an {@link ObservableValue} that zips the values of the provided {@link ObservableValue}. The {@link Map}
     * parameter provides the indexes of the {@link Map} that will be passed to the specified mapper {@link Function}.
     */
    public static <K, I, O> ObservableValue<O> zip(Map<K, ? extends Observable<I>> sourcesMap, Function<Map<K, I>, O> combiner) {
        return Operators.zip(sourcesMap, combiner);
    }

    /**
     * Produces an {@link ObservableValue} that zips the values of the provided {@link ObservableValue}. The index of
     * the provided {@link ObservableValue} matches the index of the {@link List} of values provided to the mapping
     * {@link Function}.
     * <br>
     * Note: this operator will wait until all the source {@link ObservableValue}s have a value to match.
     *
     * @param sources  the input {@link ObservableValue}s
     * @param combiner the combining function that will produce the result
     * @param <I>      the input type
     * @param <O>      the output type
     * @return an {@link ObservableValue} that zips the values of the provided {@link ObservableValue}. The index of
     * the provided {@link ObservableValue} matches the index of the {@link List} of values provided to the mapping
     * {@link Function}.
     */
    public static <I, O> ObservableValue<O> zip(List<? extends Observable<I>> sources, Function<List<I>, O> combiner) {
        return Operators.zip(sources, combiner);
    }

    /**
     * Produces an {@link ObservableValue} that zips the values of the provided {@link ObservableValue}. The {@link Map}
     * parameter provides the indexes of the resulting {@link ObservableValue} values.
     * <br>
     * Note: this operator will wait until all the source {@link ObservableValue}s have a value to match.
     *
     * @param sourcesMap the input {@link ObservableValue}s indexed
     * @param <K>        the indexing type
     * @param <I>        the input type
     * @return an {@link ObservableValue} that zips the values of the provided {@link ObservableValue}. The {@link Map} parameter provides the indexes of the
     * resulting {@link ObservableValue} values.
     */
    public static <K, I> ObservableValue<Map<K, I>> zip(Map<K, ? extends Observable<I>> sourcesMap) {
        return Operators.zip(sourcesMap);
    }

    /**
     * Produces an {@link ObservableValue} that zips the values of the provided {@link ObservableValue}. The index of
     * the provided {@link ObservableValue} matches the index of the {@link List} in the resulting {@link ObservableValue}.
     * <br>
     * Note: this operator will wait until all the source {@link ObservableValue}s have a value to match.
     *
     * @param sources the input {@link ObservableValue}s
     * @param <I>     the input type
     * @return an {@link ObservableValue} that zips the values of the provided {@link ObservableValue}. The index of
     * the provided {@link ObservableValue} matches the index of the {@link List} in the resulting {@link ObservableValue}.
     */
    public static <I> ObservableValue<List<I>> zip(List<? extends Observable<I>> sources) {
        return Operators.zip(sources);
    }

    /**
     * Produces an {@link ObservableValue} that on each update of any source {@link ObservableValue} publishes the
     * result of applying the mapper {@link Function}. The {@link Map}s are used to avoid mathing values by index.
     *
     * @param sourcesMap the input {@link ObservableValue}s indexed
     * @param combiner   the combining function that will produce the result
     * @param <K>        the key for each input {@link ObservableValue} that is used for indexing
     * @param <I>        the input type
     * @param <O>        the output type
     * @return an {@link ObservableValue} that on each update of any source {@link ObservableValue} publishes the
     * result of applying the mapper {@link Function}. The {@link Map}s are used to avoid mathing values by index.
     */
    public static <K, I, O> ObservableValue<O> combineLatest(Map<K, ? extends Observable<I>> sourcesMap,
                                                             Function<Map<K, I>, O> combiner) {
        return Operators.combineLatest(sourcesMap, combiner);
    }

    /**
     * Produces an {@link ObservableValue} that on each update of any source {@link ObservableValue} publishes the
     * result of the combiner applied with the latest values of the other inputs.
     * The order of the input values of the combiner is the same as the order of the provided source {@link ObservableValue}s
     *
     * @param sources  the input {@link ObservableValue}s
     * @param combiner the combining function that will produce the result
     * @param <I>      the input type
     * @param <O>      the output type
     * @return an {@link ObservableValue} that on each update of any source publishes the result of the
     * combiner applied with the latest values of the other inputs.
     */
    public static <I, O> ObservableValue<O> combineLatest(List<? extends Observable<I>> sources,
                                                          Function<List<I>, O> combiner) {
        return Operators.combineLatest(sources, combiner);
    }

    /**
     * Produces an {@link ObservableValue} that on each update of any source {@link ObservableValue} publishes a
     * {@link Map} with the values of each corresponding source {@link ObservableValue}.
     *
     * @param sourcesMap the input {@link ObservableValue}s indexed
     * @param <K>        the key for each input {@link ObservableValue} that is used for indexing
     * @param <I>        the input type
     * @return an {@link ObservableValue} that on each update of any source {@link ObservableValue} publishes a
     * {@link Map} with the values of each corresponding source {@link ObservableValue}.
     */
    public static <K, I> ObservableValue<Map<K, I>> combineLatest(Map<K, ? extends Observable<I>> sourcesMap) {
        return Operators.combineLatest(sourcesMap);
    }

    /**
     * Produces an {@link ObservableValue} that on each update of any source {@link ObservableValue} publishes the
     * a list containing the values of each {@link ObservableValue}
     *
     * @param sources the input {@link ObservableValue}s
     * @param <I>     the input type
     * @return an {@link ObservableValue} that on each update of any source {@link ObservableValue} publishes the
     * a list containing the values of each {@link ObservableValue}
     */
    public static <I> ObservableValue<List<I>> combineLatest(List<? extends Observable<I>> sources) {
        return Operators.combineLatest(sources);
    }

    /**
     * Sets a static, framework-wide uncaught exception handler. It is called in the following cases:
     * <ul>
     *     <li>If an observer onNext/onException throws, with an {@link UpdateDeliveryException}</li>
     *     <li>If an observer does not implement onException and an exception would be delivered, by
     *     an {@link UnhandledException}</li>
     * </ul>
     * In either case, getCause() can be used to obtain the original exception.
     *
     * @param handler the exception handler to be called
     * @see UpdateDeliveryException
     * @see UnhandledException
     */
    public static void setUncaughtExceptionHandler(Consumer<Exception> handler) {
        ExceptionHandlers.setUncaughtExceptionHandler(handler);
    }

    public static ObservableValue<Instant> periodicEvery(long period, TimeUnit unit) {
        return new PeriodicObservableValue(period, unit);
    }



}
