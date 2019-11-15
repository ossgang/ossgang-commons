package org.ossgang.commons.observables.operators.connectors;

import org.ossgang.commons.observables.ObservableValue;

/**
 * Simple implementation of a {@link DynamicConnectorObservableValue}
 *
 * @param <T> the type of the observable
 */
public class SimpleDynamicConnectorObservableValue<T> extends AbstractConnectorObservableValue<T> implements DynamicConnectorObservableValue<T> {

    SimpleDynamicConnectorObservableValue(T initial) {
        super(initial);
    }

    @Override
    public void connect(ObservableValue<T> upstream) {
        super.connect(upstream);
    }

    @Override
    public void disconnect() {
        super.disconnect();
    }

    @Override
    public ObservableValue<ConnectorState> connectorState() {
        return super.connectionState();
    }

}
