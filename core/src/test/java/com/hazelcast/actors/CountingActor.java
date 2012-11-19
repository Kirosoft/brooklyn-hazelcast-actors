package com.hazelcast.actors;

import com.hazelcast.actors.api.Actor;
import com.hazelcast.actors.api.ActorContext;
import com.hazelcast.actors.api.ActorContextAware;
import com.hazelcast.actors.api.ActorRef;
import com.hazelcast.actors.utils.Util;

import java.util.concurrent.atomic.AtomicLong;

import static org.junit.Assert.fail;

public class CountingActor implements Actor, ActorContextAware {
    private final AtomicLong counter = new AtomicLong();
    private ActorContext actorContext;

    @Override
    public void setActorContext(ActorContext context) {
        this.actorContext = context;
    }

    @Override
    public void receive(Object msg, ActorRef sender) throws Exception {
        long count = counter.incrementAndGet();

        if (count % 10000000 == 0) {
            System.out.println(actorContext.self() + " is at: " + count);
        }
    }

    public void assertCountEventually(long count) {
        for (int k = 0; k < 600; k++) {
            if (counter.get() == count) return;
            Util.sleep(1000);

        }
        fail();
    }
}
