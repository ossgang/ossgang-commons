package org.ossgang.commons.observable.connectors;

import org.assertj.core.api.Assertions;
import org.junit.Test;
import org.ossgang.commons.observable.ObservableValue;
import org.ossgang.commons.observable.Observables;
import org.ossgang.commons.observable.TestObserver;

import static org.assertj.core.api.Assertions.assertThat;

public class SimpleDynamicConnectorObservableValueTest {

    private static final String VALUE_1 = "Value 1";
    private static final String VALUE_2 = "Value 2";

    @Test
    public void testUpstreamConnection() {
        ObservableValue<Object> upstream1 = Observables.constant(VALUE_1);
        ObservableValue<Object> upstream2 = Observables.constant(VALUE_2);
        TestObserver<Object> observer = new TestObserver<>();

        SimpleDynamicConnectorObservableValue<Object> connector = new SimpleDynamicConnectorObservableValue<>(null);
        connector.subscribe(observer);

        connector.connect(upstream1);
        connector.disconnect();
        observer.awaitForValueCountToBe(1);

        connector.connect(upstream2);
        connector.disconnect();
        observer.awaitForValueCountToBe(2);

        assertThat(observer.receivedValues()).containsExactly(VALUE_1, VALUE_2);
    }

    @Test
    public void testDisconnectWhileNotConnectedFails() {
        Assertions.fail("TBD");
    }

    @Test
    public void testConnectWhileAlreadyConnectedFails() {
        Assertions.fail("TBD");
    }

}