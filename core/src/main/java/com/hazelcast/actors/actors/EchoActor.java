package com.hazelcast.actors.actors;

import com.hazelcast.actors.api.Actor;
import com.hazelcast.actors.api.ActorRef;

/**
 * A very basic actor that prints out all messages it receives.
 */
public class EchoActor implements Actor {

    @Override
    public void receive(Object msg, ActorRef sender) throws Exception {
        System.out.println("Echo:" + msg);
    }
}
