package com.hazelcast.actors.impl.actorcontainers;

import com.hazelcast.actors.api.Actor;
import com.hazelcast.actors.api.ActorFactory;
import com.hazelcast.actors.api.ActorRef;
import com.hazelcast.actors.api.ActorRuntime;
import com.hazelcast.spi.impl.NodeServiceImpl;

public interface ActorContainer<A extends Actor> {

    ActorRef getActorRef();

    A getActor();

    A activate(ActorRuntime actorRuntime, NodeServiceImpl nodeService, ActorFactory actorFactory);

    void terminate() throws Exception;

    void post(ActorRef sender, Object message) throws InterruptedException;
}
