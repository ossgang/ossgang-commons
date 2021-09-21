package org.ossgang.commons.observables.operators;

import static java.lang.Integer.parseInt;
import static java.util.concurrent.TimeUnit.SECONDS;
import static java.util.function.Function.identity;
import static org.assertj.core.api.Assertions.assertThat;
import static org.ossgang.commons.GcTests.forceGc;
import static org.ossgang.commons.observables.operators.DerivedObservableValue.derive;

import java.lang.ref.WeakReference;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.Test;
import org.ossgang.commons.observables.ObservableValue;
import org.ossgang.commons.observables.Observer;
import org.ossgang.commons.observables.Subscription;
import org.ossgang.commons.observables.SubscriptionOption;
import org.ossgang.commons.properties.Properties;
import org.ossgang.commons.properties.Property;

public class DerivedObservableValueGcTest {
    private CompletableFuture<Integer> methodReferenceUpdateValue = new CompletableFuture<>();

    private void handleUpdate(int test) {
        methodReferenceUpdateValue.complete(test);
    }

    @Test
    public void gcWhileSubscribed_shouldNotPreventGcOfUpstream() throws Exception {
        WeakReference<Property<String>> property = gcWhileSubscribed_shouldPreventGcOfUpstream_subscribe();
        forceGc();
        Property<String> prop = property.get();
        assertThat(prop).isNull();
    }

    private WeakReference<Property<String>> gcWhileSubscribed_shouldPreventGcOfUpstream_subscribe() {
        Property<String> property = Properties.property("2");
        derive(property, v -> Optional.of(parseInt(v))).subscribe(this::handleUpdate);
        return new WeakReference<>(property);
    }

    @Test
    public void gcWhileNotSubscribed_shouldGc() {
        Property<String> property = Properties.property("2");
        WeakReference<ObservableValue<Integer>> derivedObservable = gcWhileNotSubscribed_derive(property);
        assertThat(derivedObservable.get()).isNotNull();
        forceGc();
        assertThat(derivedObservable.get()).isNull();
    }

    private WeakReference<ObservableValue<Integer>> gcWhileNotSubscribed_derive(ObservableValue<String> value) {
        return new WeakReference<>(derive(value, v -> Optional.of(parseInt(v))));
    }

    @Test
    public void gcWhileSubscribedButUpstreamSubscriptionKilled_shouldGc() throws Exception {
        Property<String> property = Properties.property("2");
        AtomicReference<Subscription> upstreamSubscription = new AtomicReference<>();
        ObservableValue<String> wrappedProperty = new ObservableValue<String>() {
            @Override
            public Subscription subscribe(Observer<? super String> listener, SubscriptionOption... options) {
                upstreamSubscription.set(property.subscribe(listener, options));
                return upstreamSubscription.get();
            }

            @Override
            public String get() {
                return property.get();
            }
        };
        WeakReference<ObservableValue<Integer>> derivedObservable = gcWhileSubscribedButUpstreamSubscriptionKilled_derive(
                wrappedProperty);
        assertThat(derivedObservable.get()).isNotNull();
        forceGc(); /* downstream listener and upstream subscription intact => GC protected */
        assertThat(derivedObservable.get()).isNotNull();
        property.set("1");
        assertThat(methodReferenceUpdateValue.get(5, SECONDS)).isEqualTo(1);
        upstreamSubscription.get().unsubscribe();
        upstreamSubscription.set(null);
        forceGc(); /*  upstream subscription killed => not GC protected anymore */
        assertThat(derivedObservable.get()).isNull();
    }

    private WeakReference<ObservableValue<Integer>> gcWhileSubscribedButUpstreamSubscriptionKilled_derive(
            ObservableValue<String> value) {
        ObservableValue<Integer> derived = derive(value, v -> Optional.of(parseInt(v)));
        derived.subscribe(this::handleUpdate);
        return new WeakReference<>(derived);
    }

    @SuppressWarnings("UnusedAssignment")
    @Test
    public void gcWithMultipleDerivationsWhileSubscribedAndSourceReferenced_shouldPreventGc() throws Exception {
        Property<String> property = Properties.property("42");
        ObservableValue<String> step1 = property.map(identity());
        ObservableValue<Integer> step2 = step1.map(Integer::parseInt);
        ObservableValue<Integer> step3 = step2.filter(any -> true);

        step3.subscribe(this::handleUpdate);

        WeakReference<?> step1ref = new WeakReference<>(step1);
        step1 = null;
        WeakReference<?> step2ref = new WeakReference<>(step2);
        step2 = null;
        WeakReference<?> step3ref = new WeakReference<>(step3);
        step3 = null;

        forceGc();
        property.set("1");
        assertThat(methodReferenceUpdateValue.get(5, SECONDS)).isEqualTo(1);

        forceGc();
        assertThat(step1ref.get()).isNotNull();
        assertThat(step2ref.get()).isNotNull();
        assertThat(step3ref.get()).isNotNull();
    }

    @SuppressWarnings("UnusedAssignment")
    @Test
    public void gcWithMultipleDerivationsWhileSubscribedAndIntermediateStageReferenced_shouldPreventGc() throws Exception {
        Property<String> property = Properties.property("42");
        ObservableValue<String> step1 = property.map(identity());
        ObservableValue<Integer> step2 = step1.map(Integer::parseInt);
        ObservableValue<Integer> step3 = step2.filter(any -> true);

        step3.subscribe(this::handleUpdate);

        WeakReference<Property<String>> propertyRef = new WeakReference<>(property);
        property = null;
        WeakReference<?> step1ref = new WeakReference<>(step1);
        step1 = null;
        WeakReference<?> step2ref = new WeakReference<>(step2);
        step2 = null;

        forceGc();

        property = propertyRef.get();
        assertThat(property).isNotNull();
        assertThat(step1ref.get()).isNotNull();
        assertThat(step2ref.get()).isNotNull();
        property.set("1");
        assertThat(methodReferenceUpdateValue.get(5, SECONDS)).isEqualTo(1);
    }

    @SuppressWarnings("UnusedAssignment")
    @Test
    public void gcWithMultipleDerivationsWhileSubscribedAndNoStageReferenced_shouldGc() throws Exception {
        Property<String> property = Properties.property("42");
        ObservableValue<String> step1 = property.map(identity());
        ObservableValue<Integer> step2 = step1.map(Integer::parseInt);
        ObservableValue<Integer> step3 = step2.filter(any -> true);

        Subscription subscriptionThatShouldNotPreventGc = step3.subscribe(this::handleUpdate);

        WeakReference<?> step1ref = new WeakReference<>(step1);
        step1 = null;
        WeakReference<?> step2ref = new WeakReference<>(step2);
        step2 = null;
        WeakReference<?> step3ref = new WeakReference<>(step3);
        step3 = null;

        forceGc();
        property.set("1");
        assertThat(methodReferenceUpdateValue.get(5, SECONDS)).isEqualTo(1);

        WeakReference<?> propertyRef = new WeakReference<>(property);
        property = null;
        forceGc();
        assertThat(propertyRef.get()).isNull();
        assertThat(step1ref.get()).isNull();
        assertThat(step2ref.get()).isNull();
        assertThat(step3ref.get()).isNull();
        subscriptionThatShouldNotPreventGc.unsubscribe();
        assertThat(subscriptionThatShouldNotPreventGc).isNotNull();
    }
}
