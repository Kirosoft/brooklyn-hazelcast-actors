package com.hazelcast.actors.impl.actorcontainers;

import com.hazelcast.actors.api.Actor;
import com.hazelcast.actors.api.ActorRecipe;
import com.hazelcast.actors.api.ActorRef;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

public class DedicatedThreadActorContainer<A extends Actor> extends AbstractActorContainer<A, AbstractActorContainer.Dependencies> {
    protected final BlockingQueue mailbox = new ArrayBlockingQueue(100000);

    private final Thread thread;

    public DedicatedThreadActorContainer(ActorRecipe<A> recipe, ActorRef actorRef, Dependencies dependencies) {
        super(recipe, actorRef, dependencies);
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
    public void send(ActorRef sender, Object message) throws InterruptedException {
        if (sender == null) {
            mailbox.put(message);
        } else {
            mailbox.put(new MessageWrapper(message, sender,null));
        }
    }

    @Override
    public void ask(ActorRef sender, Object message,String responseId) throws InterruptedException {
        throw new RuntimeException();
    }

    /*
    public static class Factory<A extends Actor> implements ActorContainer.Factory<A> {
        private Dependencies actorContainerDependencies;

        @Override
        public void init(Dependencies actorContainerDependencies) {
           this.actorContainerDependencies = actorContainerDependencies;
        }

        @Override
        public ActorContainer<A> newContainer(ActorRef actorRef, ActorRecipe<A> recipe) {
            return new DedicatedThreadActorContainer<>(recipe, actorRef, actorContainerDependencies);
        }
    }   */
}
