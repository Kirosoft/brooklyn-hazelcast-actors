package com.hazelcast.actors.api;

import com.hazelcast.actors.utils.Util;

import java.io.Serializable;
import java.util.Collections;
import java.util.Map;

import static com.hazelcast.actors.utils.Util.notNull;

public class ActorRecipe implements Serializable {
    private final Class<? extends Actor> actorClass;
    private final int partitionId;
    private final Map<String, Object> properties;

    public ActorRecipe(Class<? extends Actor> actorClass, int partitionId) {
        this(actorClass, partitionId, null);
    }

    public ActorRecipe(Class<? extends Actor> actorClass, int partitionId, Map<String, Object> properties) {
        this.actorClass = Util.notNull(actorClass, "actorClass");
        this.partitionId = partitionId;
        this.properties = properties;
    }

    public Class<? extends Actor> getActorClass() {
        return actorClass;
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
