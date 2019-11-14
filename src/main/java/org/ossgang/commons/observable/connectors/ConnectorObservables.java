package org.ossgang.commons.observable.connectors;

import org.ossgang.commons.observable.ObservableValue;

import java.util.function.Supplier;

/**
 * Static support class for dealing with {@link ConnectorObservableValue}s
 */
public final class ConnectorObservables {

    private ConnectorObservables() {
        throw new UnsupportedOperationException("static only");
    }

    /**
     * Creates a {@link ConnectorObservableValue} that on each connection will subscribe to the upstream {@link ObservableValue}
     * produced by the specified {@link Supplier}
     *
     * @param supplier the supplier of upstream {@link ObservableValue} to be used when connecting
     * @param <T>      the type of the observable
     * @return a {@link ConnectorObservableValue} that uses the specified {@link Supplier} for connecting upstream
     */
    public static <T> ConnectorObservableValue<T> connectorObservableValue(Supplier<ObservableValue<T>> supplier) {
        return new SimpleConnectorObservableValue<>(supplier, null);
    }

    /**
     * Creates a {@link ConnectorObservableValue} that will connect to the specified upstream {@link ObservableValue}.
     *
     * @param upstream the upstream {@link ObservableValue} to connect to
     * @param <T>      the type of the observable
     * @return a {@link ConnectorObservableValue} that connects to the specified {@link ObservableValue}
     */
    public static <T> ConnectorObservableValue<T> connectorTo(ObservableValue<T> upstream) {
        return new SimpleConnectorObservableValue<>(() -> upstream, null);
    }

    /**
     * Creates a {@link DynamicConnectorObservableValue}
     *
     * @param <T> the type of the observable
     * @return a {@link DynamicConnectorObservableValue}
     */
    public static <T> DynamicConnectorObservableValue<T> dynamicConnectorObservableValue() {
        return new SimpleDynamicConnectorObservableValue<>(null);
    }

}
