package com.hazelcast.actors.api;

import java.util.Collection;
import java.util.concurrent.Future;


/**
 * The ActorRuntime is the 'gateway' an end user has to interact with the actor system. It is responsible for
 * managing actors, sending messages, terminating etc.
 * <p/>
 * http://erlang.org/doc/man/erlang.html
 *
 * @author Peter Veentjer.
 */
public interface ActorRuntime {

    void send(ActorRef destination, Object msg);

    void send(ActorRef sender, Collection<ActorRef> destinations, Object msg);

    void send(ActorRef sender, ActorRef destination, Object msg);

    Future ask(ActorRef sender, ActorRef destination, Object msg);

    /**
     * @param target
     */
    void exit(ActorRef target);

    /**
     * Repeatedly sends a notification message to an actor. Using this mechanism instead of an internal scheduler
     * simplifies the design and doesn't violate the 'single threaded access' of an actor.
     * <p/>
     * This functionality is useful if you want an actor to update its internal state frequently. A good example is
     * the jmx information of a JavaSoftwareProcess that every second could be read.
     *
     * @param destination  the actor to send the notification to.
     * @param notification the notification.
     * @param delaysMs     the delay between every notification.
     */
    void notify(ActorRef destination, Object notification, int delaysMs);

    /**
     * Sets up a bidirectional link between two actors. The link ensures that these 2 actors will receive each
     * others exit signals.
     * <p/>
     * If the bidirectional link already exist, the call is ignored.
     *
     * @param ref1
     * @param ref2
     * @throws NullPointerException if ref1 or ref2 is null.
     */
    void link(ActorRef ref1, ActorRef ref2);

    /**
     * Spawns a new Actor that runs somewhere in the system.
     * <p/>
     * After this method returns you get the guarantee that the actor can be found, e.g. for sending messages, unless
     * the actor has been destroyed.
     * <p/>
     * Creating the Actor is a synchronous operation, so the call waits till the actor has been fully constructed and
     * activated. If an exception is thrown during construction or activation, this exception will be propagated and
     * the Actor will be discarded.
     *
     * @return the ActorRef of the spawned Actor.
     * @throws RuntimeException
     * @throws NullPointerException if recipe is null.
     */
    ActorRef spawn(ActorRecipe recipe);


    /**
     * Spawns an new Actor (see {@link #spawn(ActorRecipe)} and then links it (see {@link #link(ActorRef, ActorRef)}.
     *
     * @param listener
     * @param recipe
     * @return the ActorRef of the spawned Actor.
     * @throws NullPointerException if listener or recipe is null.
     */
    ActorRef spawnAndLink(ActorRef listener, ActorRecipe recipe);
}
