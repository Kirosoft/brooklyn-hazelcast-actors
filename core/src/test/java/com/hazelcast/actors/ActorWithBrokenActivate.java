package com.hazelcast.actors;

import com.hazelcast.actors.api.Actor;
import com.hazelcast.actors.api.ActorLifecycleAware;
import com.hazelcast.actors.api.ActorRef;

public class ActorWithBrokenActivate implements ActorLifecycleAware, Actor {
    @Override
    public void receive(Object msg, ActorRef sender) throws Exception {
        //no-op
    }

    @Override
    public void onActivation() throws Exception {
        throw new SomeException();
    }

    @Override
    public void onSuspension() throws Exception {
        //no-op
    }

    @Override
    public void onReactivation() throws Exception {
        //no-op
    }

    @Override
    public void onExit() throws Exception {
        //no-op
    }
}
