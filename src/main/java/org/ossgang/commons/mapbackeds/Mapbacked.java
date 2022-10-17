package org.ossgang.commons.mapbackeds;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Can be added onto an interface type. Despite in general any interface can be used as mapbacked object, it is in some
 * situations unavoidable to explicitely mark an interface as such. (E.g. a json library like gson can use it to
 * select an appropriate converter). In such cases, the interface must be annotated by this annotation.
 */
@Retention(RUNTIME)
@Target(TYPE)
public @interface Mapbacked {
}
