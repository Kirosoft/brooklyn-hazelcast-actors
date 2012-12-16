package com.hazelcast.actors;

import com.hazelcast.actors.api.Actor;
import com.hazelcast.actors.api.ActorRecipe;
import com.hazelcast.actors.api.ActorRef;
import org.junit.Test;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;

import static com.hazelcast.actors.TestUtils.assertExceptionContainsLocalSeparator;
import static org.junit.Assert.*;

public class AskTest extends AbstractTest {

    @Test
    public void whenSuccess() throws ExecutionException, InterruptedException {
        ActorRef ref = actorRuntime.spawn(new ActorRecipe(AskActor.class));
        AskActor actor = (AskActor) actorRuntime.getActor(ref);
        Object msg = "foo";
        Future f = actorRuntime.ask(null, ref, msg);
        assertNotNull(f);
        assertFalse(actor.finished.get());

        //wait till the message is processed.
        f.get();
        assertTrue(actor.finished.get());
    }

    @Test
    public void whenExceptionThrownWhileProcessing() throws InterruptedException {
        ActorRef ref = actorRuntime.spawn(new ActorRecipe(ExceptionThrowingActor.class));
        Object msg = "foo";
        Future f = actorRuntime.ask(null, ref, msg);
        assertNotNull(f);
        try {
            f.get();
            fail();
        } catch (ExecutionException e) {
            Throwable cause = e.getCause();
            assertTrue(cause instanceof SomeException);
            assertExceptionContainsLocalSeparator(cause);
        }
    }

    static class ExceptionThrowingActor implements Actor {
        @Override
        public void receive(Object msg, ActorRef sender) throws Exception {
            throw new SomeException();
        }
    }

    static class AskActor implements Actor {
        private final AtomicBoolean finished = new AtomicBoolean();

        @Override
        public void receive(Object msg, ActorRef sender) throws Exception {
            Thread.sleep(5000);
            finished.set(true);
        }
    }
}
