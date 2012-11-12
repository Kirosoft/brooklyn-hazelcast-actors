package com.hazelcast.actors.api;

import com.hazelcast.actors.api.Actor;

import java.io.Serializable;
import java.util.Collections;
import java.util.Map;

import static com.hazelcast.actors.Util.notNull;

public class ActorRecipe implements Serializable {
    public final Class<? extends Actor> actorClass;
    public final int partitionId;
    private Map<String, Object> properties;

    public ActorRecipe(Class<? extends Actor> actorClass, int partitionId) {
        this(actorClass,partitionId,null);
    }

    public ActorRecipe(Class<? extends Actor> actorClass, int partitionId, Map<String, Object> properties) {
        this.actorClass = Util.notNull(actorClass, "actorClass");
        this.partitionId = partitionId;
        this.properties = properties;
    }

    public Map<String, Object> getProperties() {
        if (properties == null) {
            return Collections.EMPTY_MAP;
        } else {
            return properties;
        }
    }
}
