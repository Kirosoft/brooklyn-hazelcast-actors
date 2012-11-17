package com.hazelcast.actors.api;

import com.hazelcast.actors.utils.Util;

import java.io.Serializable;

public final class ActorRef implements Serializable {
    private final String id;
    private final int partitionId;

    public ActorRef(String id, int partitionId) {
        this.id = Util.notNull(id, "id");
        this.partitionId = partitionId;
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
        return "ActorRef{" +
                "id='" + id + '\'' +
                ", partitionId=" + partitionId +
                '}';
    }
}
