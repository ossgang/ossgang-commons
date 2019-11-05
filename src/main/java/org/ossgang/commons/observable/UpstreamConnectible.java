package org.ossgang.commons.observable;

import org.ossgang.commons.observable.connectors.ConnectorState;
import org.ossgang.commons.property.Property;

/**
 * Can be implemented by observables which have the ability to connect/disconnect upstream. The actual connection can be
 * managed (and observed) through the provided {@link #upstreamConnector()} property.
 */
public interface UpstreamConnectible {

    Property<ConnectorState> upstreamConnector();

}
