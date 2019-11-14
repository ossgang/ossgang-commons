package org.ossgang.commons.observable.connectors;

import org.ossgang.commons.observable.ObservableValue;

/**
 * {@link ObservableValue} that can connect and disconnect from upstream while keeping all the subscribers alive.
 * The {@link DynamicConnectorObservableValue} allows to specify the upstream {@link ObservableValue} on each connection,
 * so it is possible to dynamically change the upstream {@link ObservableValue}.
 *
 * @param <T> the type of the Observable
 */
public interface DynamicConnectorObservableValue<T> extends ObservableValue<T> {

    /**
     * Connect to the specified upstream {@link ObservableValue}
     *
     * @param upstream the {@link ObservableValue} to connect to
     */
    void connect(ObservableValue<T> upstream);

    /**
     * Disconnect from upstream
     */
    void disconnect();

    /**
     * The {@link ObservableValue} of the {@link ConnectorState} of this {@link DynamicConnectorObservableValue}.
     *
     * @return the {@link ObservableValue} of the {@link ConnectorState} of this {@link DynamicConnectorObservableValue}
     */
    ObservableValue<ConnectorState> connectorState();

}
