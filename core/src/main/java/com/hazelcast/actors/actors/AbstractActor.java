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

    public void receive(Callback callback, ActorRuntime sender) {
        callback.run(this);
    }

    @Override
    public void receive(Object msg, ActorRef sender) throws Exception {
        Method receiveMethod = Util.findReceiveMethod(getClass(), msg.getClass());
        if (receiveMethod == null) {
            throw new UnprocessedException("No receive method found on actor.class: " + getClass().getName() +
                    " and message.class:" + msg.getClass().getName());
        }

        try {
            receiveMethod.invoke(this, msg, sender);
        } catch (IllegalAccessException e) {
            //This will not be thrown since we make the receiveMethod accessible
            throw new RuntimeException(e);
        } catch (InvocationTargetException e) {
            Throwable cause = e.getTargetException();
            if (cause instanceof Exception) {
                throw (Exception) cause;
            } else if (cause instanceof Error) {
                throw (Error) cause;
            } else {
                throw new RuntimeException(e);
            }
        }
    }

    @Override
    public void init(ActorRecipe recipe) {
        this.recipe = recipe;
    }

    @Override
    public void terminate() {
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
