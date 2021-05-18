[![Build Status](https://travis-ci.com/ossgang/ossgang-commons.svg?branch=master)](https://travis-ci.com/ossgang/ossgang-commons)
[![Latest release](https://img.shields.io/github/release/ossgang/ossgang-commons.svg?maxAge=1000)](https://github.com/ossgang/ossgang-commons/releases)
[![Maven Central](https://img.shields.io/maven-central/v/org.ossgang/ossgang-commons)](https://search.maven.org/artifact/org.ossgang/ossgang-commons)

# ossgang-commons
A set of shared common classes.

This package currently contains the following:

### Maybe monad
A variant of Javas ``Optional<T>`` which carries either a value or an
exception that occurred while producing that value. Functional approaches
like ``map()`` are fully supported.

As a special case, a ``Maybe<Void>`` may be used for functional-style
exception handling.

### AsyncMaybe

A variant of `Maybe<T>` that will be resolved at some point in the future. It can be considered an abstraction over the Java `CompletableFuture` with a more consistent API.

It makes heavy usage of the new Java functional style and can be used to easily create asynchronous code (e.g. with `AsyncMaybe#attemptAsync(ThrowingRunnable)` or `AsyncMaybe#attemptAsync(ThrowingSupplier)`).

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

The `Observables` class contains many useful methods to interact, combine and transform `ObservableValue<T>`

### Awaitables
Two sister utilty classes, ``Await`` and ``Retry<T>`` to facilitate wait-polling, with fluent API to configure polling intervals, limit polling iterations, or set timeouts.

``Await`` awaits a particular condition (expressed by a boolean supplier returning true).

``Retry<T>`` evaluates a ``Supplier`` until the produced value passes a provided predicate, and then returns it.

### Mapbackeds
Mapbackeds can be used as immutable data containers with a type-save interface. The field types and names are simply defined by defining a java interface.

As an example, assuming a domain object which contains 2 fields:
```
private interface Person {
    long id();
    String name();
}
```
Then an instance of this person can be created using a builder as follows:
```
Person misterX = Mapbackeds.builder(Person.class) //
         .field(Person::id, 1) //
         .field(Person::name, "MisterX") //
         .build();
```

The resulting objects respect hashCode, equals and toString and are immutable.

### Extra Collections

A ``ConcurrentCircularBuffer<T>`` is a lock-free implementation of a circular
buffer, backed by a `ConcurrentHashMap`.
