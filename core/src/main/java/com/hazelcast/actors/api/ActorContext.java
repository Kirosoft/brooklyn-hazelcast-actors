package com.hazelcast.actors.api;

import com.hazelcast.core.HazelcastInstance;

import java.util.Set;


/**
 * The ActorContext gives the ability to an actor object, to interact with its 'surroundings': most likely the
 * ActorContainer.
 */
public interface ActorContext {

    ActorRef self();

    HazelcastInstance getHazelcastInstance();

    ActorRuntime getActorRuntime();

    ActorRecipe getRecipe();
}

