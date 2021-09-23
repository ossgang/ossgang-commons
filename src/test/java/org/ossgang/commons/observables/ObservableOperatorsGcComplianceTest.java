package org.ossgang.commons.observables;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.ossgang.commons.monads.Maybe;
import org.ossgang.commons.properties.Properties;
import org.ossgang.commons.properties.Property;

import java.lang.ref.WeakReference;
import java.util.Arrays;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;

import static java.util.concurrent.TimeUnit.SECONDS;
import static java.util.function.Function.identity;
import static org.assertj.core.api.Assertions.assertThat;
import static org.ossgang.commons.GcTests.forceGc;

@RunWith(Parameterized.class)
public class ObservableOperatorsGcComplianceTest {

    @Parameterized.Parameters(name = "{0}")
    public static Iterable<Object[]> parameters() {
        /* Unfortunate JUnit parametrized API... */
        return Arrays.asList(new Object[][] { { "Generic derive",
                (Function<ObservableValue<Object>, ObservableValue<Object>>) source -> source.derive(Optional::of) },
                { "Map", (Function<ObservableValue<Object>, ObservableValue<Object>>) source -> source
                        .map(Function.identity()) }, { "Filter",
                (Function<ObservableValue<Object>, ObservableValue<Object>>) source -> source.filter(any -> true) } });
    }

    @Parameterized.Parameter(0)
    public String operatorName;

    @Parameterized.Parameter(1)
    public Function<ObservableValue<Object>, ObservableValue<Object>> operator;

    private CompletableFuture<Object> result;

    @Before
    public void setUp() {
        result = new CompletableFuture<>();
    }

    private void setResult(Object obj) {
        result.complete(obj);
    }

    private Object getResult() {
        return Maybe.attempt(() -> result.get(5, TimeUnit.SECONDS)).value();
    }

    @Test
    public void whileSubscribed_holdingSource_shouldPreventGc() {
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
    }

    @Test
    public void whileSubscribed_holdingOperator_shouldPreventGc() {
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
    }

    @Test
    public void holdingSubscription_shouldPreventGc() {
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
    }

    @Test
    public void withoutSubscriber_holdingSource_shouldGcOperator() {
        Dispatcher<Object> source = Observables.dispatcher();
        ObservableValue<Object> operatorStep = operator.apply(source);

        WeakReference<?> operatorStepWeak = new WeakReference<>(operatorStep);
        operatorStep = null;

        forceGc();

        assertThat(operatorStepWeak.get()).as("Referencing source without subscriber should GC operator").isNull();
    }

    @Test
    public void withoutSubscriber_holdingOperator_shouldPreventGcOfSource() {
        Dispatcher<Object> source = Observables.dispatcher();
        ObservableValue<Object> operatorStep = operator.apply(source);

        WeakReference<Dispatcher<Object>> sourceWeak = new WeakReference<>(source);
        source = null;

        forceGc();

        assertThat(sourceWeak.get()).as("Holding operator without subscriber should keep source reachable").isNotNull();
    }

    @Test
    public void gcWithMultipleDerivationsWhileNotSubscribedAndSourceReferenced_shouldGcDerivations() throws Exception {
        Property<String> property = Properties.property("42");
        ObservableValue<String> step1 = property.map(identity());
        ObservableValue<Integer> step2 = step1.map(Integer::parseInt);
        ObservableValue<Integer> step3 = step2.filter(any -> true);

        WeakReference<?> step2ref = new WeakReference<>(step2);
        step2 = null;
        WeakReference<?> step3ref = new WeakReference<>(step3);
        step3 = null;

        for (int i = 0; i < 10; i++) {
            forceGc();
            Thread.sleep(10); /* allow weak observer stale cleanup to happen */
        }

        assertThat(step3ref.get()).isNull();
        assertThat(step2ref.get()).isNull();
    }
}
