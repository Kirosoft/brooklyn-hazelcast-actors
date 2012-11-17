package com.hazelcast.actors.api;

import com.hazelcast.core.HazelcastInstance;

public interface ActorContext {

    ActorRef self();

    HazelcastInstance getHazelcastInstance();

    ActorRuntime getActorRuntime();

    ActorRecipe getRecipe();
}

