package org.ossgang.commons.observable.connectors;

public enum ConnectorState {
    CONNECTED,
    DISCONNECTED;

    public static boolean isConnected(ConnectorState state) {
        return (CONNECTED == state);
    }

    public static ConnectorState fromConnected(boolean connected) {
        if (connected) {
            return CONNECTED;
        }
        return DISCONNECTED;
    }
}
