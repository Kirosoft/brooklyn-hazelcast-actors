package com.hazelcast.actors.api;

import com.hazelcast.actors.utils.Util;

import java.io.Serializable;
import java.util.Collections;
import java.util.Map;

/**
 * The ActorRecipe is a recipe to instantiate a new actor. The same recipe can be used to create multiple actors.
 *
 * @param <A>
 */
public class ActorRecipe<A extends Actor> implements Serializable {
    private final String actorClass;
    private final Object partitionKey;
    private final Map<String, Object> properties;
    private final ActorRef parent;

    public ActorRecipe(Class<A> actorClass, Object partitionKey) {
        this(actorClass, partitionKey, null);
    }

    public ActorRecipe(Class<A> actorClass, Object partitionKey, Map<String, Object> properties) {
        this(actorClass, null, partitionKey, properties);
    }

    public ActorRecipe(Class<A> actorClass, ActorRef parent, Object partitionKey, Map<String, Object> properties) {
        this.actorClass = Util.notNull(actorClass, "actorClass").getName();
        this.partitionKey = partitionKey;
        this.properties = properties;
        this.parent = parent;
    }

    /**
     * Returns parent actor that owns this child.
     *
     * @return the parent Actor.
     */
    public ActorRef getParent() {
        return parent;
    }

    public Class<A> getActorClass() {
        try {
            return (Class<A>) ActorRecipe.class.getClassLoader().loadClass(actorClass);
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    public Object getPartitionKey() {
        return partitionKey;
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
                ", partitionKey=" + partitionKey +
                ", properties=" + properties +
                ", parent="+parent+
                '}';
    }
}
