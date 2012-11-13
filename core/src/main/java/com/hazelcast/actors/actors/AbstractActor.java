package com.hazelcast.actors.actors;

import com.hazelcast.actors.api.Actor;
import com.hazelcast.actors.api.ActorLifecycleAware;
import com.hazelcast.actors.api.ActorRecipe;
import com.hazelcast.actors.api.ActorRef;
import com.hazelcast.actors.api.ActorRefAware;
import com.hazelcast.actors.api.ActorRuntime;
import com.hazelcast.actors.api.ActorSystemAware;
import com.hazelcast.actors.api.UnprocessedException;
import com.hazelcast.actors.utils.Util;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.HazelcastInstanceAware;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public abstract class AbstractActor implements Actor,
        HazelcastInstanceAware, ActorSystemAware, ActorLifecycleAware, ActorRefAware {

    private HazelcastInstance hzInstance;
    private ActorRuntime actorRuntime;
    private ActorRef self;
    private ActorRecipe recipe;

    public ActorRef self() {
        return self;
    }

    public ActorRecipe getRecipe() {
        return recipe;
    }

    @Override
    public void setActorRef(ActorRef actorRef) {
        this.self = actorRef;
    }

    @Override
    public final void setHazelcastInstance(HazelcastInstance hzInstance) {
        this.hzInstance = hzInstance;
    }

    @Override
    public final void setActorRuntime(ActorRuntime actorRuntime) {
        this.actorRuntime = actorRuntime;
    }

    public final void send(Object msg) {
        this.actorRuntime.send(self(), msg);
    }

    @Override
    public void init(ActorRecipe recipe)throws Exception{
        this.recipe = recipe;
    }

    @Override
    public void terminate()throws Exception {
        //no-op
    }

    @Override
    public void reactivate() throws Exception{
        //no-op
    }

    @Override
    public void suspend()throws Exception {
        //no-op
    }

    public final ActorRuntime getActorRuntime() {
        return actorRuntime;
    }

    public final HazelcastInstance getHzInstance() {
        return hzInstance;
    }
}
