package com.hazelcast.actors.impl.actorcontainers;

import com.hazelcast.actors.api.Actor;
import com.hazelcast.actors.api.ActorRecipe;
import com.hazelcast.actors.api.ActorRef;
import com.hazelcast.actors.utils.Util;
import com.hazelcast.core.IMap;

import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ForkJoinTask;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;

import static com.hazelcast.actors.utils.Util.notNull;


/**
 * The ActorContainer wraps the Actor and stores the mailbox and deals with storing and processing the messages.
 * <p/>
 * Every Actor will have 1 ActorContainer.
 */
public final class ForkJoinPoolActorContainer<A extends Actor> extends AbstractActorContainer<A> {
    //todo: can be replaced by a FIeldUpdates and making it volatile.
    private final AtomicBoolean lock = new AtomicBoolean();
    protected final BlockingQueue mailbox = new LinkedBlockingQueue(1000000);

    //todo: the fields below can all be passed as a single reference.
    private final ForkJoinPool executor;

    private ProcessingForkJoinTask processingForkJoinTask = new ProcessingForkJoinTask();

    public ForkJoinPoolActorContainer(ActorRecipe<A> recipe, ActorRef actorRef, ForkJoinPool executor, IMap<ActorRef, Set<ActorRef>> monitorMap) {
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

        executor.execute(new ProcessingForkJoinTask());
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
                executor.execute(new ProcessingForkJoinTask());
            }
            return true;
        }
    }

    public static class Factory<A extends Actor> implements ActorContainerFactory<A> {
        private final ForkJoinPool forkJoinPool;
        private IMap<ActorRef, Set<ActorRef>> monitorMap;


        public Factory(ForkJoinPool forkJoinPool) {
            this.forkJoinPool = notNull(forkJoinPool, "forkJoinPool");
        }

        @Override
        public void init(IMap<ActorRef, Set<ActorRef>> monitorMap) {
            this.monitorMap = monitorMap;
        }

        @Override
        public ForkJoinPoolActorContainer<A> newContainer(ActorRef actorRef, ActorRecipe<A> recipe) {
            return new ForkJoinPoolActorContainer<A>(recipe, actorRef, forkJoinPool, monitorMap);
        }
    }
}
