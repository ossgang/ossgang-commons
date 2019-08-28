package org.ossgang.commons.observable;

/**
 * Basic {@link SubscriptionOption}s that may or may not be available for all {@link Observable} or {@link ObservableValue}.
 * <br>
 * Implementation note: at the moment this class is an enum, future versions will be able to migrate to class for
 * more advanced options without breaking the compatibility.
 */
public enum SubscriptionOptions implements SubscriptionOption {
    /**
     * On subscription, deliver the actual value (it it exists) as a "first update".
     * This happens on the subscribing thread, before any other updates are delivered,
     * and before subscribe() returns.
     */
    FIRST_UPDATE,

    /**
     * Only notify the subscriber on updates which actually changed the value of this ObservableValue.
     */
    ON_CHANGE
}
