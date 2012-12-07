package com.hazelcast.actors.impl.actorcontainers;

import com.hazelcast.actors.api.*;
import com.hazelcast.core.IMap;
import com.hazelcast.spi.impl.NodeServiceImpl;

import java.util.Set;

/**
 * The ActorContainer is a container that manages a single actor instance.
 *
 * Normally the ActorContainer manages the Mailbox, the threading etc.
 *
 * @param <A>
 */
public interface ActorContainer<A extends Actor> {

    /**
     * Gets the Actor this ActorContainer manages.
     *
     * @return the Actor this ActorContainer manages.
     */
    A getActor();

    /**
     * Activates the ActorContainer (and therefor the Actor) so it can be used for message processing. Normally
     * this is the method that creates the real actor object.
     *
     * Will only be called by a single method; never concurrently with other methods.
     *
     * @return
     */
    void activate();

    void exit() throws Exception;

    /**
     * Posts a message to the ActorContainer.
     *
     * @param sender is allowed to be null
     * @param message is not allowed to be null.
     * @throws InterruptedException
     */
    void post(ActorRef sender, Object message) throws InterruptedException;
}
