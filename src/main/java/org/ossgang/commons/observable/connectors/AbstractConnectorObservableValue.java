package org.ossgang.commons.observable.connectors;

import org.ossgang.commons.observable.DispatchingObservableValue;
import org.ossgang.commons.observable.ObservableValue;
import org.ossgang.commons.observable.Subscription;
import org.ossgang.commons.property.Properties;
import org.ossgang.commons.property.Property;

import java.util.function.Supplier;

import static java.util.Objects.requireNonNull;
import static org.ossgang.commons.observable.SubscriptionOptions.FIRST_UPDATE;
import static org.ossgang.commons.observable.WeakObservers.weakWithErrorAndSubscriptionCountHandling;
import static org.ossgang.commons.observable.connectors.ConnectorState.CONNECTED;
import static org.ossgang.commons.observable.connectors.ConnectorState.DISCONNECTED;

/**
 * Base implementation for the {@link ConnectorObservableValue} and {@link DynamicConnectorObservableValue}.
 *
 * @param <T> the type of the observable
 */
public abstract class AbstractConnectorObservableValue<T> extends DispatchingObservableValue<T> {

    private final Object lock = new Object();
    private ObservableValue<T> upstreamObservable;
    private Subscription upstreamSubscription;
    private Property<ConnectorState> connectionState;

    protected AbstractConnectorObservableValue(T initial) {
        super(initial);
        connectionState = Properties.property(ConnectorState.DISCONNECTED);
    }

    protected void connect(Supplier<ObservableValue<T>> upstreamSupplier) {
        synchronized (lock) {
            ObservableValue<T> upstreamObservable = requireNonNull(upstreamSupplier.get(), "Connector upstream supplier produced a null observable! Not connecting");
            connect(upstreamObservable);
        }
    }

    protected void connect(ObservableValue<T> upstream) {
        synchronized (lock) {
            if (connectionState.get() == CONNECTED) {
                disconnect();
            }
            upstreamObservable = requireNonNull(upstream, "Upstream observable cannot be null! Not connecting");
            upstreamSubscription = upstreamObservable.subscribe(weakWithErrorAndSubscriptionCountHandling(this,
                    (self, value) -> self.dispatchValue(value),
                    (self, exception) -> self.dispatchException(exception),
                    AbstractConnectorObservableValue::subscriberCountChanged), FIRST_UPDATE);
            connectionState.set(CONNECTED);
        }
    }

    protected void disconnect() {
        synchronized (lock) {
            if (connectionState.get() == CONNECTED) {
                upstreamSubscription.unsubscribe();
                upstreamSubscription = null;
                upstreamObservable = null;
                connectionState.set(DISCONNECTED);
            }
        }
    }

    protected ObservableValue<ConnectorState> connectionState() {
        return connectionState;
    }

    private void subscriberCountChanged(Integer count) {
        if (count <= 0) {
            unsubscribeAllObservers();
        }
    }

}
