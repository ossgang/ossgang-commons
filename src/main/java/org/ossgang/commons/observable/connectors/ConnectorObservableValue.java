package org.ossgang.commons.observable.connectors;

import org.ossgang.commons.observable.ObservableValue;

/**
 * TODO
 * @param <T>
 */
public interface ConnectorObservableValue<T> extends ObservableValue<T> {

    /**
     * TODO
     */
    void connect();

    /**
     * TODO
     */
    void disconnect();

    /**
     * TODO
     * @return
     */
    ObservableValue<ConnectorState> connectorState();
}
