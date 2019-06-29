# ossgang-commons
A set of shared common classes.

This package currently contains the following:

### Maybe monad
A variant of Javas ``Optional<T>`` which carries either a value or an
exception that occurred while producing that value. Functional approaches
like ``map()`` are fully supported.

As a special case, a ``Maybe<Void>`` may be used for functional-style
exception handling.

### Observable, ObservableValue, Property
A simple, lightweight implementation of a value type which can be observed
for changes. A concept similar to the likely-named JavaFX classes, but
without the tight binding to JavaFX or GUIs.

An ``Observable<T>`` is a simple, state-less event stream of objects of
type ``T``.

An ``ObservableValue<T>`` is a wrapper around a value of type ``T`` which
can be observed for changes (and get at any time).

A ``Property<T>`` is an ``ObservableValue<T>`` which can also be set,
i.e. it has got a public setter.