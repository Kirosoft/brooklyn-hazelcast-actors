package com.hazelcast.actors.impl;

import com.hazelcast.actors.api.Actor;
import com.hazelcast.actors.api.exceptions.ActorInstantiationException;
import com.hazelcast.actors.api.ActorRecipe;
import com.hazelcast.actors.api.ActorRef;
import com.hazelcast.actors.api.Injected;
import com.hazelcast.actors.utils.MutableMap;
import org.junit.Test;

import java.util.LinkedList;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

public class BasicActorFactoryTest {

    @Test
    public void autowiredFieldIsInjected() {
        String value = "peter";
        Map<String, Object> dependencies = MutableMap.map("autowiredField", value);
        BasicActorFactory factory = new BasicActorFactory(dependencies);
        SomeActor actor = factory.newActor(new ActorRecipe<>(SomeActor.class, 1));

        assertNotNull(actor);
        assertEquals(value, actor.autowiredField);
    }

    @Test
    public void nonAutowiredFieldIsNotInjected() {
        String value = "peter";
        Map<String, Object> dependencies = MutableMap.map("autowiredField", value, "nonAutowiredField", "nonsense");
        BasicActorFactory factory = new BasicActorFactory(dependencies);
        SomeActor actor = factory.newActor(new ActorRecipe<>(SomeActor.class, 1));

        assertNotNull(actor);
        assertEquals(value, actor.autowiredField);
        assertNull(actor.nonAutowiredField);
    }

    @Test(expected = ActorInstantiationException.class)
    public void noArgConstructorIsMissing() {
        BasicActorFactory factory = new BasicActorFactory();
        factory.newActor(new ActorRecipe<>(ActorWithMissingNoArgConstructor.class, 1));
    }

    private static class ActorWithMissingNoArgConstructor implements Actor {
        private ActorWithMissingNoArgConstructor(int dontcare) {
        }

        @Override
        public void receive(Object msg, ActorRef sender) throws Exception {
        }
    }

    @Test(expected = ActorInstantiationException.class)
    public void missingAutowiredDependency() {
        Map<String, Object> dependencies = MutableMap.map();
        BasicActorFactory factory = new BasicActorFactory(dependencies);
        factory.newActor(new ActorRecipe<>(SomeActor.class, 1));
    }

    private static class SomeActor implements Actor {
        @Injected
        private String autowiredField;

        private String nonAutowiredField;

        @Override
        public void receive(Object msg, ActorRef sender) throws Exception {

        }
    }

    @Test(expected = ActorInstantiationException.class)
    public void staticAutowiredFieldCantBeInjected() {
        Map<String, Object> dependencies = MutableMap.map("autowiredField", "nonsense");
        BasicActorFactory factory = new BasicActorFactory(dependencies);
        factory.newActor(new ActorRecipe<>(SomeActorWithStaticAutowiredField.class, 1));
    }

    private static class SomeActorWithStaticAutowiredField implements Actor {
        @Injected
        private static String autowiredField;

        @Override
        public void receive(Object msg, ActorRef sender) throws Exception {

        }
    }

    @Test(expected = ActorInstantiationException.class)
    public void finalAutowiredFieldCantBeInjected() {
        Map<String, Object> dependencies = MutableMap.map("autowiredField", "nonsense");
        BasicActorFactory factory = new BasicActorFactory(dependencies);
        factory.newActor(new ActorRecipe<>(SomeActorWithFinalAutowiredField.class, 1));
    }

    private static class SomeActorWithFinalAutowiredField implements Actor {
        @Injected
        private final String autowiredField = "nonsense";

        @Override
        public void receive(Object msg, ActorRef sender) throws Exception {

        }
    }

    @Test(expected = ActorInstantiationException.class)
    public void fieldTypeMismatch() {
        Map<String, Object> dependencies = MutableMap.map("autowiredField", new LinkedList());
        BasicActorFactory factory = new BasicActorFactory(dependencies);
        factory.newActor(new ActorRecipe<>(SomeActor.class, 1));
    }
}
