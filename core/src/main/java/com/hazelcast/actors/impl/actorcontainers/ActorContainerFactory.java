package com.hazelcast.actors.impl.actorcontainers;

import com.hazelcast.actors.api.Actor;
import com.hazelcast.actors.api.ActorRecipe;
import com.hazelcast.actors.api.ActorRef;

public interface ActorContainerFactory<A extends Actor> {

    ActorContainer<A> newContainer(ActorRef actorRef, ActorRecipe<A> recipe);
}
