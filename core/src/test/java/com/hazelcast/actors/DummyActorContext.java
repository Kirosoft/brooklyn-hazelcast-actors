package com.hazelcast.actors;

import com.hazelcast.actors.api.ActorContext;
import com.hazelcast.actors.api.ActorRecipe;
import com.hazelcast.actors.api.ActorRef;
import com.hazelcast.actors.api.ActorRuntime;
import com.hazelcast.core.HazelcastInstance;

public class DummyActorContext implements ActorContext {
    private final HazelcastInstance hzInstance;
    private final ActorRef self = new ActorRef("foo", 1);
    private final ActorRecipe recipe;
    private ActorRuntime actorRuntime;

    public DummyActorContext(HazelcastInstance hzInstance, ActorRecipe recipe, ActorRuntime actorRuntime) {
        this.hzInstance = hzInstance;
        this.recipe = recipe;
        this.actorRuntime = actorRuntime;
    }

    @Override
    public ActorRef self() {
        return self;
    }

    @Override
    public HazelcastInstance getHazelcastInstance() {
        return hzInstance;
    }

    @Override
    public ActorRuntime getActorRuntime() {
        return actorRuntime;
    }

    @Override
    public ActorRecipe getRecipe() {
        return recipe;
    }
}
