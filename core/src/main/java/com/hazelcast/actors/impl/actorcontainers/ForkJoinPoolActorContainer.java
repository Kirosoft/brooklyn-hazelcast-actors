package com.hazelcast.actors.impl.actorcontainers;

import com.hazelcast.actors.api.*;
import com.hazelcast.core.IMap;
import com.hazelcast.spi.impl.NodeServiceImpl;

import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ForkJoinTask;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;


/**
 * The ActorContainer wraps the Actor and stores the mailbox and deals with storing and processing the messages.
 * <p/>
 * Every Actor will have 1 ActorContainer.
 */
public final class ForkJoinPoolActorContainer<A extends Actor> extends AbstractActorContainer<A, ForkJoinPoolActorContainer.Dependencies> {
    //todo: can be replaced by a FIeldUpdates and making it volatile.
    private final AtomicBoolean lock = new AtomicBoolean();
    protected final BlockingQueue mailbox = new LinkedBlockingQueue(1000000);

    public ForkJoinPoolActorContainer(ActorRecipe<A> recipe, ActorRef actorRef, Dependencies dependencies) {
        super(recipe, actorRef, dependencies);
    }

    @Override
    public void post(ActorRef sender, Object message) throws InterruptedException {
        if (sender == null) {
            mailbox.put(message);
        } else {
            mailbox.put(new MessageWrapper(message, sender));
        }

        if (lock.get()) {
            //if another thread is processing the actor, we don't need to schedule for execution. It will be the other
            //thread's responsibility
            return;
        }

        //we need to create a new ProcessingForkJoinTask since they are not te be reused.
        dependencies.forkJoinPool.execute(new ProcessingForkJoinTask());
    }

    private class ProcessingForkJoinTask extends ForkJoinTask {

        @Override
        public Object getRawResult() {
            return null;
        }

        @Override
        protected void setRawResult(Object value) {
            //no-op
        }

        @Override
        public boolean exec() {
            boolean lockAcquired = lock.compareAndSet(false, true);

            if (!lockAcquired) {
                //someone else is currently processing a message for the actor, so it will be his responsibility
                //to keep processing the mailbox.
                return true;
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
            } finally {
                lock.set(false);
            }

            if (!mailbox.isEmpty()) {
                dependencies.forkJoinPool.execute(new ProcessingForkJoinTask());
            }
            return true;
        }
    }

    public static class Dependencies extends AbstractActorContainer.Dependencies{
        private final ForkJoinPool forkJoinPool;

        public Dependencies(ActorFactory actorFactory, ActorRuntime actorRuntime, IMap<ActorRef, Set<ActorRef>> monitorMap,
                            NodeServiceImpl nodeService, ForkJoinPool forkJoinPool) {
            super(actorFactory, actorRuntime, monitorMap, nodeService);
            this.forkJoinPool = forkJoinPool;
        }
    }

    public static class ForkJoinContainerFactoryFactory implements ActorContainerFactoryFactory{

        @Override
        public ActorContainerFactory newFactory(ActorFactory actorFactory, ActorRuntime actorRuntime,
                                                IMap monitorMap,
                                                NodeServiceImpl nodeService) {
            ForkJoinPool forkJoinPool = new ForkJoinPool();

            ForkJoinPoolActorContainer.Dependencies dependencies = new ForkJoinPoolActorContainer.Dependencies(
                    actorFactory,actorRuntime,monitorMap,nodeService,forkJoinPool);
            return new Factory(dependencies);
        }
    }

    public static class Factory<A extends Actor> implements ActorContainerFactory<A> {

        private final Dependencies dependencies;

        public Factory(Dependencies dependencies) {
            this.dependencies = dependencies;
        }

        @Override
        public ForkJoinPoolActorContainer<A> newContainer(ActorRef actorRef, ActorRecipe<A> recipe) {
            return new ForkJoinPoolActorContainer<>(recipe, actorRef, dependencies);
        }
    }
}
