package com.hazelcast.actors.api;

import com.hazelcast.actors.utils.Util;

import java.io.Serializable;
import java.util.Collections;
import java.util.Map;

import static com.hazelcast.actors.utils.Util.notNull;

/**
 * The ActorRecipe is a recipe to instantiate a new actor. The same recipe can be used to create multiple actors.
 *
 * @param <A>
 */
public class ActorRecipe<A extends Actor> implements Serializable {
    private final String actorClass;
    private final int partitionId;
    private final Map<String, Object> properties;

    public ActorRecipe(Class<A> actorClass, int partitionId) {
        this(actorClass, partitionId, null);
    }

    public ActorRecipe(Class<A> actorClass, int partitionId, Map<String, Object> properties) {
        this.actorClass = Util.notNull(actorClass, "actorClass").getName();
        this.partitionId = partitionId;
        this.properties = properties;
    }

    public Class<A> getActorClass() {
        try {
            return (Class<A>)ActorRecipe.class.getClassLoader().loadClass(actorClass);
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    public int getPartitionId() {
        return partitionId;
    }

    public Map<String, Object> getProperties() {
        if (properties == null) {
            return Collections.EMPTY_MAP;
        } else {
            return properties;
        }
    }

    @Override
    public String toString() {
        return "ActorRecipe{" +
                "actorClass=" + actorClass +
                ", partitionId=" + partitionId +
                ", properties=" + properties +
                '}';
    }
}
