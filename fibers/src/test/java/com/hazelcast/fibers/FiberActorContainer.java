package com.hazelcast.actors.impl.actorcontainers;

import com.hazelcast.actors.api.Actor;
import com.hazelcast.actors.api.ActorRecipe;
import com.hazelcast.actors.api.ActorRef;
import com.hazelcast.actors.fibers.Fiber;
import com.hazelcast.actors.fibers.FiberContext;
import com.hazelcast.actors.fibers.FiberScheduler;
import com.hazelcast.core.IMap;

import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class FiberActorContainer<A extends Actor> extends AbstractActorContainer<A> implements FiberContext {

    public BlockingQueue mailbox = new LinkedBlockingQueue();
    private final Fiber fiber;

    public FiberActorContainer(ActorRecipe<A> recipe, ActorRef actorRef,
                               IMap<ActorRef, Set<ActorRef>> monitorMap, FiberScheduler fiberScheduler) {
        super(recipe, actorRef, monitorMap);
        fiber = fiberScheduler.start(this);
    }

    @Override
    public String getFiberName() {
        return "Fiber-" + getActorRef().getId();
    }

    @Override
    public void executeTask(Object task) {
        ActorRef sender;
        Object message;
        if (task instanceof MessageWrapper) {
            message = ((MessageWrapper) task).content;
            sender = ((MessageWrapper) task).sender;
        } else {
            message = task;
            sender = null;
        }

        try {
            actor.receive(message, sender);
        } catch (Exception exception) {
            handleProcessingException(sender, exception);
        }
    }

    @Override
    public void storeTask(Object task) {
        try {
            mailbox.put(task);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public Object takeTask() {
        return mailbox.poll();
    }

    @Override
    public boolean workAvailable() {
        return !mailbox.isEmpty();
    }

    @Override
    public void post(ActorRef sender, Object message) throws InterruptedException {
        if (sender == null) {
            fiber.scheduleTask(message);
        } else {
            fiber.scheduleTask(new MessageWrapper(message, sender));
        }
    }
}

