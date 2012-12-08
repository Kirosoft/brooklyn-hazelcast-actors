package com.hazelcast.actors;

import com.hazelcast.actors.actors.AbstractActor;
import com.hazelcast.actors.api.ActorRef;
import com.hazelcast.actors.utils.Util;
import org.junit.Assert;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class TestActor extends AbstractActor {
    private List<Received> messages = new CopyOnWriteArrayList<>();

    public List<Received> getMessages() {
        return messages;
    }

    @Override
    public void receive(Object msg, ActorRef sender) throws Exception {
        messages.add(new Received(msg, sender));

        if (msg instanceof Exception) {
            throw (Exception) msg;
        }
    }

    public void assertReceivesEventually(Object msg) {
        for (int k = 0; k < 60; k++) {
            int size = messages.size();

            if (size == 0) {
                Util.sleep(1000);
                continue;
            }

            for (Received received : messages) {
                if (received.msg.equals(msg)) {
                    return;
                }
            }
        }

        Assert.fail(String.format("Message '%s' is not received", msg));
    }

    public void assertReceivesEventually(Object msg, ActorRef sender) {
        for (int k = 0; k < 60; k++) {
            int size = messages.size();

            if (size == 0) {
                Util.sleep(1000);
                continue;
            }

            for (Received received : messages) {
                if (received.msg.equals(msg)) {
                    Assert.assertEquals(sender, received.sender);
                    return;
                }
            }
        }

        Assert.fail(String.format("Message '%s' is not received", msg));
    }


    private static class Received {
        final Object msg;
        final ActorRef sender;

        private Received(Object msg, ActorRef sender) {
            this.msg = msg;
            this.sender = sender;
        }
    }
}
