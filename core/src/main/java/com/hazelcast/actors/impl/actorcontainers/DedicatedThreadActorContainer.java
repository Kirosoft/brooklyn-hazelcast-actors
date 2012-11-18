package com.hazelcast.actors.impl.actorcontainers;

import com.hazelcast.actors.api.Actor;
import com.hazelcast.actors.api.ActorRecipe;
import com.hazelcast.actors.api.ActorRef;
import com.hazelcast.core.IMap;

import java.util.Set;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;

public class DedicatedThreadActorContainer<A extends Actor> extends AbstractActorContainer<A> {
    protected final BlockingQueue mailbox = new ArrayBlockingQueue(100000);

    private final Thread thread;

    public DedicatedThreadActorContainer(ActorRecipe<A> recipe, ActorRef actorRef,
                                         IMap<ActorRef, Set<ActorRef>> monitorMap) {
        super(recipe, actorRef, monitorMap);
        this.thread = new Thread("actor-thread-" + actorRef.getId()) {
            public void run() {
                while (true) {
                    Object m;
                    try {
                        m = mailbox.take();
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }

                    ActorRef sender;
                    Object message;
                    if (m instanceof MessageWrapper) {
                        message = ((MessageWrapper) m).content;
                        sender = ((MessageWrapper) m).sender;
                    } else {
                        message = m;
                        sender = null;
                    }

                    try {
                        actor.receive(message, sender);
                    } catch (Exception exception) {
                        handleProcessingException(sender, exception);
                    }
                }
            }
        };
        thread.start();
    }

    @Override
    public void post(ActorRef sender, Object message) throws InterruptedException {
        if (sender == null) {
            mailbox.put(message);
        } else {
            mailbox.put(new MessageWrapper(message, sender));
        }
    }

    public static class Factory<A extends Actor> implements ActorContainerFactory<A>{
        private IMap<ActorRef, Set<ActorRef>> monitorMap;

        @Override
        public void init(IMap<ActorRef, Set<ActorRef>> monitorMap) {
            this.monitorMap = monitorMap;
        }

        @Override
        public ActorContainer<A> newContainer(ActorRef actorRef, ActorRecipe<A> recipe) {
            return new DedicatedThreadActorContainer<>(recipe, actorRef, monitorMap);
        }
    }
}