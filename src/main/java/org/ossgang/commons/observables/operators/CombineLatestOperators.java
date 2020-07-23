package org.ossgang.commons.observables.operators;

import org.ossgang.commons.monads.Function3;
import org.ossgang.commons.monads.Function4;
import org.ossgang.commons.monads.Function5;
import org.ossgang.commons.observables.Observable;
import org.ossgang.commons.observables.ObservableValue;

import java.util.*;
import java.util.function.BiFunction;
import java.util.function.Function;

import static java.util.stream.Collectors.toList;
import static org.ossgang.commons.observables.operators.DerivedObservableValue.derive;
import static org.ossgang.commons.observables.operators.OperatorUtils.*;

public final class CombineLatestOperators {

    /**
     * @see org.ossgang.commons.observables.Observables#combineLatestObjects(Map, Function)
     */
    public static <K, O> ObservableValue<O> combineLatestObjects(Map<K, ? extends Observable<?>> sourcesMap,
                                                                 Function<Map<K, Object>, O> combiner) {
        Map<K, Observable<Object>> sourcesMapObject = (Map<K, Observable<Object>>) sourcesMap;
        Map<K, Object> valueMap = new HashMap<>();
        Set<K> keys = new HashSet<>(sourcesMapObject.keySet());
        return derive(sourcesMapObject, (k, v) -> {
            synchronized (valueMap) {
                valueMap.put(k, v);
                if (valueMap.keySet().containsAll(keys)) {
                    return Optional.of(combiner.apply(new HashMap<>(valueMap)));
                }
                return Optional.empty();
            }
        });
    }

    /**
     * @see org.ossgang.commons.observables.Observables#combineLatestObjects(List, Function)
     */
    public static <O> ObservableValue<O> combineLatestObjects(List<? extends Observable<?>> sources,
                                                              Function<List<Object>, O> combiner) {
        return combineLatestObjects(toIndexMap(sources), idxMap -> combiner.apply(fromIndexMap(idxMap)));
    }

    /**
     * @see org.ossgang.commons.observables.Observables#combineLatestObjects(Map)
     */
    public static <K> ObservableValue<Map<K, Object>> combineLatestObjects(Map<K, ? extends Observable<?>> sourcesMap) {
        return combineLatestObjects(sourcesMap, Function.identity());
    }

    /**
     * @see org.ossgang.commons.observables.Observables#combineLatestObjects(List)
     */
    public static ObservableValue<List<Object>> combineLatestObjects(List<? extends Observable<?>> sources) {
        return combineLatestObjects(sources, Function.identity());
    }

    /**
     * @see org.ossgang.commons.observables.Observables#combineLatest(Map, Function)
     */
    @SuppressWarnings("unchecked")
    public static <K, I, O> ObservableValue<O> combineLatest(Map<K, ? extends Observable<I>> sourcesMap,
                                                             Function<Map<K, I>, O> combiner) {
        Function<Map<K, Object>, Map<K, I>> typedTranslator = values -> applyToMapValues(values, v -> (I) v);
        return combineLatestObjects(sourcesMap, typedTranslator.andThen(combiner));
    }

    /**
     * @see org.ossgang.commons.observables.Observables#combineLatest(List, Function)
     */
    @SuppressWarnings("unchecked")
    public static <I, O> ObservableValue<O> combineLatest(List<? extends Observable<I>> sources,
                                                          Function<List<I>, O> combiner) {
        Function<List<Object>, List<I>> typedTranslator = values -> values.stream().map(v -> (I) v).collect(toList());
        return combineLatestObjects(toIndexMap(sources), idxMap -> typedTranslator.andThen(combiner).apply(fromIndexMap(idxMap)));
    }

    /**
     * @see org.ossgang.commons.observables.Observables#combineLatest(Map)
     */
    @SuppressWarnings("unchecked")
    public static <K, I> ObservableValue<Map<K, I>> combineLatest(Map<K, ? extends Observable<I>> sourcesMap) {
        Function<Map<K, Object>, Map<K, I>> typedTranslator = values -> applyToMapValues(values, v -> (I) v);
        return combineLatestObjects(sourcesMap, typedTranslator);
    }

    /**
     * @see org.ossgang.commons.observables.Observables#combineLatest(List)
     */
    @SuppressWarnings("unchecked")
    public static <I> ObservableValue<List<I>> combineLatest(List<? extends Observable<I>> sources) {
        Function<List<Object>, List<I>> typedTranslator = values -> values.stream().map(v -> (I) v).collect(toList());
        return combineLatestObjects(sources, typedTranslator);
    }

    /**
     * @see org.ossgang.commons.observables.Observables#combineLatest(Observable, Observable, BiFunction)
     */
    @SuppressWarnings("unchecked")
    public static <I1, I2, O> ObservableValue<O> combineLatest(Observable<I1> source1, Observable<I2> source2,
                                                               BiFunction<I1, I2, O> combiner) {
        List<Observable<?>> sourcesListObject = Arrays.asList(source1, source2);
        Function<List<Object>, O> combinerObject = values -> {
            I1 sourceValue1 = (I1) values.get(0);
            I2 sourceValue2 = (I2) values.get(1);
            return combiner.apply(sourceValue1, sourceValue2);
        };
        return combineLatestObjects(sourcesListObject, combinerObject);
    }

    /**
     * @see org.ossgang.commons.observables.Observables#combineLatest(Observable, Observable, Observable, Function3)
     */
    @SuppressWarnings("unchecked")
    public static <I1, I2, I3, O> ObservableValue<O> combineLatest(Observable<I1> source1, Observable<I2> source2,
                                                                   Observable<I3> source3,
                                                                   Function3<I1, I2, I3, O> combiner) {
        List<Observable<?>> sourcesListObject = Arrays.asList(source1, source2, source3);
        Function<List<Object>, O> combinerObject = values -> {
            I1 sourceValue1 = (I1) values.get(0);
            I2 sourceValue2 = (I2) values.get(1);
            I3 sourceValue3 = (I3) values.get(2);
            return combiner.apply(sourceValue1, sourceValue2, sourceValue3);
        };
        return combineLatestObjects(sourcesListObject, combinerObject);
    }

    /**
     * @see org.ossgang.commons.observables.Observables#combineLatest(Observable, Observable, Observable, Observable, Function4)
     */
    @SuppressWarnings("unchecked")
    public static <I1, I2, I3, I4, O> ObservableValue<O> combineLatest(Observable<I1> source1, Observable<I2> source2,
                                                                       Observable<I3> source3, Observable<I4> source4,
                                                                       Function4<I1, I2, I3, I4, O> combiner) {
        List<Observable<?>> sourcesListObject = Arrays.asList(source1, source2, source3, source4);
        Function<List<Object>, O> combinerObject = values -> {
            I1 sourceValue1 = (I1) values.get(0);
            I2 sourceValue2 = (I2) values.get(1);
            I3 sourceValue3 = (I3) values.get(2);
            I4 sourceValue4 = (I4) values.get(3);
            return combiner.apply(sourceValue1, sourceValue2, sourceValue3, sourceValue4);
        };
        return combineLatestObjects(sourcesListObject, combinerObject);
    }

    /**
     * @see org.ossgang.commons.observables.Observables#combineLatest(Observable, Observable, Observable, Observable, Observable, Function5)
     */
    @SuppressWarnings("unchecked")
    public static <I1, I2, I3, I4, I5, O> ObservableValue<O> combineLatest(Observable<I1> source1, Observable<I2> source2,
                                                                           Observable<I3> source3, Observable<I4> source4,
                                                                           Observable<I5> source5,
                                                                           Function5<I1, I2, I3, I4, I5, O> combiner) {
        List<Observable<?>> sourcesListObject = Arrays.asList(source1, source2, source3, source4, source5);
        Function<List<Object>, O> combinerObject = values -> {
            I1 sourceValue1 = (I1) values.get(0);
            I2 sourceValue2 = (I2) values.get(1);
            I3 sourceValue3 = (I3) values.get(2);
            I4 sourceValue4 = (I4) values.get(3);
            I5 sourceValue5 = (I5) values.get(4);
            return combiner.apply(sourceValue1, sourceValue2, sourceValue3, sourceValue4, sourceValue5);
        };
        return combineLatestObjects(sourcesListObject, combinerObject);
    }

    private CombineLatestOperators() {
        throw new UnsupportedOperationException("static only");
    }
}
