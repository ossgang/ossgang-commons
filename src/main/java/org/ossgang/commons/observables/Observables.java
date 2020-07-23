package org.ossgang.commons.observables;

import org.ossgang.commons.monads.Function3;
import org.ossgang.commons.monads.Function4;
import org.ossgang.commons.monads.Function5;
import org.ossgang.commons.observables.exceptions.UnhandledException;
import org.ossgang.commons.observables.exceptions.UpdateDeliveryException;
import org.ossgang.commons.observables.operators.CombineLatestOperators;
import org.ossgang.commons.observables.operators.Operators;
import org.ossgang.commons.observables.operators.connectors.ConnectorObservableValue;
import org.ossgang.commons.observables.operators.connectors.ConnectorObservables;
import org.ossgang.commons.observables.operators.connectors.DynamicConnectorObservableValue;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.function.BiFunction;
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
     * Creates a {@link ConnectorObservableValue} that on each connection will subscribe to the upstream
     * {@link ObservableValue}
     * produced by the specified {@link Supplier}
     *
     * @param upstreamSupplier the supplier of upstream {@link ObservableValue} to be used when connecting
     * @param <T>              the type of the observable
     * @return a {@link ConnectorObservableValue} that uses the specified {@link Supplier} for connecting upstream
     * @see ConnectorObservables#connectorObservableValue(Supplier)
     */
    public static <T> ConnectorObservableValue<T> connectorObservableValue(
            Supplier<ObservableValue<T>> upstreamSupplier) {
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
     * @param <T>       any type, to be compatible with any ObservableValue signature
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
     * parameter provides the indexes of the {@link Map} that will be passed to the specified mapper
     * {@link Function}.
     */
    public static <K, I, O> ObservableValue<O> zip(Map<K, ? extends Observable<I>> sourcesMap,
                                                   Function<Map<K, I>, O> combiner) {
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
     * the provided {@link ObservableValue} matches the index of the {@link List} of values provided to the
     * mapping
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
     * @return an {@link ObservableValue} that zips the values of the provided {@link ObservableValue}. The {@link Map}
     * parameter provides the indexes of the
     * resulting {@link ObservableValue} values.
     */
    public static <K, I> ObservableValue<Map<K, I>> zip(Map<K, ? extends Observable<I>> sourcesMap) {
        return Operators.zip(sourcesMap);
    }

    /**
     * Produces an {@link ObservableValue} that zips the values of the provided {@link ObservableValue}. The index of
     * the provided {@link ObservableValue} matches the index of the {@link List} in the resulting
     * {@link ObservableValue}.
     * <br>
     * Note: this operator will wait until all the source {@link ObservableValue}s have a value to match.
     *
     * @param sources the input {@link ObservableValue}s
     * @param <I>     the input type
     * @return an {@link ObservableValue} that zips the values of the provided {@link ObservableValue}. The index of
     * the provided {@link ObservableValue} matches the index of the {@link List} in the resulting
     * {@link ObservableValue}.
     */
    public static <I> ObservableValue<List<I>> zip(List<? extends Observable<I>> sources) {
        return Operators.zip(sources);
    }

    /**
     * Produces an {@link ObservableValue} that on each update of any source {@link ObservableValue} publishes the
     * result of the combiner applied with the latest values of the other inputs.
     * The {@link Map}s are used to avoid matching values by index.
     *
     * @param sourcesMap the input {@link ObservableValue}s indexed
     * @param combiner   the combining function that will produce the result
     * @param <K>        the key for each input {@link ObservableValue} that is used for indexing
     * @param <I>        the input type
     * @param <O>        the output type
     * @return an {@link ObservableValue} that on each update of any source {@link ObservableValue} publishes the
     * result of combiner applied with the latest values of the other inputs.
     */
    public static <K, I, O> ObservableValue<O> combineLatest(Map<K, ? extends Observable<I>> sourcesMap,
                                                             Function<Map<K, I>, O> combiner) {
        return CombineLatestOperators.combineLatest(sourcesMap, combiner);
    }

    /**
     * Produces an {@link ObservableValue} that on each update of any source {@link ObservableValue} publishes the
     * result of the combiner applied with the latest values of the other inputs.
     * The order of the input values of the combiner is the same as the order of the provided source
     * {@link ObservableValue}s
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
        return CombineLatestOperators.combineLatest(sources, combiner);
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
        return CombineLatestOperators.combineLatest(sourcesMap);
    }

    /**
     * Produces an {@link ObservableValue} that on each update of any source {@link ObservableValue} publishes the
     * a {@link List} containing the values of each {@link ObservableValue}
     *
     * @param sources the input {@link ObservableValue}s
     * @param <I>     the input type
     * @return an {@link ObservableValue} that on each update of any source {@link ObservableValue} publishes the
     * a {@link List} containing the values of each {@link ObservableValue}
     */
    public static <I> ObservableValue<List<I>> combineLatest(List<? extends Observable<I>> sources) {
        return CombineLatestOperators.combineLatest(sources);
    }

    /**
     * Produces an {@link ObservableValue} that on each update of any source {@link ObservableValue} publishes the
     * result of the combiner applied with the latest values of the other inputs.
     * The {@link Map}s are used to avoid matching values by index.
     * This method does not restrict the type of the input values and, therefore, produces {@link Object}s as result.
     * To be used in special cases when the typed version of the combineLatest operator cannot be used!
     *
     * @param sourcesMap the input {@link ObservableValue}s indexed
     * @param combiner   the combining function that will produce the result
     * @param <K>        the key for each input {@link ObservableValue} that is used for indexing
     * @param <O>        the output type
     * @return an {@link ObservableValue} that on each update of any source {@link ObservableValue} publishes the
     * result of combiner applied with the latest values of the other inputs.
     */
    public static <K, O> ObservableValue<O> combineLatestObjects(Map<K, ? extends Observable<?>> sourcesMap,
                                                                 Function<Map<K, Object>, O> combiner) {
        return CombineLatestOperators.combineLatestObjects(sourcesMap, combiner);
    }

    /**
     * Produces an {@link ObservableValue} that on each update of any source {@link ObservableValue} publishes the
     * result of the combiner applied with the latest values of the other inputs.
     * The order of the input values of the combiner is the same as the order of the provided source
     * {@link ObservableValue}s.
     * This method does not restrict the type of the input values and, therefore, produces {@link Object}s as result.
     * To be used in special cases when the typed version of the combineLatest operator cannot be used!
     *
     * @param sources  the input {@link ObservableValue}s
     * @param combiner the combining function that will produce the result
     * @param <O>      the output type
     * @return an {@link ObservableValue} that on each update of any source publishes the result of the
     * combiner applied with the latest values of the other inputs.
     */
    public static <O> ObservableValue<O> combineLatestObjects(List<? extends Observable<?>> sources,
                                                              Function<List<Object>, O> combiner) {
        return CombineLatestOperators.combineLatestObjects(sources, combiner);
    }

    /**
     * Produces an {@link ObservableValue} that on each update of any source {@link ObservableValue} publishes a
     * {@link Map} with the values of each corresponding source {@link ObservableValue}.
     * This method does not restrict the type of the input values and, therefore, produces {@link Object}s as result.
     * To be used in special cases when the typed version of the combineLatest operator cannot be used!
     *
     * @param sourcesMap the input {@link ObservableValue}s indexed
     * @param <K>        the key for each input {@link ObservableValue} that is used for indexing
     * @return an {@link ObservableValue} that on each update of any source {@link ObservableValue} publishes a
     * {@link Map} with the values of each corresponding source {@link ObservableValue}.
     */
    public static <K> ObservableValue<Map<K, Object>> combineLatestObjects(Map<K, ? extends Observable<?>> sourcesMap) {
        return CombineLatestOperators.combineLatestObjects(sourcesMap);
    }

    /**
     * Produces an {@link ObservableValue} that on each update of any source {@link ObservableValue} publishes the
     * a {@link List} containing the values of each {@link ObservableValue}.
     * This method does not restrict the type of the input values and, therefore, produces {@link Object}s as result.
     * To be used in special cases when the typed version of the combineLatest operator cannot be used!
     *
     * @param sources the input {@link ObservableValue}s
     * @return an {@link ObservableValue} that on each update of any source {@link ObservableValue} publishes the
     * a {@link List} containing the values of each {@link ObservableValue}
     */
    public static ObservableValue<List<Object>> combineLatestObjects(List<? extends Observable<?>> sources) {
        return CombineLatestOperators.combineLatestObjects(sources);
    }

    /**
     * Produces an {@link ObservableValue} that on each update of any source {@link ObservableValue} publishes the
     * result of the combiner applied with the latest values of the other input.
     *
     * @param source1  the first input {@link ObservableValue}
     * @param source2  the second input {@link ObservableValue}
     * @param combiner the combining function that will produce the result
     * @param <I1>     the first input value type
     * @param <I2>     the second input value type
     * @param <O>      the output type
     * @return an {@link ObservableValue} that on each update of any source published the result of the combiner applied
     * with the latest values of the other input
     */
    public static <I1, I2, O> ObservableValue<O> combineLatest(Observable<I1> source1, Observable<I2> source2,
                                                               BiFunction<I1, I2, O> combiner) {
        return CombineLatestOperators.combineLatest(source1, source2, combiner);
    }

    /**
     * Produces an {@link ObservableValue} that on each update of any source {@link ObservableValue} publishes the
     * result of the combiner applied with the latest values of the other inputs.
     *
     * @param source1  the first input {@link ObservableValue}
     * @param source2  the second input {@link ObservableValue}
     * @param source3  the third input {@link ObservableValue}
     * @param combiner the combining function that will produce the result
     * @param <I1>     the first input value type
     * @param <I2>     the second input value type
     * @param <I3>     the third input value type
     * @param <O>      the output type
     * @return an {@link ObservableValue} that on each update of any source published the result of the combiner applied
     * with the latest values of the other input
     */
    public static <I1, I2, I3, O> ObservableValue<O> combineLatest(Observable<I1> source1, Observable<I2> source2,
                                                                   Observable<I3> source3,
                                                                   Function3<I1, I2, I3, O> combiner) {
        return CombineLatestOperators.combineLatest(source1, source2, source3, combiner);
    }

    /**
     * Produces an {@link ObservableValue} that on each update of any source {@link ObservableValue} publishes the
     * result of the combiner applied with the latest values of the other inputs.
     *
     * @param source1  the first input {@link ObservableValue}
     * @param source2  the second input {@link ObservableValue}
     * @param source3  the third input {@link ObservableValue}
     * @param source4  the fourth input {@link ObservableValue}
     * @param combiner the combining function that will produce the result
     * @param <I1>     the first input value type
     * @param <I2>     the second input value type
     * @param <I3>     the third input value type
     * @param <I4>     the fourth input value type
     * @param <O>      the output type
     * @return an {@link ObservableValue} that on each update of any source published the result of the combiner applied
     * with the latest values of the other input
     */
    public static <I1, I2, I3, I4, O> ObservableValue<O> combineLatest(Observable<I1> source1, Observable<I2> source2,
                                                                       Observable<I3> source3, Observable<I4> source4,
                                                                       Function4<I1, I2, I3, I4, O> combiner) {
        return CombineLatestOperators.combineLatest(source1, source2, source3, source4, combiner);
    }

    /**
     * Produces an {@link ObservableValue} that on each update of any source {@link ObservableValue} publishes the
     * result of the combiner applied with the latest values of the other inputs.
     *
     * @param source1  the first input {@link ObservableValue}
     * @param source2  the second input {@link ObservableValue}
     * @param source3  the third input {@link ObservableValue}
     * @param source4  the fourth input {@link ObservableValue}
     * @param source5  the fifth input {@link ObservableValue}
     * @param combiner the combining function that will produce the result
     * @param <I1>     the first input value type
     * @param <I2>     the second input value type
     * @param <I3>     the third input value type
     * @param <I4>     the fourth input value type
     * @param <I5>     the fifth input value type
     * @param <O>      the output type
     * @return an {@link ObservableValue} that on each update of any source published the result of the combiner applied
     * with the latest values of the other input
     */
    public static <I1, I2, I3, I4, I5, O> ObservableValue<O> combineLatest(Observable<I1> source1, Observable<I2> source2,
                                                                           Observable<I3> source3, Observable<I4> source4,
                                                                           Observable<I5> source5,
                                                                           Function5<I1, I2, I3, I4, I5, O> combiner) {
        return CombineLatestOperators.combineLatest(source1, source2, source3, source4, source5, combiner);
    }

    /**
     * Sets a static, framework-wide uncaught exception handler. It is called in the following cases:
     * <ul>
     * <li>If an observer onNext/onException throws, with an {@link UpdateDeliveryException}</li>
     * <li>If an observer does not implement onException and an exception would be delivered, by
     * an {@link UnhandledException}</li>
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

    /**
     * Creates an observable value that emits the actual time every given period.
     *
     * @param period the period in the given unit, when to emit
     * @param unit   the unit for the period
     * @return a periodically emitting obersvable value
     * @throws NullPointerException if the unit is {@code null}
     */
    public static ObservableValue<Instant> periodicEvery(long period, TimeUnit unit) {
        return new PeriodicObservableValue(period, unit);
    }

}
