package com.hazelcast.actors.actors;

import com.hazelcast.actors.api.*;
import com.hazelcast.core.HazelcastInstance;

import static com.hazelcast.actors.utils.Util.notNull;

public abstract class AbstractActor implements Actor,
        ActorLifecycleAware, ActorContextAware {

    private ActorContext actorContext;

    @Override
    public final void setActorContext(ActorContext actorContext) {
        this.actorContext = notNull(actorContext, "actorContext");
    }

    public final void send(ActorRef destination, Object msg) {
        getActorContext().getActorRuntime().send(self(), destination, msg);
    }

    @Override
    public void onActivation() throws Exception {
        //no-op
    }

    @Override
    public void onExit() throws Exception {
        //no-op
    }

    @Override
    public void onReactivation() throws Exception {
        //no-op
    }

    @Override
    public void onSuspension() throws Exception {
        //no-op
    }

    /**
     * Spawns a new Actor and links it to the current Actor.
     *
     * @param actorClass
     * @return
     */
    public final ActorRef spawnAndLink(Class<? extends Actor> actorClass) {
        notNull(actorClass, "actorClass");

        ActorContext actorContext = getActorContext();
        ActorRecipe actorRecipe = new ActorRecipe(actorClass,
                actorContext.getRecipe().getPartitionKey(),
                null);
        return actorContext.getActorRuntime().spawnAndLink(self(), actorRecipe);
    }

    public final void link(ActorRef otherActor){
        getActorRuntime().link(self(),otherActor);
    }

    public final ActorContext getActorContext() {
        if (actorContext == null) {
            throw new IllegalStateException("actorContext has not yet been set");
        }
        return actorContext;
    }

    public final ActorRef self() {
        return getActorContext().self();
    }

    public final ActorRecipe getRecipe() {
        return getActorContext().getRecipe();
    }

    public final ActorRuntime getActorRuntime() {
        return getActorContext().getActorRuntime();
    }

    public final HazelcastInstance getHzInstance() {
        return getActorContext().getHazelcastInstance();
    }
}
