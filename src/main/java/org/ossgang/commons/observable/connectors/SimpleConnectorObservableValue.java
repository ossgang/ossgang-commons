package org.ossgang.commons.observable.connectors;

import org.ossgang.commons.observable.ObservableValue;

import java.util.function.Supplier;

import static java.util.Objects.requireNonNull;

/**
 * TODO
 *
 * @param <T>
 */
public class SimpleConnectorObservableValue<T> extends AbstractConnectorObservableValue<T> implements ConnectorObservableValue<T> {

    private final Supplier<ObservableValue<T>> upstreamSupplier;

    protected SimpleConnectorObservableValue(Supplier<ObservableValue<T>> upstreamSupplier, T initial) {
        super(initial);
        this.upstreamSupplier = requireNonNull(upstreamSupplier, "Upstream supplier cannot be null");
    }

    @Override
    public void connect() {
        ObservableValue<T> upstreamObservable = requireNonNull(upstreamSupplier.get(), "Connector upstream supplier produced a null observable! Not connecting");
        super.connect(upstreamObservable);
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
