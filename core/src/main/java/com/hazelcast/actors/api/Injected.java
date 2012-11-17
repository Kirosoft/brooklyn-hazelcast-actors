package com.hazelcast.actors.api;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Can be placed on a field of an actor to inject a dependency. It depends on the {@link ActorFactory} if this
 * is obeyed.
 *
 * This Annotation is comparable to the @Autowired annotation of Spring. It has a different name to prevent confusion.
 *
 * @author Peter Veentjer.
 */
@Target(ElementType.FIELD)
@Retention(value = RetentionPolicy.RUNTIME)
public @interface Injected {
}
