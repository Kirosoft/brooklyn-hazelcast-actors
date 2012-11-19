package com.hazelcast.actors.api;

import java.util.Collection;
import java.util.Map;

public interface ActorRuntime {

    void send(ActorRef destination, Object msg);

    void send(ActorRef sender, Collection<ActorRef> destinations, Object msg);

    void send(ActorRef sender, ActorRef destination, Object msg);

    void terminate(ActorRef target);

    void repeat(ActorRef ref, Object msg, int delaysMs);

    void monitor(ActorRef listener, ActorRef target);

    ActorRef newActor(Class<? extends Actor> actorClass);

    ActorRef newActor(Class<? extends Actor> actorClass, Map<String, Object> properties);

    ActorRef newActor(Class<? extends Actor> actorClass, int partitionId);

    /**
     * Creates a new Actor that runs somewhere in the system.
     * <p/>
     * After this method returns you get the guarantee that the actor can be found, e.g. for sending messages, unless
     * the actor has been destroyed.
     * <p/>
     * Creating the Actor is a synchronous operation, so the call waits till the actor has been fully constructed and
     * activated. If an exception is thrown during construction or activation, this exception will be propagated and
     * the Actor will be discarded.
     *
     * @param actorClass  the class of the Actor to create.
     * @param partitionId the id of the partition to create the actor on. -1 Indicates that there is no preference for
     *                    a partition and one will be selected.
     * @param properties  the properties handed over to the actor. Can be null if no properties need to be passed.
     * @return
     * @throws RuntimeException
     * @throws NullPointerException if actorClass is null.
     */
    ActorRef newActor(Class<? extends Actor> actorClass, int partitionId, Map<String, Object> properties);

    ActorRef newActor(ActorRecipe recipe);
}
