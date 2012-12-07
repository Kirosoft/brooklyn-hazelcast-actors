package com.hazelcast.actors.api;


/**
 * A callback interface an Actor can implement to be informed about certain lifecycle events.
 */
public interface ActorLifecycleAware {

    /**
     * Called when the Actor is activated.
     *
     * @throws Exception
     */
    void onActivation() throws Exception;

    /**
     * Called when the Actor is suspended.
     *
     * @throws Exception
     */
    void onSuspension() throws Exception;

    /**
     * Called when the Actor is reactivated.
     *
     * @throws Exception
     */
    void onReactivation() throws Exception;

    /**
     * Called when the Actor is terminated.
     *
     * @throws Exception
     */
    void onExit() throws Exception;
}
