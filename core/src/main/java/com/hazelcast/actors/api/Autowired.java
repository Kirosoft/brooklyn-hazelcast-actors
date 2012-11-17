package com.hazelcast.actors.api;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Can be placed on a field of an actor to inject a dependency. It depends on the {@link ActorFactory} if this
 * is obeyed.
 *
 * @author Peter Veentjer.
 */
@Target(ElementType.FIELD)
@Retention(value = RetentionPolicy.RUNTIME)
public @interface Autowired {
}
