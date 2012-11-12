package com.hazelcast.actors.actors;

import com.hazelcast.actors.api.Actor;
import com.hazelcast.actors.api.ActorLifecycleAware;
import com.hazelcast.actors.api.ActorRecipe;
import com.hazelcast.actors.api.ActorRef;
import com.hazelcast.actors.api.ActorRefAware;
import com.hazelcast.actors.api.ActorRuntime;
import com.hazelcast.actors.api.ActorSystemAware;
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

    @Override
    public void receive(Object msg, ActorRef sender) {
        Method receiveMethod = Util.findReceiveMethod(getClass(), msg.getClass());
        if (receiveMethod == null) {
            throw new RuntimeException("No receive method found on actor.class: " + getClass().getName() +
                    " and message.class:" + msg.getClass().getName());
        }

        try {
            receiveMethod.invoke(this, msg, sender);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        } catch (InvocationTargetException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void init(ActorRecipe recipe) {
        this.recipe = recipe;
    }

    @Override
    public void stop() {
        //no-op
    }

    @Override
    public void reactivate() {
        //no-op
    }

    @Override
    public void suspend() {
        //no-op
    }

    public final ActorRuntime getActorRuntime() {
        return actorRuntime;
    }

    public final HazelcastInstance getHzInstance() {
        return hzInstance;
    }
}
