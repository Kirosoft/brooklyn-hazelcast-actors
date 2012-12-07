package com.hazelcast.actors.impl.actorcontainers;

import com.hazelcast.actors.api.Actor;
import com.hazelcast.actors.api.ActorRecipe;
import com.hazelcast.actors.api.ActorRef;

/**
 * The Caller runs actor container executes it on the calling thread. This container should only be used for testing
 * since it doesn't deal correctly with concurrent invokes of the post method.
 *
 * @param <A>
 */
public class CallerRunsActorContainer<A extends Actor> extends AbstractActorContainer<A> {

    public CallerRunsActorContainer(ActorRecipe<A> recipe, ActorRef actorRef, Dependencies dependencies) {
        super(recipe, actorRef, dependencies);
    }

    @Override
    public void post(ActorRef sender, Object message) throws InterruptedException {
        try {
            actor.receive(message, sender);
        } catch (Exception exception) {
            handleProcessingException(sender, exception);
        }
    }
}
