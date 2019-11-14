package org.ossgang.commons.observable.connectors;

import org.ossgang.commons.observable.ObservableValue;
import org.ossgang.commons.property.Property;

/**
 * {@link ObservableValue} that can connect and disconnect from upstream while keeping all the subscribers alive.
 *
 * @param <T> the type of the observable
 */
public interface ConnectorObservableValue<T> extends ObservableValue<T> {

    /**
     * Connect to upstream
     */
    void connect();

    /**
     * Disconnect from upstream
     */
    void disconnect();

    /**
     * Returns the {@link Property} that could be used to control the {@link ConnectorState} of this {@link ConnectorObservableValue}.
     * By setting the {@link ConnectorState} this {@link ConnectorObservableValue} will connect or disconnect from upstream.
     *
     * @return the {@link Property} for controlling the {@link ConnectorState} of this {@link ConnectorObservableValue}
     */
    Property<ConnectorState> connectorState();

}
