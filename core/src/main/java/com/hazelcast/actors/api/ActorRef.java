package com.hazelcast.actors.api;

import com.hazelcast.core.PartitionAware;

import java.io.Serializable;

import static com.hazelcast.actors.utils.Util.notNegative;
import static com.hazelcast.actors.utils.Util.notNull;


/**
 * The ActorRef contains the id that uniquely identifies an Actor.
 *
 * The ActorRef also contains information about the partition the Actor is residing on. The partition will always be the
 * same, but of course the partition can be moved from one machine to another. The ActorRef also implements the
 * {@link PartitionAware} interface; this is useful for Hazelcast so it knows that this ActorRef contains the information
 * to determine the partition.
 *
 * @author Peter Veentjer.
 */
public final class ActorRef implements Serializable, PartitionAware {
    private final String id;
    private final Object partitionKey;
    private final int partitionId;

    public ActorRef(String id, Object partitionKey, int partitionId) {
        this.id = notNull(id, "id");
        this.partitionKey = notNull(partitionKey, "partitionKey");
        this.partitionId = notNegative(partitionId, "partitionId");
    }

    @Override
    public Object getPartitionKey() {
        return partitionKey;
    }

    public int getPartitionId() {
        return partitionId;
    }

    public String getId() {
        return id;
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
