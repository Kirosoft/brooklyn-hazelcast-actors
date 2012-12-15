package com.hazelcast.actors.actors;

import com.hazelcast.actors.AbstractTest;
import com.hazelcast.actors.DummyActorContext;
import com.hazelcast.actors.TestUtils;
import com.hazelcast.actors.api.ActorRecipe;
import com.hazelcast.actors.api.ActorRef;
import com.hazelcast.actors.api.exceptions.UnprocessedException;
import com.hazelcast.actors.impl.ActorService;
import com.hazelcast.actors.impl.ActorServiceConfig;
import com.hazelcast.actors.impl.BasicActorFactory;
import com.hazelcast.config.Config;
import com.hazelcast.config.Services;
import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import java.net.URL;
import java.util.HashSet;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.fail;

public class DispatchingActorTest extends AbstractTest {

    @Test
    public void whenExactMatchAndNoSender() throws Exception {
        ActorRecipe recipe = new ActorRecipe(DispatchingTestActor.class, 0);
        DispatchingTestActor actor = new DispatchingTestActor();
        actor.setActorContext(new DummyActorContext(hzInstance, recipe, actorRuntime));

        Object msg = "foo";
        actor.receive(msg, null);
        assertSame(msg, actor.received);
    }

    @Test
    public void whenExactMatchAndSender() throws Exception {
        ActorRecipe recipe = new ActorRecipe(DispatchingTestActor.class, 0);
        DispatchingTestActor actor = new DispatchingTestActor();
        actor.setActorContext(new DummyActorContext(hzInstance, recipe, actorRuntime));

        Object msg = new URL("http://www.actor.bar");
        actor.receive(msg, null);
        assertSame(msg, actor.received);
        assertNull(actor.sender);

        msg = new URL("http://www.actor.bar");
        ActorRef ref = TestUtils.newRandomActorRef();
        actor.receive(msg, ref);
        assertSame(msg, actor.received);
        assertSame(ref, actor.sender);
    }

    @Test
    public void whenNoMatch() throws Exception {
        ActorRecipe recipe = new ActorRecipe(DispatchingTestActor.class, 0);
        DispatchingTestActor actor = new DispatchingTestActor();
        actor.setActorContext(new DummyActorContext(hzInstance, recipe, actorRuntime));

        Object msg = new HashSet<>();

        try {
            actor.receive(msg, null);
            fail();
        } catch (UnprocessedException expected) {
        }
    }

    @Test
    public void whenNoSpecificReceiveButSuperClassFound_thenSelectSuperclass() throws Exception {
        ActorRecipe recipe = new ActorRecipe(DispatchingTestActor.class, 0);
        DispatchingTestActor actor = new DispatchingTestActor();
        actor.setActorContext(new DummyActorContext(hzInstance, recipe, actorRuntime));

        Object msg = new Dog();

        actor.receive(msg, null);

        assertSame(msg, actor.received);
        assertEquals(Animal.class, actor.receivedClazz);
    }

    @Test
    public void whenSpecificReceiveAndLessSpecific_thenSpecific() throws Exception {
        ActorRecipe recipe = new ActorRecipe(DispatchingTestActor.class, 0);
        DispatchingTestActor actor = new DispatchingTestActor();
        actor.setActorContext(new DummyActorContext(hzInstance, recipe, actorRuntime));

        Object msg = new Animal();

        actor.receive(msg, null);

        assertSame(msg, actor.received);
        assertEquals(Animal.class, actor.receivedClazz);
    }

    @Test
    public void whenReceiveMethodThrowsException() throws Exception {
        ActorRecipe recipe = new ActorRecipe(DispatchingTestActor.class, 0);
        DispatchingTestActor actor = new DispatchingTestActor();
        actor.setActorContext(new DummyActorContext(hzInstance, recipe, actorRuntime));

        Object msg = new Exception();

        try {
            actor.receive(msg, null);
            fail();
        } catch (Exception expected) {
            assertSame(msg, expected);
        }
    }

    @Test
    public void whenReceiveObject_thenNotStuckInLoop() throws Exception {
        ActorRecipe recipe = new ActorRecipe(DispatchingTestActor.class, 0);
        DispatchingTestActor actor = new DispatchingTestActor();
        actor.setActorContext(new DummyActorContext(hzInstance, recipe, actorRuntime));

        Object msg = new Object();

        try {
            actor.receive(msg, null);
            fail();
        } catch (UnprocessedException expected) {
        }
    }

    @Test
    public void whenAmbiguousReceive() throws Exception {
        ActorRecipe recipe = new ActorRecipe(DispatchingTestActor.class, 0);
        DispatchingTestActor actor = new DispatchingTestActor();
        actor.setActorContext(new DummyActorContext(hzInstance, recipe, actorRuntime));

        Object msg = new FooBar();

        try {
            actor.receive(msg, null);
            fail();
        } catch (UnprocessedException expected) {
        }
    }

    @Test
    public void whenReceiveMethodReturnsNoneVoid() throws Exception {
        ActorRecipe recipe = new ActorRecipe(DispatchingTestActor.class, 0);
        DispatchingTestActor actor = new DispatchingTestActor();
        actor.setActorContext(new DummyActorContext(hzInstance, recipe, actorRuntime));

        Object msg = 1;

        try {
            actor.receive(msg, null);
            fail();
        } catch (UnprocessedException expected) {
        }
    }

    @Test
    public void whenReceiveMethodIsStatic() throws Exception {
        ActorRecipe recipe = new ActorRecipe(DispatchingTestActor.class, 0);
        DispatchingTestActor actor = new DispatchingTestActor();
        actor.setActorContext(new DummyActorContext(hzInstance, recipe, actorRuntime));

        Object msg = true;

        try {
            actor.receive(msg, null);
            fail();
        } catch (UnprocessedException expected) {
        }
    }

    @Test
    public void whenReceiveMethodSecondArgumentIsNotActorRef() throws Exception {
        ActorRecipe recipe = new ActorRecipe(DispatchingTestActor.class, 0);
        DispatchingTestActor actor = new DispatchingTestActor();
        actor.setActorContext(new DummyActorContext(hzInstance, recipe, actorRuntime));

        Object msg = (double) 1;

        try {
            actor.receive(msg, null);
            fail();
        } catch (UnprocessedException expected) {
        }
    }


    public static class DispatchingTestActor extends DispatchingActor {
        private Object received;
        private ActorRef sender;
        private Class receivedClazz;

        public void receive(String msg) {
            received = msg;
            receivedClazz = String.class;
        }

        public void receive(URL url, ActorRef sender) {
            this.received = url;
            this.sender = sender;
            receivedClazz = URL.class;
        }

        public void receive(Exception e) throws Exception {
            received = e;
            receivedClazz = Exception.class;
            throw e;
        }

        public void receive(Organism msg) {
            this.received = msg;
            receivedClazz = Organism.class;
        }

        public void receive(Animal msg) {
            this.received = msg;
            this.receivedClazz = Animal.class;
        }

        public void receive(Foo msg) {
            this.received = msg;
            this.receivedClazz = Foo.class;

        }

        public void receive(Bar msg) {
            this.received = msg;
            this.receivedClazz = Foo.class;
        }

        public int receive(Integer msg) {
            return 1;
        }

        public static void receive(Boolean msg) {
        }

        public static void receive(Double msg, Double nonse) {
        }
    }

    static class Organism {
    }

    static class Animal extends Organism {
    }

    static class Dog extends Animal {
    }

    static interface Foo {
    }

    static interface Bar {
    }

    static class FooBar implements Foo, Bar {
    }
}
