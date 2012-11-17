package com.hazelcast.actors.impl.actorcontainers;

import com.hazelcast.actors.api.Actor;
import com.hazelcast.actors.api.ActorFactory;
import com.hazelcast.actors.api.ActorRecipe;
import com.hazelcast.actors.api.ActorRef;
import com.hazelcast.actors.api.ActorRuntime;
import com.hazelcast.core.IMap;
import com.hazelcast.spi.impl.NodeServiceImpl;

import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class ActorExecutorActorContainer<A extends Actor> extends AbstractActorContainer<A> {

    private ActorExecutor actorExecutor = new ActorExecutor();

    public BlockingQueue mailbox = new LinkedBlockingQueue();
    public ActorExecutor.WorkIndicator workIndicator;

    public ActorExecutorActorContainer(ActorRecipe<A> recipe, ActorRef actorRef,
                                       IMap<ActorRef, Set<ActorRef>> monitorMap) {
        super(recipe, actorRef, monitorMap);
    }

    @Override
    public A activate(ActorRuntime actorRuntime, NodeServiceImpl nodeService, ActorFactory actorFactory) {
        A actor = super.activate(actorRuntime, nodeService, actorFactory);
        workIndicator = actorExecutor.register(this);
        return actor;
    }

    @Override
    public void post(ActorRef sender, Object message) throws InterruptedException {
        if (sender == null) {
            mailbox.put(message);
        } else {
            mailbox.put(new MessageWrapper(message, sender));
        }
        workIndicator.signal();
    }

    public void process() {
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

        if (!mailbox.isEmpty()) {
            // executor.execute(new ProcessingForkJoinTask());
        }

        return;
    }
}

