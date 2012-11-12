package com.hazelcast.actors.api;

public interface Actor {

    void receive(Object msg, ActorRef sender) throws Exception;
}
