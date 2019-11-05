package org.ossgang.commons.observable.connectors;

import org.ossgang.commons.observable.ObservableValue;
import org.ossgang.commons.observable.Subscription;

import java.lang.ref.WeakReference;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

import static org.ossgang.commons.observable.SubscriptionOptions.FIRST_UPDATE;
import static org.ossgang.commons.observable.connectors.ConnectorState.CONNECTED;
import static org.ossgang.commons.observable.connectors.ConnectorState.DISCONNECTED;

public final class ConnectorObservables {

    private ConnectorObservables() {
        throw new UnsupportedOperationException("static only");
    }

    public static <T> ConnectorObservableValue<T> connectWhen(Supplier<ObservableValue<T>> upstreamSupplier, ObservableValue<ConnectorState> connectorProvider) {
        ConnectorObservableValue<T> connectorObservable = connectorObservableValue(upstreamSupplier);
        bindConnectorToConnectorStateProvider(connectorObservable, connectorProvider);
        return connectorObservable;
    }

    public static <T> ConnectorObservableValue<T> connectWhen(ObservableValue<T> upstream, ObservableValue<ConnectorState> connectorProvider) {
        ConnectorObservableValue<T> connectorObservable = connectorTo(upstream);
        bindConnectorToConnectorStateProvider(connectorObservable, connectorProvider);
        return connectorObservable;
    }

    private static <T> void bindConnectorToConnectorStateProvider(ConnectorObservableValue<T> connectorObservable, ObservableValue<ConnectorState> connectorProvider) {
        WeakReference<ConnectorObservableValue<T>> connectorWeakReference = new WeakReference<>(connectorObservable);
        AtomicReference<Subscription> subscriptionReference = new AtomicReference<>();
        subscriptionReference.set(connectorProvider.filter(Objects::nonNull).subscribe(requestedState -> {
            ConnectorObservableValue<T> connector = connectorWeakReference.get();
            if (connector == null) {
                Subscription subscription = subscriptionReference.getAndSet(null);
                if (subscription != null) {
                    subscription.unsubscribe();
                }
            } else {
                ConnectorState actualState = connector.connectorState().get();
                if (requestedState == CONNECTED && actualState == DISCONNECTED) {
                    connector.connect();
                } else if (requestedState == DISCONNECTED && actualState == CONNECTED) {
                    connector.disconnect();
                }
            }
        }, FIRST_UPDATE));
    }

    public static <T> ConnectorObservableValue<T> connectorObservableValue(Supplier<ObservableValue<T>> supplier) {
        return new SimpleConnectorObservableValue<>(supplier, null);
    }

    public static <T> ConnectorObservableValue<T> connectorTo(ObservableValue<T> upstream) {
        SimpleConnectorObservableValue<T> connector = new SimpleConnectorObservableValue<>(() -> upstream, null);
        connector.connect();
        return connector;
    }

    public static <T> DynamicConnectorObservableValue<T> dynamicConnectorObservableValue() {
        return new SimpleDynamicConnectorObservableValue<>(null);
    }

}
