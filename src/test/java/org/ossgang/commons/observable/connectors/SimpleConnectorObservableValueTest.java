package org.ossgang.commons.observable.connectors;

import org.assertj.core.api.Assertions;
import org.junit.Test;
import org.ossgang.commons.observable.ObservableValue;
import org.ossgang.commons.observable.Observables;
import org.ossgang.commons.observers.TestObserver;
import org.ossgang.commons.property.Properties;
import org.ossgang.commons.property.Property;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.ossgang.commons.observable.SubscriptionOptions.FIRST_UPDATE;
import static org.ossgang.commons.observable.connectors.ConnectorState.CONNECTED;
import static org.ossgang.commons.observable.connectors.ConnectorState.DISCONNECTED;

public class SimpleConnectorObservableValueTest {

    private static final String VALUE_1 = "Value 1";
    private static final String VALUE_2 = "Value 2";

    @Test
    public void testUpstreamConnection() {
        AtomicInteger count = new AtomicInteger();
        List<Object> values = Arrays.asList(VALUE_1, VALUE_2);
        Supplier<ObservableValue<Object>> factory = () -> Observables.constant(values.get(count.getAndIncrement() % values.size()));
        TestObserver<Object> observer = new TestObserver<>();

        SimpleConnectorObservableValue<Object> connector = new SimpleConnectorObservableValue<>(factory, null);
        connector.subscribe(observer);

        connector.connect();
        connector.disconnect();
        observer.awaitForValueCountToBe(1);

        connector.connect();
        connector.disconnect();
        observer.awaitForValueCountToBe(2);

        assertThat(observer.receivedValues()).containsExactlyElementsOf(values);
    }

    @Test
    public void testUpstreamConnectionViaProperty() {
        AtomicInteger count = new AtomicInteger();
        List<Object> values = Arrays.asList(VALUE_1, VALUE_2);
        Supplier<ObservableValue<Object>> factory = () -> Observables.constant(values.get(count.getAndIncrement() % values.size()));
        TestObserver<Object> observer = new TestObserver<>();
        TestObserver<ConnectorState> stateObserver = new TestObserver<>();
        TestObserver<ConnectorState> switchObserver = new TestObserver<>();

        Property<ConnectorState> connectorSwitch = Properties.property();
        connectorSwitch.subscribe(switchObserver, FIRST_UPDATE);

        ConnectorObservableValue<Object> connector = Observables.connectWhen(factory, connectorSwitch);
        connector.connectorState().subscribe(stateObserver, FIRST_UPDATE);
        connector.subscribe(observer, FIRST_UPDATE);

        connectorSwitch.set(CONNECTED);
        switchObserver.awaitForValueCountToBe(1);
        observer.awaitForPublishedValuesToContain(VALUE_1);
        stateObserver.awaitForValueCountToBe(2);

        connectorSwitch.set(DISCONNECTED);
        switchObserver.awaitForValueCountToBe(2);
        stateObserver.awaitForValueCountToBe(2);

        connectorSwitch.set(CONNECTED);
        switchObserver.awaitForValueCountToBe(3);
        observer.awaitForPublishedValuesToContain(VALUE_2);
        stateObserver.awaitForValueCountToBe(3);

        connectorSwitch.set(DISCONNECTED);
        switchObserver.awaitForValueCountToBe(4);
        stateObserver.awaitForValueCountToBe(4);

        assertThat(observer.receivedValues()).containsExactlyElementsOf(values);
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