package org.ossgang.commons.observables.operators.connectors;

import org.ossgang.commons.observables.ObservableValue;
import org.ossgang.commons.properties.Property;

import java.util.function.Supplier;

import static java.util.Objects.requireNonNull;
import static org.ossgang.commons.properties.Properties.wrapperProperty;

/**
 * Simple implementation of a {@link ConnectorObservableValue}
 *
 * @param <T> the type of the observable
 */
public class SimpleConnectorObservableValue<T> extends AbstractConnectorObservableValue<T> implements ConnectorObservableValue<T> {

    private final Supplier<ObservableValue<T>> upstreamSupplier;
    private final Property<ConnectorState> connectorStateProperty;

    SimpleConnectorObservableValue(Supplier<ObservableValue<T>> upstreamSupplier, T initial) {
        super(initial);
        this.upstreamSupplier = requireNonNull(upstreamSupplier, "Upstream supplier cannot be null");
        connectorStateProperty = wrapperProperty(super.connectionState(), this::setConnectorState);
    }

    @Override
    public void connect() {
        super.connect(upstreamSupplier);
    }

    @Override
    public void disconnect() {
        super.disconnect();
    }

    @Override
    public Property<ConnectorState> connectorState() {
        return connectorStateProperty;
    }

    private void setConnectorState(ConnectorState connectorState) {
        if (connectorState == ConnectorState.CONNECTED) {
            connect();
        } else if (connectorState == ConnectorState.DISCONNECTED) {
            disconnect();
        } else {
            throw new IllegalArgumentException("Cannot set connector state to " + connectorState);
        }
    }
}
