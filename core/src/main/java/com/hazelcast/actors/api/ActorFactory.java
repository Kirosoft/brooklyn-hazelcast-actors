package com.hazelcast.actors.api;

/**
 * A Factory for creating {@link Actor} instances based on an {@link ActorRecipe}.
 *
 * By using a Factory you have the flexibility to control Actor instance creation completely,
 * e.g. you could create a Guice/Spring based on that does a lookup.
 *
 * @author Peter Veentjer.
 */
public interface ActorFactory {

    /**
     * Creates a new Actor instance based on an ActorRecipe.
     *
     * @param recipe the ActorRecipe that contains all information about what kind of Actor to create
     * @return the created Actor. Should never be null.
     */
    <A extends Actor> A newActor(ActorRecipe<A> recipe);
}
