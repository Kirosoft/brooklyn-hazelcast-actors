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
        post(null, EXIT);
    }

    protected void handleExit() {
        //thinking about termination; what about orphan children.
        //at this point we are sure that no new actors can be created;

        if (actor instanceof ActorLifecycleAware) {
            try {
                ((ActorLifecycleAware) actor).onExit();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        //this is still mess
        Set<ActorRef> monitors = dependencies.monitorMap.get(ref);
        if (monitors != null) {
            try {
                Actors.Exit termination = new Actors.Exit(ref);
                dependencies.actorRuntime.send(ref, monitors, termination);
            } finally {
                dependencies.monitorMap.remove(ref);
            }
        }

        //exit all children.
        IMap<ActorRef, Set<ActorRef>> childrenMap = getHazelcastInstance().getMap("childrenMap");
        childrenMap.lock(ref);
        try {
            Set<ActorRef> children = childrenMap.remove(ref);
            if (children != null) {
                for(ActorRef child: children){
                    dependencies.actorRuntime.exit(child);
                }
            }
        } finally {
            childrenMap.unlock(ref);
        }
    }

    protected void handleProcessingException(ActorRef sender, Exception exception) {
        exception.printStackTrace();

        MessageDeliveryFailure messageDeliveryFailure = null;
        if (sender != null) {
            messageDeliveryFailure = new MessageDeliveryFailure(ref, sender, exception);
            dependencies.actorRuntime.send(sender, messageDeliveryFailure);
        }

        Set<ActorRef> monitorsForSubject = dependencies.monitorMap.get(ref);
        if (monitorsForSubject != null && !monitorsForSubject.isEmpty()) {
            if (messageDeliveryFailure == null)
                messageDeliveryFailure = new MessageDeliveryFailure(ref, sender, exception);

            for (ActorRef monitor : monitorsForSubject) {
                //if the sender also is a monitor, we don't want to send the same message to him again.
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
        public final IMap<ActorRef, Set<ActorRef>> monitorMap;
        public final NodeServiceImpl nodeService;
        public final ActorFactory actorFactory;
        public final HazelcastInstanceImpl hzInstance;

        public Dependencies(ActorFactory actorFactory, ActorRuntime actorRuntime, IMap<ActorRef, Set<ActorRef>> monitorMap,
                            NodeServiceImpl nodeService) {
            this.actorFactory = actorFactory;
            this.actorRuntime = actorRuntime;
            this.monitorMap = monitorMap;
            this.nodeService = nodeService;
            this.hzInstance = nodeService.getNode().hazelcastInstance;
        }
    }
}
