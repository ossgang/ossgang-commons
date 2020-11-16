package org.ossgang.commons.observables;

import org.assertj.core.api.Assertions;
import org.junit.Test;
import org.ossgang.commons.observables.testing.TestObserver;

import java.util.concurrent.CompletableFuture;

import static org.ossgang.commons.observables.SubscriptionOptions.FIRST_UPDATE;
import static org.ossgang.commons.observables.testing.TestObserver.ObserverEvent.*;

public class ObserverApiComplianceTest {

    @Test
    public void testOnSubscribe_isCalledBeforeFirstUpdate_onDispatchingObservableValue() {
        DispatchingObservableValue<Object> dispatchingObservable = new DispatchingObservableValue<Object>("FIRST") {

        };
        TestObserver<Object> observer = new TestObserver<>();
        dispatchingObservable.subscribe(observer, FIRST_UPDATE);
        Assertions.assertThat(observer.receivedEvents()).containsExactly(ON_SUBSCRIBE, ON_VALUE);
    }

    @Test
    public void testValueAndExceptionEvents_areCollectedCorrectly_onDispatchingObservableValue() {
        DispatchingObservableValue<Object> dispatchingObservable = new DispatchingObservableValue<Object>(null) {

        };
        TestObserver<Object> observer = new TestObserver<>();
        dispatchingObservable.subscribe(observer, FIRST_UPDATE);
        dispatchingObservable.dispatchValue("Value1");
        dispatchingObservable.dispatchException(new RuntimeException("Exception"));
        dispatchingObservable.dispatchValue("Value2");

        observer.awaitForEventCountsToBe(4);
        Assertions.assertThat(observer.receivedEvents()).containsExactly(ON_SUBSCRIBE, ON_VALUE, ON_EXCEPTION, ON_VALUE);
    }

    @Test
    public void testFirstUpdate_isNotPublishedOnTheCallerThread_forDispatchingObservableValue() {
        CompletableFuture<Thread> firstUpdateThreadId = new CompletableFuture<>();
        DispatchingObservableValue<Object> dispatchingObservable = new DispatchingObservableValue<Object>("First") {

        };
        dispatchingObservable.subscribe(any -> firstUpdateThreadId.complete(Thread.currentThread()), FIRST_UPDATE);

        Assertions.assertThat(Thread.currentThread())
                .as("First update must not be executed on the 'subscribe' caller Thread, but on the ossgang dispatching thread pool")
                .isNotSameAs(firstUpdateThreadId.join());
    }

}
