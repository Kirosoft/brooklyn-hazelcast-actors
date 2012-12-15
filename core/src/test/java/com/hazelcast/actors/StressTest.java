package com.hazelcast.actors;

import com.hazelcast.actors.actors.AbstractActor;
import com.hazelcast.actors.api.ActorRecipe;
import com.hazelcast.actors.api.ActorRef;
import com.hazelcast.actors.utils.Util;
import org.junit.Test;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.fail;

public class StressTest extends AbstractTest {
    private static final AtomicInteger failureCount = new AtomicInteger();

    @Test
    public void singleActor() {
        ActorRef ref = actorRuntime.spawn(new ActorRecipe(DetectingActor.class));

        int count = 1000;
        for (int k = 0; k < count; k++) {
            actorRuntime.send(ref, "");
        }

        DetectingActor actor = (DetectingActor) actorRuntime.getActor(ref);
        actor.assertCountEventually(count);
    }

    @Test
    public void multipleActors() {
        int actorCount = 10;
        ActorRef[] refs = new ActorRef[actorCount];
        for (int k = 0; k < refs.length; k++) {
            refs[k] = actorRuntime.spawn(new ActorRecipe(DetectingActor.class));
        }

        int count = 10000;
        for (int k = 0; k < count; k++) {
            for (ActorRef ref : refs) {
                actorRuntime.send(ref, "");
            }
        }

        for (ActorRef ref : refs) {
            DetectingActor actor = (DetectingActor) actorRuntime.getActor(ref);
            actor.assertCountEventually(count);
        }
    }

    private static class DetectingActor extends AbstractActor {
        private final AtomicInteger concurrentAccessCounter = new AtomicInteger();
        private final AtomicInteger counter = new AtomicInteger();

        public DetectingActor() {
        }

        @Override
        public void receive(Object msg, ActorRef sender) throws Exception {
            if (concurrentAccessCounter.incrementAndGet() > 0) {
                failureCount.incrementAndGet();
            }

            Util.sleep(5);

            int c = counter.incrementAndGet();

            if (c % 100 == 0) {
                System.out.println(self() + " at: " + c);
            }

            concurrentAccessCounter.decrementAndGet();
        }

        public void assertCountEventually(long count) {
            for (int k = 0; k < 600; k++) {
                if (counter.get() == count) return;
                Util.sleep(1000);

            }
            fail();
        }
    }


}