[![Build Status](https://travis-ci.com/ossgang/ossgang-commons.svg?branch=master)](https://travis-ci.com/ossgang/ossgang-commons)

# ossgang-commons
A set of shared common classes.

This package currently contains the following:

### Maybe monad
A variant of Javas ``Optional<T>`` which carries either a value or an
exception that occurred while producing that value. Functional approaches
like ``map()`` are fully supported.

As a special case, a ``Maybe<Void>`` may be used for functional-style
exception handling.

### Observable, ObservableValue, Property, Dispatcher
A simple, lightweight implementation of a value type which can be observed
for changes. A concept similar to the likely-named JavaFX classes, but
without the tight binding to JavaFX or GUIs.

An ``Observable<T>`` is a simple, state-less event stream of objects of
type ``T``.

An ``ObservableValue<T>`` is a wrapper around a value of type ``T`` which
can be observed for changes and caches the last value.

A ``Property<T>`` is an ``ObservableValue<T>`` which can also be set,
i.e. it has got a public setter. Conceptually it holds the reference to a value (``T``) that can be set, get and observed for changes.

A ``Dispatcher<T>`` is an ``ObservableValue<T>`` which can be used to dispatch
arbitrary values of ``T`` or ``Throwables`` downstream. This feature is particularly useful in the case of event dispatchers, services dispatching updates and in general any kind of 'source' of data.

### Extra Collections

A ``ConcurrentCircularBuffer<T>`` is a lock-free implementation of a circular
buffer, backed by a ConcurrentHashMap.
