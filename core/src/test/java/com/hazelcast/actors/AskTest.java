package com.hazelcast.actors;

import com.hazelcast.actors.api.Actor;
import com.hazelcast.actors.api.ActorRecipe;
import com.hazelcast.actors.api.ActorRef;
import org.junit.Test;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class AskTest extends AbstractTest {

    @Test
    public void test() throws ExecutionException, InterruptedException {
        ActorRef ref = actorRuntime.spawn(new ActorRecipe(AskActor.class));
        Object msg = "foo";
        Future f = actorRuntime.ask(null, ref, msg);
        f.get();
    }

    @Test
    public void testWhenThrowsException() throws InterruptedException {
        ActorRef ref = actorRuntime.spawn(new ActorRecipe(ExceptionThrowingActor.class));
        Object msg = "foo";
        Future f = actorRuntime.ask(null, ref, msg);
        try {
            f.get();
            fail();
        } catch (ExecutionException e) {
            assertTrue(e.getCause() instanceof SomeException);
        }
    }

    static class ExceptionThrowingActor implements Actor {
        @Override
        public void receive(Object msg, ActorRef sender) throws Exception {
            throw new SomeException();
        }
    }

    static class AskActor implements Actor {
        @Override
        public void receive(Object msg, ActorRef sender) throws Exception {
            Thread.sleep(5000);
        }
    }
}
