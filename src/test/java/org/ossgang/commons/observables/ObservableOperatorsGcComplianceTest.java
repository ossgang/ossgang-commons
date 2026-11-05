package org.ossgang.commons.observables;

import static java.time.Duration.ofMillis;
import static java.time.Duration.ofSeconds;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.params.provider.Arguments.arguments;
import static org.ossgang.commons.GcTests.forceGc;
import static org.ossgang.commons.GcTests.wasGarbageCollected;

import java.lang.ref.Reference;
import java.lang.ref.WeakReference;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.ossgang.commons.awaitables.Await;
import org.ossgang.commons.monads.Maybe;
import org.ossgang.commons.properties.Properties;
import org.ossgang.commons.properties.Property;

public class ObservableOperatorsGcComplianceTest {

    static Stream<Arguments> operators() {
        return Stream.of( //
                arguments("Generic derive", (Function<ObservableValue<Object>, ObservableValue<Object>>) source -> source.derive(Optional::of)), //
                arguments("Map", (Function<ObservableValue<Object>, ObservableValue<Object>>) source -> source.map(Function.identity())), //
                arguments("Filter", (Function<ObservableValue<Object>, ObservableValue<Object>>) source -> source.filter(any -> true)) //
        );
    }

    private CompletableFuture<Object> result;

    @BeforeEach
    public void setUp() {
        result = new CompletableFuture<>();
    }

    private void setResult(Object obj) {
        result.complete(obj);
    }

    private Object getResult() {
        return Maybe.attempt(() -> result.get(5, TimeUnit.SECONDS)).value();
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("operators")
    public void whileSubscribed_holdingSource_shouldPreventGc(String operatorName, Function<ObservableValue<Object>, ObservableValue<Object>> operator) {
        Dispatcher<Object> source = Observables.dispatcher();
        ObservableValue<Object> operatorStep = operator.apply(source);
        Subscription subscription = operatorStep.subscribe(this::setResult);

        WeakReference<?> operatorStepWeak = new WeakReference<>(operatorStep);
        operatorStep = null;
        WeakReference<?> subscriptionWeak = new WeakReference<>(subscription);
        subscription = null;

        forceGc();

        source.dispatchValue(1);
        assertThat(getResult()).as("Value flow should be kept").isEqualTo(1);

        assertThat(operatorStepWeak.get()).as("Referencing source should keep operator reachable").isNotNull();
        assertThat(subscriptionWeak.get()).as("Referencing source should keep subscription reachable").isNotNull();

        // keep strongly reachable until here, the JIT may otherwise consider the local variable dead after its last use
        Reference.reachabilityFence(source);
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("operators")
    public void whileSubscribed_holdingOperator_shouldPreventGc(String operatorName, Function<ObservableValue<Object>, ObservableValue<Object>> operator) {
        Dispatcher<Object> source = Observables.dispatcher();
        ObservableValue<Object> operatorStep = operator.apply(source);
        Subscription subscription = operatorStep.subscribe(this::setResult);

        WeakReference<Dispatcher<Object>> sourceWeak = new WeakReference<>(source);
        source = null;
        WeakReference<?> subscriptionWeak = new WeakReference<>(subscription);
        subscription = null;

        forceGc();

        assertThat(sourceWeak.get()).as("Holding operator should keep source reachable").isNotNull();
        sourceWeak.get().dispatchValue(1);
        assertThat(getResult()).as("Value flow should be kept").isEqualTo(1);

        forceGc();

        assertThat(sourceWeak.get()).as("Holding operator should keep source reachable").isNotNull();
        assertThat(subscriptionWeak.get()).as("Holding operator should keep subscription reachable").isNotNull();

        // keep strongly reachable until here, the JIT may otherwise consider the local variable dead after its last use
        Reference.reachabilityFence(operatorStep);
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("operators")
    public void holdingSubscription_shouldPreventGc(String operatorName, Function<ObservableValue<Object>, ObservableValue<Object>> operator) {
        Dispatcher<Object> source = Observables.dispatcher();
        ObservableValue<Object> operatorStep = operator.apply(source);
        Subscription subscription = operatorStep.subscribe(this::setResult);

        WeakReference<Dispatcher<Object>> sourceWeak = new WeakReference<>(source);
        source = null;
        WeakReference<?> operatorStepWeak = new WeakReference<>(operatorStep);
        operatorStep = null;

        forceGc();

        assertThat(sourceWeak.get()).as("Holding subscription should keep source reachable").isNotNull();
        sourceWeak.get().dispatchValue(1);
        assertThat(getResult()).as("Value flow should be kept").isEqualTo(1);

        forceGc();

        assertThat(sourceWeak.get()).as("Holding subscription should keep source reachable").isNotNull();
        assertThat(operatorStepWeak.get()).as("Holding subscription should keep operator reachable").isNotNull();

        // keep strongly reachable until here, the JIT may otherwise consider the local variable dead after its last use
        Reference.reachabilityFence(subscription);
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("operators")
    public void withoutSubscription_holdingSource_shouldGcOperator(String operatorName, Function<ObservableValue<Object>, ObservableValue<Object>> operator) {
        Dispatcher<Object> source = Observables.dispatcher();
        ObservableValue<Object> operatorStep = operator.apply(source);

        WeakReference<?> operatorStepWeak = new WeakReference<>(operatorStep);
        operatorStep = null;

        forceGc();

        assertThat(operatorStepWeak.get()).as("Referencing source without subscriber should GC operator").isNull();

        // keep strongly reachable until here, the JIT may otherwise consider the local variable dead after its last use
        Reference.reachabilityFence(source);
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("operators")
    public void withoutSubscription_holdingOperator_shouldPreventGcOfSource(String operatorName, Function<ObservableValue<Object>, ObservableValue<Object>> operator) {
        Dispatcher<Object> source = Observables.dispatcher();
        ObservableValue<Object> operatorStep = operator.apply(source);

        WeakReference<Dispatcher<Object>> sourceWeak = new WeakReference<>(source);
        source = null;

        forceGc();

        assertThat(sourceWeak.get()).as("Holding operator without subscriber should keep source reachable").isNotNull();

        // keep strongly reachable until here, the JIT may otherwise consider the local variable dead after its last use
        Reference.reachabilityFence(operatorStep);
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("operators")
    public void withoutSubscription_withMultipleDerivationsWhileHoldingSource_shouldGcDerivations(String operatorName, Function<ObservableValue<Object>, ObservableValue<Object>> operator) throws Exception {
        Property<Object> source = Properties.property("42");
        ObservableValue<Object> operatorStep1 = operator.apply(source);
        ObservableValue<Object> operatorStep2 = operator.apply(operatorStep1);

        WeakReference<?> operatorStep1Weak = new WeakReference<>(operatorStep1);
        operatorStep1 = null;
        WeakReference<?> operatorStep2Weak = new WeakReference<>(operatorStep2);
        operatorStep2 = null;

        Await.await(() -> wasGarbageCollected(operatorStep1Weak)).withRetryInterval(ofMillis(10))
                .withErrorMessage("Holding source without subscriber should keep GC dangling operators")
                .atMost(ofSeconds(5));

        Await.await(() -> wasGarbageCollected(operatorStep2Weak)).withRetryInterval(ofMillis(10))
                .withErrorMessage("Holding source without subscriber should keep GC dangling operators")
                .atMost(ofSeconds(5));

        // keep strongly reachable until here, the JIT may otherwise consider the local variable dead after its last use
        Reference.reachabilityFence(source);
    }
}
