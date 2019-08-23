package org.ossgang.commons.observable;

import org.junit.Test;
import org.ossgang.commons.property.Properties;
import org.ossgang.commons.property.Property;

import java.lang.ref.WeakReference;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;

public class DerivedObservableValueGcTest {
    private CompletableFuture<Integer> methodReferenceUpdateValue = new CompletableFuture<>();

    private void handleUpdate(int test) {
        methodReferenceUpdateValue.complete(test);
    }


    @Test
    public void gcWhileSubscribed_shouldPreventGc() throws Exception {
        Property<String> property = Properties.property("2");
        gcWhileSubscribed_shouldPreventGc_subscribe(property);
        forceGc();
        property.set("1");
        assertThat(methodReferenceUpdateValue.get(1, SECONDS)).isEqualTo(1);
    }

    private void gcWhileSubscribed_shouldPreventGc_subscribe(ObservableValue<String> value) {
        value.map(Integer::parseInt).subscribe(this::handleUpdate);
    }

    @Test
    public void gcWhileSubscribed_shouldPreventGcOfUpstream() throws Exception {
        WeakReference<Property<String>> property = gcWhileSubscribed_shouldPreventGcOfUpstream_subscribe();
        forceGc();
        Property<String> prop = property.get();
        assertThat(prop).isNotNull();
        prop.set("3");
        assertThat(methodReferenceUpdateValue.get(1, SECONDS)).isEqualTo(3);
    }

    private WeakReference<Property<String>> gcWhileSubscribed_shouldPreventGcOfUpstream_subscribe() {
        Property<String> property = Properties.property("2");
        property.map(Integer::parseInt).subscribe(this::handleUpdate);
        return new WeakReference<>(property);
    }

    @Test
    public void gcWhileNotSubscribed_shouldGc() {
        Property<String> property = Properties.property("2");
        WeakReference<ObservableValue<Integer>> derivedObservable = mapWeakReference(property);
        assertThat(derivedObservable.get()).isNotNull();
        forceGc();
        assertThat(derivedObservable.get()).isNull();
    }

    private WeakReference<ObservableValue<Integer>> mapWeakReference(ObservableValue<String> value) {
        return new WeakReference<>(value.map(Integer::parseInt));
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
        WeakReference<ObservableValue<Integer>> derivedObservable = mapWeakReference(wrappedProperty);
        assertThat(derivedObservable.get()).isNotNull();
        derivedObservable.get().subscribe(this::handleUpdate);
        assertThat(derivedObservable.get()).isNotNull();
        forceGc(); /* downstream listener and upstream subscription intact => GC protected */
        assertThat(derivedObservable.get()).isNotNull();
        property.set("1");
        assertThat(methodReferenceUpdateValue.get(5, SECONDS)).isEqualTo(1);
        upstreamSubscription.get().unsubscribe();
        forceGc(); /*  upstream subscription killed => not GC protected anymore */
        assertThat(derivedObservable.get()).isNull();
    }

    /**
     * Force the GC to run a few times. Note that (in Oracles JVM) multiple GC runs are needed to resolve indirections
     * and cycles, therefore this methods makes sure that the GC run at least 10 times.
     */
    private static void forceGc() {
        for (int run = 0; run < 10; run++) {
            WeakReference<?> ref = new WeakReference<>(new Object());
            while (ref.get() != null) {
                System.gc();
            }
        }
    }
}
