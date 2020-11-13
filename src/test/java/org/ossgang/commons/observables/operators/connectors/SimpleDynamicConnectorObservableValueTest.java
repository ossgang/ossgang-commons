package org.ossgang.commons.observables.operators.connectors;

import org.junit.Test;
import org.ossgang.commons.observables.ObservableValue;
import org.ossgang.commons.observables.Observables;
import org.ossgang.commons.observables.testing.TestObserver;

import java.util.function.Function;

import static org.assertj.core.api.Assertions.assertThat;
import static org.ossgang.commons.observables.operators.connectors.ConnectorState.CONNECTED;
import static org.ossgang.commons.observables.operators.connectors.ConnectorState.DISCONNECTED;

public class SimpleDynamicConnectorObservableValueTest {

    private static final String VALUE_1 = "Value 1";
    private static final String VALUE_2 = "Value 2";

    @Test
    public void testUpstreamDispatchingObservableConnection() {
        ObservableValue<Object> upstream1 = Observables.<Object>constant(VALUE_1).map(Function.identity());
        ObservableValue<Object> upstream2 = Observables.<Object>constant(VALUE_2).map(Function.identity());
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
    public void testDisconnectWhileNotConnectedDoesNothing() {
        TestObserver<Object> valueObserver = new TestObserver<>();
        TestObserver<ConnectorState> stateObserver = new TestObserver<>();

        ObservableValue<Object> upstream = Observables.constant(VALUE_1);

        DynamicConnectorObservableValue<Object> connector = new SimpleDynamicConnectorObservableValue<>(null);
        connector.subscribe(valueObserver);
        connector.connectorState().subscribe(stateObserver);

        connector.connect(upstream);
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
        TestObserver<Object> valueObserver = new TestObserver<>();
        TestObserver<ConnectorState> stateObserver = new TestObserver<>();

        ObservableValue<Object> upstream1 = Observables.constant(VALUE_1);
        ObservableValue<Object> upstream2 = Observables.constant(VALUE_2);

        DynamicConnectorObservableValue<Object> connector = new SimpleDynamicConnectorObservableValue<>(null);
        connector.subscribe(valueObserver);
        connector.connectorState().subscribe(stateObserver);

        connector.connect(upstream1);
        valueObserver.awaitForPublishedValuesToContain(VALUE_1);
        stateObserver.awaitForValueCountToBe(1);

        connector.connect(upstream2);
        stateObserver.awaitForPublishedValuesToContain(DISCONNECTED);
        valueObserver.awaitForPublishedValuesToContain(VALUE_2);
        stateObserver.awaitForValueCountToBe(3);

        assertThat(connector.connectorState().get()).isEqualTo(CONNECTED);
        assertThat(stateObserver.receivedValues()).containsExactlyInAnyOrder(CONNECTED, DISCONNECTED, CONNECTED);
        assertThat(valueObserver.receivedValues()).containsExactly(VALUE_1, VALUE_2);
    }

}