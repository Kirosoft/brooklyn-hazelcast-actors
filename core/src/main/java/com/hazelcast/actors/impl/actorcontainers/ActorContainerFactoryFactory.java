package com.hazelcast.actors.impl.actorcontainers;

import com.hazelcast.actors.api.Actor;
import com.hazelcast.actors.api.ActorFactory;
import com.hazelcast.actors.api.ActorRuntime;
import com.hazelcast.core.IMap;
import com.hazelcast.spi.impl.NodeServiceImpl;

public interface ActorContainerFactoryFactory<A extends Actor> {

    ActorContainerFactory<A> newFactory(ActorFactory actorFactory, ActorRuntime actorRuntime, IMap monitorMap,
                                                       NodeServiceImpl nodeService);
}
