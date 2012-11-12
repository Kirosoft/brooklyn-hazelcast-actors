package com.hazelcast.actors;

public class EchoActor implements Actor {
    @Override
    public void receive(Object msg, ActorRef sender) throws Exception {
        System.out.println("Echo:"+msg);
    }
}
