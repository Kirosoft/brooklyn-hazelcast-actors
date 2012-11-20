package com.hazelcast.actors.actors;

import com.hazelcast.actors.api.Actor;
import com.hazelcast.actors.api.ActorContext;
import com.hazelcast.actors.api.ActorContextAware;
import com.hazelcast.actors.api.ActorLifecycleAware;
import com.hazelcast.actors.api.ActorRecipe;
import com.hazelcast.actors.api.ActorRef;
import com.hazelcast.actors.api.ActorRuntime;
import com.hazelcast.core.HazelcastInstance;

import javax.naming.directory.BasicAttribute;

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
    public void activate() throws Exception {
        //no-op
    }

    @Override
    public void terminate() throws Exception {
        //no-op
    }

    @Override
    public void reactivate() throws Exception {
        //no-op
    }

    @Override
    public void suspend() throws Exception {
        //no-op
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
