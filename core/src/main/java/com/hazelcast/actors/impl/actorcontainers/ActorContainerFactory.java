package com.hazelcast.actors.impl.actorcontainers;

import com.hazelcast.actors.api.Actor;
import com.hazelcast.actors.api.ActorRecipe;
import com.hazelcast.actors.api.ActorRef;
import com.hazelcast.core.IMap;

import java.util.Set;

public interface ActorContainerFactory<A extends Actor> {

    void init(IMap<ActorRef, Set<ActorRef>> monitorMap);

    ActorContainer<A> newContainer(ActorRef actorRef, ActorRecipe<A> recipe);
}
