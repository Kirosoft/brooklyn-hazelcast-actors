package com.hazelcast.actors;

import com.hazelcast.actors.actors.AbstractActor;
import com.hazelcast.actors.api.ActorRef;
import com.hazelcast.actors.utils.Util;
import org.junit.Assert;

import java.util.List;
import java.util.Vector;

public class TestActor extends AbstractActor {
    private List messages = new Vector();

    public List getMessages() {
        return messages;
    }

    @Override
    public void receive(Object msg, ActorRef sender) throws Exception {
        messages.add(msg);

        if (msg instanceof Exception) {
            throw (Exception) msg;
        }
    }

    public void assertReceivesEventually(Object msg) {
        for (int k = 0; k < 60; k++) {
            int size = messages.size();

            if (size > 0 && messages.get(size - 1).equals(msg)) {
                return;
            }

            Util.sleep(1000);
        }

        Assert.fail(String.format("Message '%s' is not received", msg));
    }
}
