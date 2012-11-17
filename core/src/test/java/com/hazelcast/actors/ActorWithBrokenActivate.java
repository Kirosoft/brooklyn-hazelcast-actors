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
    public void activate() throws Exception {
        throw new SomeException();
    }

    @Override
    public void suspend() throws Exception {
        //no-op
    }

    @Override
    public void reactivate() throws Exception {
        //no-op
    }

    @Override
    public void terminate() throws Exception {
       //no-op
    }
}
