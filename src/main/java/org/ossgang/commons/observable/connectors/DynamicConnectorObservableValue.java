package org.ossgang.commons.observable.connectors;

import org.ossgang.commons.observable.ObservableValue;

public interface DynamicConnectorObservableValue<T> extends ObservableValue<T> {

    void connect(ObservableValue<T> upstream);

    void disconnect();

    ObservableValue<ConnectorState> connectorState();

}
