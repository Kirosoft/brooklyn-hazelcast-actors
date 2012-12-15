package com.hazelcast.actors.impl;

import com.hazelcast.actors.api.ActorRef;
import com.hazelcast.actors.utils.Util;
import com.hazelcast.spi.AbstractOperation;
import com.hazelcast.spi.Operation;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

class ExitOperation extends AbstractOperation {
    private String name;
    private ActorRef target;

    ExitOperation() {
    }

    ExitOperation(String name, ActorRef target) {
        this.name = name;
        this.target = target;
    }

    public void writeInternal(DataOutput out) throws IOException {
        out.writeUTF(name);
        byte[] targetBytes = Util.toBytes(target);
        out.writeInt(targetBytes.length);
        out.write(targetBytes);
    }

    public void readInternal(DataInput in) throws IOException {
        name = in.readUTF();
        int targetBytesLength = in.readInt();
        byte[] targetBytes = new byte[targetBytesLength];
        in.readFully(targetBytes);
        this.target = (ActorRef) Util.toObject(targetBytes);
    }

    @Override
    public void run() {
        ActorService actorService = (ActorService) getService();
        ActorPartitionContainer actorPartitionContainer = actorService.partitionContainers[getPartitionId()];
        ActorPartition actorPartition = actorPartitionContainer.getPartition(name);
        try {
            actorPartition.exit(target);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    @Override
    public boolean returnsResponse() {
        return false;
    }
}
