package com.hazelcast.actors;

import java.util.Map;

public interface ActorRuntime {

    void send(ActorRef destination, Object msg);

    void send(ActorRef sender, ActorRef destination, Object msg);

    void stop(ActorRef target);

    void repeat(ActorRef ref, Object msg, int delaysMs);

    ActorRef newActor(Class<? extends Actor> actorClass);

    ActorRef newActor(Class<? extends Actor> actorClass, Map<String, Object> properties);

    ActorRef newActor(Class<? extends Actor> actorClass, int partitionId);

    ActorRef newActor(ActorRecipe recipe);
}
