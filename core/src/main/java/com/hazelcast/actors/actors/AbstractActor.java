package com.hazelcast.actors.actors;

import com.hazelcast.actors.api.Actor;
import com.hazelcast.actors.api.ActorContext;
import com.hazelcast.actors.api.ActorContextAware;
import com.hazelcast.actors.api.ActorLifecycleAware;
import com.hazelcast.actors.api.ActorRecipe;
import com.hazelcast.actors.api.ActorRef;
import com.hazelcast.actors.api.ActorRuntime;
import com.hazelcast.core.HazelcastInstance;

import static com.hazelcast.actors.utils.Util.notNull;

public abstract class AbstractActor implements Actor,
        ActorLifecycleAware, ActorContextAware {

    private ActorContext actorContext;

    @Override
    public final void setActorContext(ActorContext actorContext) {
        this.actorContext = notNull(actorContext, "actorContext");
    }

    public final void send(Object msg) {
        assertActorContextNotNull();
        actorContext.getActorRuntime().send(self(), msg);
    }

    @Override
    public void init() throws Exception {
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

    public final ActorRef self() {
        assertActorContextNotNull();
        return actorContext.self();
    }

    public final ActorRecipe getRecipe() {
        assertActorContextNotNull();
        return actorContext.getRecipe();
    }

    public final ActorRuntime getActorRuntime() {
        assertActorContextNotNull();
        return actorContext.getActorRuntime();
    }

    public final HazelcastInstance getHzInstance() {
        assertActorContextNotNull();
        return actorContext.getHazelcastInstance();
    }

    private void assertActorContextNotNull() {
        if (actorContext == null) {
            throw new IllegalStateException("actorContext has not yet been set");
        }
    }
}
