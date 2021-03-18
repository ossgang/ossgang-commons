package org.ossgang.commons.observables;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.ossgang.commons.GcTests.forceGc;

import java.lang.ref.WeakReference;
import java.util.HashSet;
import java.util.concurrent.CompletableFuture;

import org.junit.Test;
import org.ossgang.commons.properties.Properties;
import org.ossgang.commons.properties.Property;

public class ObservableValueGcTest {
    private CompletableFuture<Integer> methodReferenceUpdateValue = new CompletableFuture<>();

    private void handleUpdate(int test) {
        methodReferenceUpdateValue.complete(test);
    }

    @Test
    public void gcWhileSubscribed_shouldPreventGc() throws Exception {
        WeakReference<Property<Integer>> weakProp = gcWhileSubscribed_shouldPreventGc_subscribe();
        forceGc();
        Property<Integer> prop = weakProp.get();
        assertThat(prop).isNotNull();
        prop.set(2);
        assertThat(methodReferenceUpdateValue.get(1, SECONDS)).isEqualTo(2);
    }

    private WeakReference<Property<Integer>> gcWhileSubscribed_shouldPreventGc_subscribe() {
        Property<Integer> property = Properties.property(1);
        property.subscribe(this::handleUpdate);
        return new WeakReference<>(property);
    }

    @Test
    public void gcWhileNotSubscribed_shouldGc() {
        WeakReference<Property<Integer>> derivedObservable = gcWhileNotSubscribed_shouldGc_makeProperty();
        assertThat(derivedObservable.get()).isNotNull();
        forceGc();
        assertThat(derivedObservable.get()).isNull();
    }

    private WeakReference<Property<Integer>> gcWhileNotSubscribed_shouldGc_makeProperty() {
        Property<Integer> property = Properties.property(1);
        return new WeakReference<>(property);
    }

    @Test
    public void gcAfterUnsubscribe_shouldGc() {
        WeakReference<Property<Integer>> weakProp = gcAfterUnsubscribe_shouldGc_subscribeAndUnsubscribe();
        forceGc();
        Property<Integer> prop = weakProp.get();
        assertThat(prop).isNull();
    }

    private WeakReference<Property<Integer>> gcAfterUnsubscribe_shouldGc_subscribeAndUnsubscribe() {
        Property<Integer> property = Properties.property(1);
        HashSet<Subscription> subscriptions = new HashSet<>();
        for (int i=0; i<100; i++) {
            subscriptions.add(property.subscribe(this::handleUpdate));
        }
        subscriptions.forEach(Subscription::unsubscribe);
        return new WeakReference<>(property);
    }
}
