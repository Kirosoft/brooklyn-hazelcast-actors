package com.hazelcast.actors.api;


/**
 * An {@link RuntimeException} thrown when an Actor failed to be instantiated.
 *
 * @author Peter Veentjer.
 */
public class ActorInstantiationException extends RuntimeException {

    public ActorInstantiationException(String message) {
        super(message);
    }

    public ActorInstantiationException(String message, Throwable cause) {
        super(message, cause);
    }
}
