package example.hazelcast.actors;

import java.io.Serializable;

import static example.hazelcast.Util.notNull;

public final class ActorRef implements Serializable {
    public final String id;
    public final int partitionId;

    public ActorRef(String id, int partitionId) {
        this.id = notNull(id, "id");
        this.partitionId = partitionId;
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
