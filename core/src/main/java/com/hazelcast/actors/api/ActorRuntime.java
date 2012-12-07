package com.hazelcast.actors.api;

import java.util.Collection;
import java.util.Map;

public interface ActorRuntime {

    void send(ActorRef destination, Object msg);

    void send(ActorRef sender, Collection<ActorRef> destinations, Object msg);

    void send(ActorRef sender, ActorRef destination, Object msg);

    void exit(ActorRef target);

    /**
     * Repeatedly sends a notification message to an actor. Using this mechanism instead of an internal scheduler
     * simplifies the design and doesn't violate the 'single threaded access' of an actor.
     *
     * This functionality is useful if you want an actor to update its internal state frequently. A good example is
     * the jmx information of a JavaSoftwareProcess that every second could be read.
     *
     * @param destination the actor to send the notification to.
     * @param notification the notification.
     * @param delaysMs the delay between every notification.
     */
    void notify(ActorRef destination, Object notification, int delaysMs);

    /**
     * A uni directional link between a listener and a target; the listener will be notified of exit events
     * of the target.
     *
     * @param listener
     * @param target
     */
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
