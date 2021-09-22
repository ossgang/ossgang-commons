/*
 *
 * This file is part of ossgang-commons.
 *
 * Copyright (c) 2008-2020, CERN. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.ossgang.commons.observables;

import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

import org.ossgang.commons.monads.*;
import org.ossgang.commons.observables.exceptions.UnhandledException;
import org.ossgang.commons.observables.exceptions.UpdateDeliveryException;
import org.ossgang.commons.observables.operators.*;
import org.ossgang.commons.observables.operators.connectors.ConnectorObservableValue;
import org.ossgang.commons.observables.operators.connectors.ConnectorObservables;
import org.ossgang.commons.observables.operators.connectors.DynamicConnectorObservableValue;
import org.ossgang.commons.utils.NamedDaemonThreadFactory;

import static org.ossgang.commons.utils.NamedDaemonThreadFactory.daemonThreadFactoryWithPrefix;

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
     * Creates a {@link ConnectorObservableValue} that on each connection (call to {@link ConnectorObservableValue#connect()})
     * will subscribe to the upstream {@link ObservableValue} produced by the specified {@link Supplier}.
     * NOTE: The {@link ConnectorObservableValue} will NOT connect immediately, a call to {@link ConnectorObservableValue#connect()}
     * is needed afterwards.
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
     * Creates a {@link ConnectorObservableValue} that will be connected to the specified upstream {@link ObservableValue} when
     * {@link ConnectorObservableValue#connect()} is called.
     * NOTE: The {@link ConnectorObservableValue} will NOT connect immediately, a call to {@link ConnectorObservableValue#connect()}
     * is needed afterwards.
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
        return CombinationOperators.zip(sourcesMap, combiner);
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
        return CombinationOperators.zip(sources, combiner);
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
        return CombinationOperators.zip(sourcesMap);
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
        return CombinationOperators.zip(sources);
    }

    /**
     * Produces an {@link ObservableValue} that zips the values of the provided {@link ObservableValue}.
     * <br>
     * Note: this operator will wait until all the source {@link ObservableValue}s have a value to match.
     * <br>
     * This method does not restrict the type of the input values and, therefore, produces {@link Object}s as result.
     * To be used in special cases when the typed version of the combineLatest operator cannot be used!
     *
     * @param sourcesMap the input {@link Observable}s indexed
     * @param combiner   the combining function that will produce the result
     * @param <K>        the key for each input {@link Observable} that is used for indexing
     * @param <O>        the output type
     * @return an {@link ObservableValue} that on each update of any source {@link Observable} publishes the
     * result of combiner applied with the latest values of the other inputs.
     */
    public static <K, O> ObservableValue<O> zipObjects(Map<K, ? extends Observable<?>> sourcesMap,
                                                       Function<Map<K, Object>, O> combiner) {
        return CombinationOperators.zipObjects(sourcesMap, combiner);
    }

    /**
     * Produces an {@link ObservableValue} that zips the values of the provided {@link ObservableValue}.
     * <br>
     * Note: this operator will wait until all the source {@link ObservableValue}s have a value to match.
     * <br>
     * This method does not restrict the type of the input values and, therefore, produces {@link Object}s as result.
     * To be used in special cases when the typed version of the combineLatest operator cannot be used!
     *
     * @param sources  the input {@link Observable}s
     * @param combiner the combining function that will produce the result
     * @param <O>      the output type
     * @return an {@link ObservableValue} that on each update of any source publishes the result of the
     * combiner applied with the latest values of the other inputs.
     */
    public static <O> ObservableValue<O> zipObjects(List<? extends Observable<?>> sources,
                                                    Function<List<Object>, O> combiner) {
        return CombinationOperators.zipObjects(sources, combiner);
    }

    /**
     * Produces an {@link ObservableValue} that zips the values of the provided {@link ObservableValue}.
     * <br>
     * Note: this operator will wait until all the source {@link ObservableValue}s have a value to match.
     * <br>
     * This method does not restrict the type of the input values and, therefore, produces {@link Object}s as result.
     * To be used in special cases when the typed version of the combineLatest operator cannot be used!
     *
     * @param sourcesMap the input {@link Observable}s indexed
     * @param <K>        the key for each input {@link Observable} that is used for indexing
     * @return an {@link ObservableValue} that on each update of any source {@link Observable} publishes a
     * {@link Map} with the values of each corresponding source {@link Observable}.
     */
    public static <K> ObservableValue<Map<K, Object>> zipObjects(Map<K, ? extends Observable<?>> sourcesMap) {
        return CombinationOperators.zipObjects(sourcesMap);
    }

    /**
     * Produces an {@link ObservableValue} that zips the values of the provided {@link ObservableValue}.
     * <br>
     * Note: this operator will wait until all the source {@link ObservableValue}s have a value to match.
     * <br>
     * This method does not restrict the type of the input values and, therefore, produces {@link Object}s as result.
     * To be used in special cases when the typed version of the combineLatest operator cannot be used!
     *
     * @param sources the input {@link Observable}s
     * @return an {@link ObservableValue} that on each update of any source {@link Observable} publishes the
     * a {@link List} containing the values of each {@link Observable}
     */
    public static ObservableValue<List<Object>> zipObjects(List<? extends Observable<?>> sources) {
        return CombinationOperators.zipObjects(sources);
    }

    /**
     * Produces an {@link ObservableValue} that zips the values of the provided {@link ObservableValue}.
     * <br>
     * Note: this operator will wait until all the source {@link ObservableValue}s have a value to match.
     *
     * @param source1  the first input {@link Observable}
     * @param source2  the second input {@link Observable}
     * @param combiner the combining function that will produce the result
     * @param <I1>     the first input value type
     * @param <I2>     the second input value type
     * @param <O>      the output type
     * @return an {@link ObservableValue} that on each update of any source published the result of the combiner applied
     * with the latest values of the other input
     */
    public static <I1, I2, O> ObservableValue<O> zip(Observable<I1> source1, Observable<I2> source2,
                                                     BiFunction<I1, I2, O> combiner) {
        return CombinationOperators.zip(source1, source2, combiner);
    }

    /**
     * Produces an {@link ObservableValue} that zips the values of the provided {@link ObservableValue}.
     * <br>
     * Note: this operator will wait until all the source {@link ObservableValue}s have a value to match.
     *
     * @param source1  the first input {@link Observable}
     * @param source2  the second input {@link Observable}
     * @param source3  the third input {@link Observable}
     * @param combiner the combining function that will produce the result
     * @param <I1>     the first input value type
     * @param <I2>     the second input value type
     * @param <I3>     the third input value type
     * @param <O>      the output type
     * @return an {@link ObservableValue} that on each update of any source published the result of the combiner applied
     * with the latest values of the other input
     */
    public static <I1, I2, I3, O> ObservableValue<O> zip(Observable<I1> source1, Observable<I2> source2,
                                                         Observable<I3> source3,
                                                         Function3<I1, I2, I3, O> combiner) {
        return CombinationOperators.zip(source1, source2, source3, combiner);
    }

    /**
     * Produces an {@link ObservableValue} that zips the values of the provided {@link ObservableValue}.
     * <br>
     * Note: this operator will wait until all the source {@link ObservableValue}s have a value to match.
     *
     * @param source1  the first input {@link Observable}
     * @param source2  the second input {@link Observable}
     * @param source3  the third input {@link Observable}
     * @param source4  the fourth input {@link Observable}
     * @param combiner the combining function that will produce the result
     * @param <I1>     the first input value type
     * @param <I2>     the second input value type
     * @param <I3>     the third input value type
     * @param <I4>     the fourth input value type
     * @param <O>      the output type
     * @return an {@link ObservableValue} that on each update of any source published the result of the combiner applied
     * with the latest values of the other input
     */
    public static <I1, I2, I3, I4, O> ObservableValue<O> zip(Observable<I1> source1, Observable<I2> source2,
                                                             Observable<I3> source3, Observable<I4> source4,
                                                             Function4<I1, I2, I3, I4, O> combiner) {
        return CombinationOperators.zip(source1, source2, source3, source4, combiner);
    }

    /**
     * Produces an {@link ObservableValue} that zips the values of the provided {@link ObservableValue}.
     * <br>
     * Note: this operator will wait until all the source {@link ObservableValue}s have a value to match.
     *
     * @param source1  the first input {@link Observable}
     * @param source2  the second input {@link Observable}
     * @param source3  the third input {@link Observable}
     * @param source4  the fourth input {@link Observable}
     * @param source5  the fifth input {@link Observable}
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
    public static <I1, I2, I3, I4, I5, O> ObservableValue<O> zip(Observable<I1> source1, Observable<I2> source2,
                                                                 Observable<I3> source3, Observable<I4> source4,
                                                                 Observable<I5> source5,
                                                                 Function5<I1, I2, I3, I4, I5, O> combiner) {
        return CombinationOperators.zip(source1, source2, source3, source4, source5, combiner);
    }

    /**
     * Produces an {@link ObservableValue} that on each update of any source {@link Observable} publishes the
     * result of the combiner applied with the latest values of the other inputs.
     * The {@link Map}s are used to avoid matching values by index.
     *
     * @param sourcesMap the input {@link Observable}s indexed
     * @param combiner   the combining function that will produce the result
     * @param <K>        the key for each input {@link Observable} that is used for indexing
     * @param <I>        the input type
     * @param <O>        the output type
     * @return an {@link ObservableValue} that on each update of any source {@link Observable} publishes the
     * result of combiner applied with the latest values of the other inputs.
     */
    public static <K, I, O> ObservableValue<O> combineLatest(Map<K, ? extends Observable<I>> sourcesMap,
                                                             Function<Map<K, I>, O> combiner) {
        return CombinationOperators.combineLatest(sourcesMap, combiner);
    }

    /**
     * Produces an {@link ObservableValue} that on each update of any source {@link Observable} publishes the
     * result of the combiner applied with the latest values of the other inputs.
     * The order of the input values of the combiner is the same as the order of the provided source
     * {@link Observable}s
     *
     * @param sources  the input {@link Observable}s
     * @param combiner the combining function that will produce the result
     * @param <I>      the input type
     * @param <O>      the output type
     * @return an {@link ObservableValue} that on each update of any source publishes the result of the
     * combiner applied with the latest values of the other inputs.
     */
    public static <I, O> ObservableValue<O> combineLatest(List<? extends Observable<I>> sources,
                                                          Function<List<I>, O> combiner) {
        return CombinationOperators.combineLatest(sources, combiner);
    }

    /**
     * Produces an {@link ObservableValue} that on each update of any source {@link Observable} publishes a
     * {@link Map} with the values of each corresponding source {@link Observable}.
     *
     * @param sourcesMap the input {@link Observable}s indexed
     * @param <K>        the key for each input {@link Observable} that is used for indexing
     * @param <I>        the input type
     * @return an {@link ObservableValue} that on each update of any source {@link Observable} publishes a
     * {@link Map} with the values of each corresponding source {@link Observable}.
     */
    public static <K, I> ObservableValue<Map<K, I>> combineLatest(Map<K, ? extends Observable<I>> sourcesMap) {
        return CombinationOperators.combineLatest(sourcesMap);
    }

    /**
     * Produces an {@link ObservableValue} that on each update of any source {@link Observable} publishes the
     * a {@link List} containing the values of each {@link Observable}
     *
     * @param sources the input {@link Observable}s
     * @param <I>     the input type
     * @return an {@link ObservableValue} that on each update of any source {@link Observable} publishes the
     * a {@link List} containing the values of each {@link Observable}
     */
    public static <I> ObservableValue<List<I>> combineLatest(List<? extends Observable<I>> sources) {
        return CombinationOperators.combineLatest(sources);
    }

    /**
     * Produces an {@link ObservableValue} that on each update of any source {@link Observable} publishes the
     * result of the combiner applied with the latest values of the other inputs.
     * The {@link Map}s are used to avoid matching values by index.
     * This method does not restrict the type of the input values and, therefore, produces {@link Object}s as result.
     * To be used in special cases when the typed version of the combineLatest operator cannot be used!
     *
     * @param sourcesMap the input {@link Observable}s indexed
     * @param combiner   the combining function that will produce the result
     * @param <K>        the key for each input {@link Observable} that is used for indexing
     * @param <O>        the output type
     * @return an {@link ObservableValue} that on each update of any source {@link Observable} publishes the
     * result of combiner applied with the latest values of the other inputs.
     */
    public static <K, O> ObservableValue<O> combineLatestObjects(Map<K, ? extends Observable<?>> sourcesMap,
                                                                 Function<Map<K, Object>, O> combiner) {
        return CombinationOperators.combineLatestObjects(sourcesMap, combiner);
    }

    /**
     * Produces an {@link ObservableValue} that on each update of any source {@link Observable} publishes the
     * result of the combiner applied with the latest values of the other inputs.
     * The order of the input values of the combiner is the same as the order of the provided source
     * {@link Observable}s.
     * This method does not restrict the type of the input values and, therefore, produces {@link Object}s as result.
     * To be used in special cases when the typed version of the combineLatest operator cannot be used!
     *
     * @param sources  the input {@link Observable}s
     * @param combiner the combining function that will produce the result
     * @param <O>      the output type
     * @return an {@link ObservableValue} that on each update of any source publishes the result of the
     * combiner applied with the latest values of the other inputs.
     */
    public static <O> ObservableValue<O> combineLatestObjects(List<? extends Observable<?>> sources,
                                                              Function<List<Object>, O> combiner) {
        return CombinationOperators.combineLatestObjects(sources, combiner);
    }

    /**
     * Produces an {@link ObservableValue} that on each update of any source {@link Observable} publishes a
     * {@link Map} with the values of each corresponding source {@link Observable}.
     * This method does not restrict the type of the input values and, therefore, produces {@link Object}s as result.
     * To be used in special cases when the typed version of the combineLatest operator cannot be used!
     *
     * @param sourcesMap the input {@link Observable}s indexed
     * @param <K>        the key for each input {@link Observable} that is used for indexing
     * @return an {@link ObservableValue} that on each update of any source {@link Observable} publishes a
     * {@link Map} with the values of each corresponding source {@link Observable}.
     */
    public static <K> ObservableValue<Map<K, Object>> combineLatestObjects(Map<K, ? extends Observable<?>> sourcesMap) {
        return CombinationOperators.combineLatestObjects(sourcesMap);
    }

    /**
     * Produces an {@link ObservableValue} that on each update of any source {@link Observable} publishes the
     * a {@link List} containing the values of each {@link Observable}.
     * This method does not restrict the type of the input values and, therefore, produces {@link Object}s as result.
     * To be used in special cases when the typed version of the combineLatest operator cannot be used!
     *
     * @param sources the input {@link Observable}s
     * @return an {@link ObservableValue} that on each update of any source {@link Observable} publishes the
     * a {@link List} containing the values of each {@link Observable}
     */
    public static ObservableValue<List<Object>> combineLatestObjects(List<? extends Observable<?>> sources) {
        return CombinationOperators.combineLatestObjects(sources);
    }

    /**
     * Produces an {@link ObservableValue} that on each update of any source {@link Observable} publishes the
     * result of the combiner applied with the latest values of the other input.
     *
     * @param source1  the first input {@link Observable}
     * @param source2  the second input {@link Observable}
     * @param combiner the combining function that will produce the result
     * @param <I1>     the first input value type
     * @param <I2>     the second input value type
     * @param <O>      the output type
     * @return an {@link ObservableValue} that on each update of any source published the result of the combiner applied
     * with the latest values of the other input
     */
    public static <I1, I2, O> ObservableValue<O> combineLatest(Observable<I1> source1, Observable<I2> source2,
                                                               BiFunction<I1, I2, O> combiner) {
        return CombinationOperators.combineLatest(source1, source2, combiner);
    }

    /**
     * Produces an {@link ObservableValue} that on each update of any source {@link Observable} publishes the
     * result of the combiner applied with the latest values of the other inputs.
     *
     * @param source1  the first input {@link Observable}
     * @param source2  the second input {@link Observable}
     * @param source3  the third input {@link Observable}
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
        return CombinationOperators.combineLatest(source1, source2, source3, combiner);
    }

    /**
     * Produces an {@link ObservableValue} that on each update of any source {@link Observable} publishes the
     * result of the combiner applied with the latest values of the other inputs.
     *
     * @param source1  the first input {@link Observable}
     * @param source2  the second input {@link Observable}
     * @param source3  the third input {@link Observable}
     * @param source4  the fourth input {@link Observable}
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
        return CombinationOperators.combineLatest(source1, source2, source3, source4, combiner);
    }

    /**
     * Produces an {@link ObservableValue} that on each update of any source {@link Observable} publishes the
     * result of the combiner applied with the latest values of the other inputs.
     *
     * @param source1  the first input {@link Observable}
     * @param source2  the second input {@link Observable}
     * @param source3  the third input {@link Observable}
     * @param source4  the fourth input {@link Observable}
     * @param source5  the fifth input {@link Observable}
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
        return CombinationOperators.combineLatest(source1, source2, source3, source4, source5, combiner);
    }

    /**
     * Produces an {@link ObservableValue} that emits any update of any source {@link Observable}.
     *
     * @param sources the source observables
     * @param <O>     the output type
     * @return the merged observable
     */
    public static <O, I extends O> ObservableValue<O> merge(Collection<? extends Observable<I>> sources) {
        return CombinationOperators.merge(sources);
    }

    /**
     * Produces an {@link ObservableValue} that emits any update of any source {@link Observable}.
     *
     * @param sources the source observables
     * @param <O>     the output type
     * @return the merged observable
     */
    @SafeVarargs
    public static <O, I extends O> ObservableValue<O> merge(Observable<I>... sources) {
        return CombinationOperators.merge(Arrays.asList(sources));
    }

    /**
     * Subscribe to the updates of the values of each source {@link Observable}. The provided consumer will be called
     * according to the policy specified via the provided {@link ValueCombinationPolicy}
     *
     * @param source1             the first input {@link Observable}
     * @param source2             the second input {@link Observable}
     * @param consumer            the consumer of the values of the input {@link Observable}s
     * @param combinationPolicy   the combination policy of the values of the input {@link Observable}s
     * @param subscriptionOptions the subscription options to pass to each input {@link Observable}s
     * @param <I1>                the first input value type
     * @param <I2>                the second input value type
     * @return a Subscription object, which can be used to unsubscribe at a later point
     */
    public static <I1, I2> Subscription subscribeValues(Observable<I1> source1, Observable<I2> source2,
                                                        BiConsumer<I1, I2> consumer,
                                                        ValueCombinationPolicy combinationPolicy, SubscriptionOption... subscriptionOptions) {
        return SubscribeValuesOperators.subscribeValues(source1, source2,
                consumer, combinationPolicy, subscriptionOptions);
    }

    /**
     * Subscribe to the updates of the values of each source {@link Observable}. The provided consumer will be called
     * according to the policy specified via the provided {@link ValueCombinationPolicy}
     *
     * @param source1             the first input {@link Observable}
     * @param source2             the second input {@link Observable}
     * @param source3             the third input {@link Observable}
     * @param consumer            the consumer of the values of the input {@link Observable}s
     * @param combinationPolicy   the combination policy of the values of the input {@link Observable}s
     * @param subscriptionOptions the subscription options to pass to each input {@link Observable}s
     * @param <I1>                the first input value type
     * @param <I2>                the second input value type
     * @param <I3>                the third input value type
     * @return a Subscription object, which can be used to unsubscribe at a later point
     */
    public static <I1, I2, I3> Subscription subscribeValues(Observable<I1> source1, Observable<I2> source2, Observable<I3> source3,
                                                            Consumer3<I1, I2, I3> consumer,
                                                            ValueCombinationPolicy combinationPolicy, SubscriptionOption... subscriptionOptions) {
        return SubscribeValuesOperators.subscribeValues(source1, source2, source3,
                consumer, combinationPolicy, subscriptionOptions);
    }

    /**
     * Subscribe to the updates of the values of each source {@link Observable}. The provided consumer will be called
     * according to the policy specified via the provided {@link ValueCombinationPolicy}
     *
     * @param source1             the first input {@link Observable}
     * @param source2             the second input {@link Observable}
     * @param source3             the third input {@link Observable}
     * @param source4             the fourth input {@link Observable}
     * @param consumer            the consumer of the values of the input {@link Observable}s
     * @param combinationPolicy   the combination policy of the values of the input {@link Observable}s
     * @param subscriptionOptions the subscription options to pass to each input {@link Observable}s
     * @param <I1>                the first input value type
     * @param <I2>                the second input value type
     * @param <I3>                the third input value type
     * @param <I4>                the fourth input value type
     * @return a Subscription object, which can be used to unsubscribe at a later point
     */
    public static <I1, I2, I3, I4> Subscription subscribeValues(Observable<I1> source1, Observable<I2> source2,
                                                                Observable<I3> source3, Observable<I4> source4,
                                                                Consumer4<I1, I2, I3, I4> consumer,
                                                                ValueCombinationPolicy combinationPolicy, SubscriptionOption... subscriptionOptions) {
        return SubscribeValuesOperators.subscribeValues(source1, source2, source3, source4,
                consumer, combinationPolicy, subscriptionOptions);
    }

    /**
     * Subscribe to the updates of the values of each source {@link Observable}. The provided consumer will be called
     * according to the policy specified via the provided {@link ValueCombinationPolicy}
     *
     * @param source1             the first input {@link Observable}
     * @param source2             the second input {@link Observable}
     * @param source3             the third input {@link Observable}
     * @param source4             the fourth input {@link Observable}
     * @param source5             the fifth input {@link Observable}
     * @param consumer            the consumer of the values of the input {@link Observable}s
     * @param combinationPolicy   the combination policy of the values of the input {@link Observable}s
     * @param subscriptionOptions the subscription options to pass to each input {@link Observable}s
     * @param <I1>                the first input value type
     * @param <I2>                the second input value type
     * @param <I3>                the third input value type
     * @param <I4>                the fourth input value type
     * @param <I5>                the fifth input value type
     * @return a Subscription object, which can be used to unsubscribe at a later point
     */
    public static <I1, I2, I3, I4, I5> Subscription subscribeValues(Observable<I1> source1, Observable<I2> source2,
                                                                    Observable<I3> source3, Observable<I4> source4,
                                                                    Observable<I5> source5,
                                                                    Consumer5<I1, I2, I3, I4, I5> consumer,
                                                                    ValueCombinationPolicy combinationPolicy, SubscriptionOption... subscriptionOptions) {
        return SubscribeValuesOperators.subscribeValues(source1, source2, source3, source4, source5,
                consumer, combinationPolicy, subscriptionOptions);
    }

    /**
     * Creates an {@link ObservableValue} that only dispatch those items dispatched by the source {@link ObservableValue}
     * that are not followed by another item within the specified time window.
     * NOTE: if the source {@link ObservableValue} always dispatch items with a shorter rate then the time window, then
     * this debounced {@link ObservableValue} will never dispatch !
     *
     * @param source         Observable
     * @param debouncePeriod the time window for debouncing the dispatching of the values
     * @param <T>            the value type
     * @return the debounced ObservableValue
     */
    public static <T> ObservableValue<T> debounce(Observable<T> source, Duration debouncePeriod) {
        return new DebouncedObservableValue<>(source, debouncePeriod);
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
