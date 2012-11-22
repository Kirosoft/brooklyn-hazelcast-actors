package com.hazelcast.actors.api;

import com.hazelcast.actors.utils.Util;
import com.hazelcast.core.PartitionAware;

import java.io.Serializable;

public final class ActorRef implements Serializable, PartitionAware<Integer> {
    private final String id;
    private final int partitionId;

    public ActorRef(String id, int partitionId) {
        this.id = Util.notNull(id, "id");
        this.partitionId = partitionId;
    }

    @Override
    public Integer getPartitionKey() {
        return partitionId;
    }

    public String getId() {
        return id;
    }

    public int getPartitionId() {
        return partitionId;
    }

    @Override
    public boolean equals(Object thatObj) {
        if (this == thatObj) return true;
        if (!(thatObj instanceof ActorRef)) return false;
        ActorRef that = (ActorRef) thatObj;
        return this.id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }

    @Override
    public String toString() {
        return id;
    }
}
