package com.hazelcast.actors.api;


/**
 * An interface an Actor can implement to get the ActorContext injected.
 *
 * @author Peter Veentjer.
 */
public interface ActorContextAware{

    void setActorContext(ActorContext context);
}
