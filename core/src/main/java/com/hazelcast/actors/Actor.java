package com.hazelcast.actors;

public interface Actor {

    void receive(Object msg, ActorRef sender) throws Exception;
}
