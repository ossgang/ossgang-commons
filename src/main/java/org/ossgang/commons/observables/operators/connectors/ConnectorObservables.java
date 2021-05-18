package org.ossgang.commons.observables.operators.connectors;

import java.util.function.Supplier;

import org.ossgang.commons.observables.ObservableValue;

/**
 * Static support class for dealing with {@link ConnectorObservableValue}s
 */
public final class ConnectorObservables {

    private ConnectorObservables() {
        throw new UnsupportedOperationException("static only");
    }

    /**
     * @see org.ossgang.commons.observables.Observables#connectorObservableValue(Supplier)
     */
    public static <T> ConnectorObservableValue<T> connectorObservableValue(Supplier<ObservableValue<T>> supplier) {
        return new SimpleConnectorObservableValue<>(supplier, null);
    }

    /**
     * @see org.ossgang.commons.observables.Observables#connectorTo(ObservableValue)
     */
    public static <T> ConnectorObservableValue<T> connectorTo(ObservableValue<T> upstream) {
        return new SimpleConnectorObservableValue<>(() -> upstream, null);
    }

    /**
     * @see org.ossgang.commons.observables.Observables#dynamicConnectorObservableValue()
     */
    public static <T> DynamicConnectorObservableValue<T> dynamicConnectorObservableValue() {
        return new SimpleDynamicConnectorObservableValue<>(null);
    }

}
