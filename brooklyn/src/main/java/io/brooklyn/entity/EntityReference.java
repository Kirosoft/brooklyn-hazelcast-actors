package io.brooklyn.entity;

import com.hazelcast.actors.api.ActorRef;
import com.hazelcast.core.PartitionAware;

import java.io.Serializable;

import static com.hazelcast.actors.utils.Util.notNegative;
import static com.hazelcast.actors.utils.Util.notNull;

public class EntityReference implements Serializable, PartitionAware {
    private final String id;
    private final Object partitionKey;
    private final int partitionId;

    public EntityReference(ActorRef ref) {
        this(ref.getId(), ref.getPartitionKey(), ref.getPartitionId());
    }

    public EntityReference(String id, Object partitionKey, int partitionId) {
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
        if (!(thatObj instanceof EntityReference)) return false;
        EntityReference that = (EntityReference) thatObj;
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

    public ActorRef toActorRef() {
        return new ActorRef(id, partitionKey, partitionId);
    }
}
