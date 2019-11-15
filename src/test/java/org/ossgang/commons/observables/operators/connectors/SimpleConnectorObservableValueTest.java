package org.ossgang.commons.observables.operators.connectors;

import org.junit.Test;
import org.ossgang.commons.observables.ObservableValue;
import org.ossgang.commons.observables.Observables;
import org.ossgang.commons.observables.testing.TestObserver;
import org.ossgang.commons.properties.Property;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.ossgang.commons.observables.SubscriptionOptions.FIRST_UPDATE;
import static org.ossgang.commons.observables.operators.connectors.ConnectorState.CONNECTED;
import static org.ossgang.commons.observables.operators.connectors.ConnectorState.DISCONNECTED;

public class SimpleConnectorObservableValueTest {

    private static final String VALUE_1 = "Value 1";
    private static final String VALUE_2 = "Value 2";

    @Test
    public void testUpstreamConnection() {
        AtomicInteger count = new AtomicInteger();
        List<Object> values = Arrays.asList(VALUE_1, VALUE_2);
        Supplier<ObservableValue<Object>> upstreamFactory = () -> Observables.constant(values.get(count.getAndIncrement() % values.size()));
        TestObserver<Object> observer = new TestObserver<>();

        SimpleConnectorObservableValue<Object> connector = new SimpleConnectorObservableValue<>(upstreamFactory, null);
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
        List<Object> values = Arrays.asList(VALUE_1, VALUE_2);

        TestObserver<Object> observer = new TestObserver<>();
        TestObserver<ConnectorState> stateObserver = new TestObserver<>();
        TestObserver<ConnectorState> switchObserver = new TestObserver<>();

        ConnectorObservableValue<Object> connector = new SimpleConnectorObservableValue<>(cyclingUpstreamSupplier(values), null);
        Property<ConnectorState> connectorSwitch = connector.connectorState();

        connectorSwitch.subscribe(switchObserver, FIRST_UPDATE);
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
    public void testDisconnectWhileNotConnectedDoesNothing() {
        TestObserver<Object> valueObserver = new TestObserver<>();
        TestObserver<ConnectorState> stateObserver = new TestObserver<>();

        ObservableValue<Object> upstream = Observables.constant(VALUE_1);

        ConnectorObservableValue<Object> connector = new SimpleConnectorObservableValue<>(() -> upstream, null);
        connector.subscribe(valueObserver);
        connector.connectorState().subscribe(stateObserver);

        connector.connect();
        valueObserver.awaitForPublishedValuesToContain(VALUE_1);
        stateObserver.awaitForValueCountToBe(1);

        connector.disconnect();
        stateObserver.awaitForValueCountToBe(2);

        connector.disconnect();
        stateObserver.awaitForValueCountToBe(2);

        assertThat(connector.connectorState().get()).isEqualTo(DISCONNECTED);
        assertThat(stateObserver.receivedValues()).containsExactly(CONNECTED, DISCONNECTED);
        assertThat(valueObserver.receivedValues()).containsExactly(VALUE_1);
    }

    @Test
    public void testConnectWhileAlreadyConnectedDisconnectsFromOldUpstream() {
        List<Object> values = Arrays.asList(VALUE_1, VALUE_2);
        TestObserver<Object> valueObserver = new TestObserver<>();
        TestObserver<ConnectorState> stateObserver = new TestObserver<>();

        ConnectorObservableValue<Object> connector = new SimpleConnectorObservableValue<>(cyclingUpstreamSupplier(values), null);
        connector.subscribe(valueObserver);
        connector.connectorState().subscribe(stateObserver);

        connector.connect();
        valueObserver.awaitForPublishedValuesToContain(VALUE_1);
        stateObserver.awaitForValueCountToBe(1);

        connector.connect();
        stateObserver.awaitForPublishedValuesToContain(DISCONNECTED);
        valueObserver.awaitForPublishedValuesToContain(VALUE_2);
        stateObserver.awaitForValueCountToBe(3);

        assertThat(connector.connectorState().get()).isEqualTo(CONNECTED);
        assertThat(stateObserver.receivedValues()).containsExactlyInAnyOrder(CONNECTED, DISCONNECTED, CONNECTED);
        assertThat(valueObserver.receivedValues()).containsExactly(VALUE_1, VALUE_2);
    }

    private static Supplier<ObservableValue<Object>> cyclingUpstreamSupplier(List<Object> values) {
        AtomicInteger count = new AtomicInteger();
        return () -> Observables.constant(values.get(count.getAndIncrement() % values.size()));
    }

}