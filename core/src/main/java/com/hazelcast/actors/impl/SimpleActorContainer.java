package com.hazelcast.actors.impl;

import com.hazelcast.actors.api.Actor;
import com.hazelcast.actors.api.ActorContext;
import com.hazelcast.actors.api.ActorContextAware;
import com.hazelcast.actors.api.ActorFactory;
import com.hazelcast.actors.api.ActorLifecycleAware;
import com.hazelcast.actors.api.ActorRecipe;
import com.hazelcast.actors.api.ActorRef;
import com.hazelcast.actors.api.ActorRuntime;
import com.hazelcast.actors.api.MessageDeliveryFailure;
import com.hazelcast.actors.utils.Util;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.IMap;
import com.hazelcast.nio.DataSerializable;
import com.hazelcast.spi.impl.NodeServiceImpl;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;


/**
 * The ActorContainer wraps the Actor and stores the mailbox and deals with storing and processing the messages.
 * <p/>
 * Every Actor will have 1 ActorContainer.
 */
public final class SimpleActorContainer<A extends Actor> implements DataSerializable, ActorContainer, ActorContext {
    private final ActorRef ref;
    private final ActorRecipe<A> recipe;

    private final ForkJoinPool executor;
    private final BlockingQueue mailbox = new LinkedBlockingQueue();
    private final Runnable processingRunnable = new ProcessingRunnable();

    //todo: can be replaced by a FIeldUpdates and making it volatile.
    private final AtomicBoolean lock = new AtomicBoolean();

    private A actor;
    private final IMap<ActorRef, Set<ActorRef>> monitorMap;
    private ActorRuntime actorRuntime;
    private HazelcastInstance hzInstance;

    public SimpleActorContainer(ActorRecipe<A> recipe, ActorRef actorRef, ForkJoinPool executor, IMap<ActorRef, Set<ActorRef>> monitorMap) {
        this.recipe = Util.notNull(recipe, "recipe");
        this.ref = Util.notNull(actorRef, "ref");
        this.executor = Util.notNull(executor, "executor");
        this.monitorMap = monitorMap;
    }

    @Override
    public A getActor() {
        return actor;
    }

    @Override
    public ActorRef self() {
        return ref;
    }

    @Override
    public HazelcastInstance getHazelcastInstance() {
        return hzInstance;
    }

    @Override
    public ActorRuntime getActorRuntime() {
        return actorRuntime;
    }

    @Override
    public ActorRecipe getRecipe() {
        return recipe;
    }

    @Override
    public void init(ActorRuntime actorRuntime, NodeServiceImpl nodeService, ActorFactory actorFactory) {
        this.actorRuntime = actorRuntime;

        this.actor = actorFactory.newActor(recipe);
        this.hzInstance = nodeService.getNode().hazelcastInstance;

        if (actor instanceof ActorContextAware) {
            ((ActorContextAware) actor).setActorContext(this);
        }

        if (actor instanceof ActorLifecycleAware) {
            try {
                ((ActorLifecycleAware) actor).init();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

    @Override
    public void terminate() throws Exception {
        if (actor instanceof ActorLifecycleAware) {
            ((ActorLifecycleAware) actor).terminate();
        }
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

        executor.execute(processingRunnable);
    }

    private class ProcessingRunnable implements Runnable {
        @Override
        public void run() {
            try {
                boolean lockAcquired = lock.compareAndSet(false, true);

                if (!lockAcquired) {
                    //someone else is currently processing a message for the actor, so it will be his responsibility
                    //to keep processing the mailbox.
                    return;
                }

                try {
                    Object m = mailbox.take();
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
                    executor.execute(processingRunnable);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private void handleProcessingException(ActorRef sender, Exception exception) {
        exception.printStackTrace();

        MessageDeliveryFailure messageDeliveryFailure = null;
        if (sender != null) {
            messageDeliveryFailure = new MessageDeliveryFailure(ref, sender, exception);
            actorRuntime.send(sender, messageDeliveryFailure);
        }

        Set<ActorRef> monitorsForSubject = monitorMap.get(ref);
        if (monitorsForSubject != null && !monitorsForSubject.isEmpty()) {
            if (messageDeliveryFailure == null)
                messageDeliveryFailure = new MessageDeliveryFailure(ref, sender, exception);

            for (ActorRef monitor : monitorsForSubject) {
                //if the sender also is a monitor, we don't want to send the same message to him again.
                if (!monitor.equals(sender)) {
                    actorRuntime.send(ref, monitor, messageDeliveryFailure);
                }
            }
        }
    }

    private static class MessageWrapper {
        private final Object content;
        private final ActorRef sender;

        MessageWrapper(Object content, ActorRef sender) {
            this.content = content;
            this.sender = sender;
        }
    }

    @Override
    public void readData(DataInput in) throws IOException {
        //To change body map implemented methods use File | Settings | File Templates.
    }

    @Override
    public void writeData(DataOutput out) throws IOException {
        //To change body map implemented methods use File | Settings | File Templates.
    }
}
