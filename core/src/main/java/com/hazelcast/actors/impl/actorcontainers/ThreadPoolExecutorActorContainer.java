package com.hazelcast.actors.impl.actorcontainers;

import com.hazelcast.actors.api.*;
import com.hazelcast.core.IMap;
import com.hazelcast.spi.impl.NodeEngineImpl;

import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;

import static com.hazelcast.actors.utils.Util.notNull;

public class ThreadPoolExecutorActorContainer<A extends Actor>
        extends AbstractActorContainer<A, ThreadPoolExecutorActorContainer.Dependencies> {
    //todo: can be replaced by a FIeldUpdates and making it volatile.
    private final AtomicBoolean lock = new AtomicBoolean();

    protected final BlockingQueue mailbox = new LinkedBlockingQueue();

    private ProcessingForkJoinTask processingForkJoinTask = new ProcessingForkJoinTask();

    public ThreadPoolExecutorActorContainer(ActorRecipe<A> recipe, ActorRef actorRef, Dependencies dependencies) {
        super(recipe, actorRef, dependencies);
    }

    @Override
    public void ask(ActorRef sender, Object message, String responseId) throws InterruptedException {
        mailbox.put(new MessageWrapper(message, sender, responseId));

        if (lock.get()) {
            //if another thread is processing the actor, we don't need to schedule for execution. It will be the other
            //thread's responsibility
            return;
        }

        dependencies.executor.execute(processingForkJoinTask);
    }

    @Override
    public void send(ActorRef sender, Object message) throws InterruptedException {
        if (sender == null) {
            mailbox.put(message);
        } else {
            mailbox.put(new MessageWrapper(message, sender, null));
        }

        if (lock.get()) {
            //if another thread is processing the actor, we don't need to schedule for execution. It will be the other
            //thread's responsibility
            return;
        }

        dependencies.executor.execute(processingForkJoinTask);
    }

    private class ProcessingForkJoinTask implements Runnable {

        public void run() {
            boolean lockAcquired = lock.compareAndSet(false, true);

            if (!lockAcquired) {
                //someone else is currently processing a message for the actor, so it will be his responsibility
                //to keep processing the mailbox.
                return;
            }

            try {
                Object m;
                try {
                    m = mailbox.take();
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }

                ActorRef sender;
                Object message;
                String askId;
                if (m instanceof MessageWrapper) {
                    MessageWrapper messageWrapper = (MessageWrapper) m;
                    message = messageWrapper.content;
                    sender = messageWrapper.sender;
                    askId = messageWrapper.askId;
                } else {
                    message = m;
                    sender = null;
                    askId = null;
                }

                if (message == EXIT) {
                    handleExit();
                } else {
                    try {
                        //todo: we need let the receive method return an object.
                        actor.receive(message, sender);
                        if (askId != null) {
                            Object response = true;
                            dependencies.responseMap.put(askId, response);
                        }
                    } catch (Exception exception) {
                        handleProcessingException(sender, exception);

                        if (askId != null) {
                            dependencies.responseMap.put(askId, exception);
                        }
                    }

                }
            } finally {
                lock.set(false);
            }

            if (!mailbox.isEmpty()) {
                dependencies.executor.execute(this);
            }
        }
    }

    public static class Dependencies extends AbstractActorContainer.Dependencies {

        public final ExecutorService executor;

        public Dependencies(ActorFactory actorFactory, ActorRuntime actorRuntime, IMap<ActorRef,
                Set<ActorRef>> monitorMap,
                            IMap<String, Object> responseMap,
                            NodeEngineImpl nodeEngine, ExecutorService executor) {
            super(actorFactory, actorRuntime, monitorMap, responseMap, nodeEngine);
            this.executor = notNull(executor, "executor");
        }
    }

    public static class FactoryFactory implements ActorContainerFactoryFactory {

        @Override
        public ActorContainerFactory newFactory(ActorFactory actorFactory, ActorRuntime actorRuntime, IMap monitorMap, IMap responseMap, NodeEngineImpl nodeService) {
            ExecutorService nodeEngine = Executors.newFixedThreadPool(16);
            Dependencies dependencies = new Dependencies(actorFactory, actorRuntime, monitorMap, responseMap, nodeService, nodeEngine);
            return new Factory(dependencies);
        }
    }

    public static class Factory<A extends Actor> implements ActorContainerFactory<A> {
        private final Dependencies dependencies;

        public Factory(Dependencies dependencies) {
            this.dependencies = dependencies;
        }

        @Override
        public ThreadPoolExecutorActorContainer<A> newContainer(ActorRef actorRef, ActorRecipe<A> recipe) {
            return new ThreadPoolExecutorActorContainer<>(recipe, actorRef, dependencies);
        }
    }
}

