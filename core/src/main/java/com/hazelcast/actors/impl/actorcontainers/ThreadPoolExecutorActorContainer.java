package com.hazelcast.actors.impl.actorcontainers;

import com.hazelcast.actors.api.Actor;
import com.hazelcast.actors.api.ActorRecipe;
import com.hazelcast.actors.api.ActorRef;
import com.hazelcast.actors.utils.Util;
import com.hazelcast.core.IMap;

import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;

import static com.hazelcast.actors.utils.Util.notNull;

public class ThreadPoolExecutorActorContainer<A extends Actor> extends AbstractActorContainer<A> {
    //todo: can be replaced by a FIeldUpdates and making it volatile.
    private final AtomicBoolean lock = new AtomicBoolean();

    //todo: the fields below can all be passed as a single reference.
    private final Executor executor;
    protected final BlockingQueue mailbox = new LinkedBlockingQueue();

    private ProcessingForkJoinTask processingForkJoinTask = new ProcessingForkJoinTask();

    public ThreadPoolExecutorActorContainer(ActorRecipe<A> recipe, ActorRef actorRef, Executor executor, IMap<ActorRef, Set<ActorRef>> monitorMap) {
        super(recipe, actorRef, monitorMap);
        this.executor = Util.notNull(executor, "executor");
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

        executor.execute(processingForkJoinTask);
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
                Object m = null;
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
                executor.execute(this);
            }
        }
    }

    public static class Factory<A extends Actor> implements ActorContainerFactory<A> {
        private final ExecutorService executor;
        private IMap<ActorRef, Set<ActorRef>> monitorMap;


        public Factory() {
            this(Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors() * 4));
        }

        public Factory(ExecutorService executor) {
            this.executor = notNull(executor, "executor");
        }

        @Override
        public void init(IMap<ActorRef, Set<ActorRef>> monitorMap) {
            this.monitorMap = monitorMap;
        }

        @Override
        public ThreadPoolExecutorActorContainer<A> newContainer(ActorRef actorRef, ActorRecipe<A> recipe) {
            return new ThreadPoolExecutorActorContainer<>(recipe, actorRef, executor, monitorMap);
        }
    }
}

