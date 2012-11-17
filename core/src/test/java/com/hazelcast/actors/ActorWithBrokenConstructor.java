package com.hazelcast.actors;

import com.hazelcast.actors.api.Actor;
import com.hazelcast.actors.api.ActorRef;

public class ActorWithBrokenConstructor implements Actor {
    ActorWithBrokenConstructor() {
        throw new SomeException();
    }

    @Override
    public void receive(Object msg, ActorRef sender) throws Exception {
        //no-op
    }
}
