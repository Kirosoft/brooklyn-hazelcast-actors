package com.hazelcast.actors.service;

import com.hazelcast.actors.api.Actor;
import com.hazelcast.actors.api.ActorLifecycleAware;
import com.hazelcast.actors.api.ActorRecipe;
import com.hazelcast.actors.api.ActorRef;
import com.hazelcast.actors.api.ActorRefAware;
import com.hazelcast.actors.api.ActorRuntime;
import com.hazelcast.actors.api.ActorSystemAware;
import com.hazelcast.actors.api.Autowired;
import com.hazelcast.actors.api.MessageDeliveryFailure;
import com.hazelcast.actors.utils.Util;
import com.hazelcast.core.HazelcastInstanceAware;
import com.hazelcast.nio.DataSerializable;
import com.hazelcast.spi.impl.NodeServiceImpl;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;


public final class ActorContainer implements DataSerializable {
    private Actor actor;
    private BlockingQueue mailbox = new LinkedBlockingQueue();
    private ActorRef ref;
    private ActorRecipe recipe;
    private Map<ActorRef, Set<ActorRef>> monitorMap;
    private ActorRuntime actorRuntime;

    public ActorContainer(ActorRecipe recipe, ActorRef actorRef) {
        this.recipe = Util.notNull(recipe, "recipe");
        this.ref = Util.notNull(actorRef, "ref");
    }

    public Actor getActor() {
        return actor;
    }

    public void init(ActorRuntime actorRuntime, NodeServiceImpl nodeService, Map<String, Object> dependencies) {
        this.actorRuntime = actorRuntime;
        monitorMap = nodeService.getNode().hazelcastInstance.getMap("monitorMap");

        try {
            Constructor<? extends Actor> constructor = recipe.getActorClass().getConstructor();
            constructor.setAccessible(true);
            actor = constructor.newInstance();
        } catch (InstantiationException e) {
            throw new RuntimeException(e);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        } catch (NoSuchMethodException e) {
            throw new RuntimeException(e);
        } catch (InvocationTargetException e) {
            throw new RuntimeException(e);
        }

        initAutowiredFields(dependencies);

        if (actor instanceof HazelcastInstanceAware) {
            ((HazelcastInstanceAware) actor).setHazelcastInstance(nodeService.getNode().hazelcastInstance);
        }

        if (actor instanceof ActorSystemAware) {
            ((ActorSystemAware) actor).setActorRuntime(actorRuntime);
        }

        if (actor instanceof ActorRefAware) {
            ((ActorRefAware) actor).setActorRef(ref);
        }

        if (actor instanceof ActorLifecycleAware) {
            ((ActorLifecycleAware) actor).init(recipe);
        }
    }

    public void initAutowiredFields(Map<String, Object> dependencies) {
        Class clazz = actor.getClass();
        for (; ; ) {
            Field[] fields = clazz.getDeclaredFields();
            for (Field field : fields) {
                if (field.isAnnotationPresent(Autowired.class)) {
                    Object dependency = dependencies.get(field.getName());
                    field.setAccessible(true);
                    try {
                        field.set(actor, dependency);
                    } catch (IllegalAccessException e) {
                        throw new RuntimeException(e);
                    }
                }
            }

            clazz = clazz.getSuperclass();
            if (clazz == null) {
                return;
            }
        }
    }

    public void terminate() {
        if (actor instanceof ActorLifecycleAware) {
            ((ActorLifecycleAware) actor).terminate();
        }
    }

    public void post(ActorRef sender, Object message) throws InterruptedException {
        if (sender == null) {
            mailbox.put(message);
        } else {
            mailbox.put(new Message(message, sender));
        }
    }

    public synchronized void processMessage() throws InterruptedException {
        Object m = mailbox.take();
        ActorRef sender;
        Object message;
        if (m instanceof Message) {
            message = ((Message) m).content;
            sender = ((Message) m).sender;
        } else {
            message = m;
            sender = null;
        }

        try {
            actor.receive(message, sender);
        } catch (Exception e) {
            e.printStackTrace();

            if (sender != null) {
                MessageDeliveryFailure messageDeliveryFailure = new MessageDeliveryFailure(ref, sender, e);
                actorRuntime.send(sender, messageDeliveryFailure);
            }

            Set<ActorRef> listeners = monitorMap.get(ref);
            if (listeners != null && !listeners.isEmpty()) {
                MessageDeliveryFailure messageDeliveryFailure = new MessageDeliveryFailure(ref, sender, e);
                for (ActorRef listener : listeners) {
                    //if the sender also is a monitor, we don't want to send the same message to him again.
                    if (!listener.equals(sender)) {
                        actorRuntime.send(ref, listener, messageDeliveryFailure);
                    }
                }
            }
        }
    }

    private static class Message {
        private final Object content;
        private final ActorRef sender;

        Message(Object content, ActorRef sender) {
            this.content = content;
            this.sender = sender;
        }
    }

    @Override
    public void readData(DataInput in) throws IOException {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void writeData(DataOutput out) throws IOException {
        //To change body of implemented methods use File | Settings | File Templates.
    }
}
