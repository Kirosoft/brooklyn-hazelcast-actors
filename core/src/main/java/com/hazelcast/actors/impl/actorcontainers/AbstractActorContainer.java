package com.hazelcast.actors.impl.actorcontainers;

import com.hazelcast.actors.api.*;
import com.hazelcast.actors.api.exceptions.ActorInstantiationException;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.IMap;
import com.hazelcast.instance.HazelcastInstanceImpl;
import com.hazelcast.nio.DataSerializable;
import com.hazelcast.spi.impl.NodeServiceImpl;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.Set;

import static com.hazelcast.actors.utils.Util.notNull;
import static java.lang.String.format;

public abstract class AbstractActorContainer<A extends Actor, D extends AbstractActorContainer.Dependencies>
        implements DataSerializable, ActorContainer<A>, ActorContext {

    protected final static TerminateMessage EXIT = new TerminateMessage();

    protected final ActorRef ref;
    protected final ActorRecipe<A> recipe;
    protected A actor;
    protected D dependencies;
    protected volatile boolean trapExit = false;
    protected volatile boolean exitPending = false;

    public AbstractActorContainer(ActorRecipe<A> recipe, ActorRef actorRef, D dependencies) {
        this.recipe = notNull(recipe, "recipe");
        this.ref = notNull(actorRef, "ref");
        this.dependencies = notNull(dependencies, "dependencies");
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
        return dependencies.hzInstance;
    }

    @Override
    public ActorRuntime getActorRuntime() {
        return dependencies.actorRuntime;
    }

    @Override
    public ActorRecipe getRecipe() {
        return recipe;
    }

    @Override
    public void trapExit() {
        trapExit = true;
    }

    @Override
    public void activate() {
        this.actor = dependencies.actorFactory.newActor(recipe);

        if (actor instanceof ActorContextAware) {
            ((ActorContextAware) actor).setActorContext(this);
        }

        if (actor instanceof ActorLifecycleAware) {
            try {
                ((ActorLifecycleAware) actor).onActivation();
            } catch (Exception e) {
                throw new ActorInstantiationException(format("Failed to call %s.activate()", actor.getClass().getName()), e);
            }
        }
    }

    @Override
    public void exit() throws Exception {
        if (trapExit) {
            post(null, EXIT);
        } else {
            exitPending = true;
            //todo: following is  hack to get the actor running
            post(null, EXIT);
            //todo: the problem here is that we don't want to wait till the next message is send to
        }
    }

    protected void handleExit() {
        if (actor instanceof ActorLifecycleAware) {
            try {
                ((ActorLifecycleAware) actor).onExit();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        Set<ActorRef> linkedActors = dependencies.linkedMap.remove(ref);
        if (linkedActors != null) {
            try {
                for(ActorRef linkedActor: linkedActors){
                   getActorRuntime().exit(linkedActor);
                }
            } finally {
                 dependencies.linkedMap.remove(ref);
             }
        }
    }

    protected void handleProcessingException(ActorRef sender, Exception exception) {
        exception.printStackTrace();

        MessageDeliveryFailure messageDeliveryFailure = null;
        if (sender != null) {
            messageDeliveryFailure = new MessageDeliveryFailure(ref, sender, exception);
            dependencies.actorRuntime.send(sender, messageDeliveryFailure);
        }

        Set<ActorRef> monitorsForSubject = dependencies.linkedMap.get(ref);
        if (monitorsForSubject != null && !monitorsForSubject.isEmpty()) {
            if (messageDeliveryFailure == null)
                messageDeliveryFailure = new MessageDeliveryFailure(ref, sender, exception);

            for (ActorRef monitor : monitorsForSubject) {
                //if the sender also is a link, we don't want to send the same message to him again.
                if (!monitor.equals(sender)) {
                    dependencies.actorRuntime.send(ref, monitor, messageDeliveryFailure);
                }
            }
        }
    }

    protected static class MessageWrapper {
        protected final Object content;
        protected final ActorRef sender;

        MessageWrapper(Object content, ActorRef sender) {
            this.content = content;
            this.sender = sender;
        }
    }

    private static class TerminateMessage {
    }

    @Override
    public void readData(DataInput in) throws IOException {
        //To change body map implemented methods use File | Settings | File Templates.
    }

    @Override
    public void writeData(DataOutput out) throws IOException {
        //To change body map implemented methods use File | Settings | File Templates.
    }

    public static class Dependencies {
        public final ActorRuntime actorRuntime;
        public final IMap<ActorRef, Set<ActorRef>> linkedMap;
        public final NodeServiceImpl nodeService;
        public final ActorFactory actorFactory;
        public final HazelcastInstanceImpl hzInstance;

        public Dependencies(ActorFactory actorFactory, ActorRuntime actorRuntime, IMap<ActorRef, Set<ActorRef>> linkedMap,
                            NodeServiceImpl nodeService) {
            this.actorFactory = actorFactory;
            this.actorRuntime = actorRuntime;
            this.linkedMap = linkedMap;
            this.nodeService = nodeService;
            this.hzInstance = nodeService.getNode().hazelcastInstance;
        }
    }
}
