package com.hazelcast.actors.impl;

import com.hazelcast.actors.api.ActorRecipe;
import com.hazelcast.actors.api.ActorRef;
import com.hazelcast.actors.utils.Util;
import com.hazelcast.spi.Operation;
import com.hazelcast.spi.PartitionAwareOperation;
import com.hazelcast.spi.impl.AbstractNamedOperation;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

class CreateOperation  extends AbstractNamedOperation {
    private String name;
    private ActorRecipe actorRecipe;
    private Object partitionKey;
    private transient Object response;

    CreateOperation() {
    }

    CreateOperation(String name, ActorRecipe actorRecipe, Object partitionKey) {
        this.name = name;
        this.actorRecipe = actorRecipe;
        this.partitionKey = partitionKey;
    }

    public void writeInternal(DataOutput out) throws IOException {
        out.writeUTF(name);

        byte[] recipeBytes = Util.toBytes(actorRecipe);
        out.writeInt(recipeBytes.length);
        out.write(recipeBytes);

        byte[] partitionKeyBytes = Util.toBytes(partitionKey);
        out.writeInt(partitionKeyBytes.length);
        out.write(partitionKeyBytes);
    }

    public void readInternal(DataInput in) throws IOException {
        this.name = in.readUTF();
        int recipeBytesLength = in.readInt();

        byte[] recipeBytes = new byte[recipeBytesLength];
        in.readFully(recipeBytes);
        this.actorRecipe = (ActorRecipe) Util.toObject(recipeBytes);

        byte[] partitionKeyBytes = new byte[in.readInt()];
        in.readFully(partitionKeyBytes);
        this.partitionKey = Util.toObject(partitionKeyBytes);
    }

    @Override
    public void run() {
        ActorService actorService = (ActorService) getService();
        ActorPartitionContainer actorPartitionContainer = actorService.partitionContainers[getPartitionId()];
        ActorPartition actorPartition = actorPartitionContainer.getPartition(name);
        try {
            ActorRef ref = actorPartition.createActor(actorRecipe, partitionKey, getPartitionId());
            response = ref;
        } catch (Exception e) {
            response = e;
        }
    }

    @Override
    public Object getResponse() {
        return response;
    }

    @Override
    public boolean returnsResponse() {
        return true;
    }
}
