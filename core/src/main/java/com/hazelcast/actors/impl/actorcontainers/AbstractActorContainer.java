package com.hazelcast.actors.impl.actorcontainers;

import com.hazelcast.actors.api.Actor;
import com.hazelcast.actors.api.ActorContext;
import com.hazelcast.actors.api.ActorContextAware;
import com.hazelcast.actors.api.ActorFactory;
import com.hazelcast.actors.api.ActorLifecycleAware;
import com.hazelcast.actors.api.ActorRecipe;
import com.hazelcast.actors.api.ActorRef;
import com.hazelcast.actors.api.ActorRuntime;
import com.hazelcast.actors.api.Actors;
import com.hazelcast.actors.api.MessageDeliveryFailure;
import com.hazelcast.actors.api.exceptions.ActorInstantiationException;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.IMap;
import com.hazelcast.nio.DataSerializable;
import com.hazelcast.spi.impl.NodeServiceImpl;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.Set;

import static com.hazelcast.actors.utils.Util.notNull;
import static java.lang.String.format;

public abstract class AbstractActorContainer<A extends Actor> implements DataSerializable, ActorContainer<A>, ActorContext {
    protected final static TerminateMessage TERMINATION = new TerminateMessage();

    protected final ActorRef ref;
    protected final ActorRecipe<A> recipe;
    protected A actor;

    //todo: the fields below can all be passed as a single reference.
    protected final IMap<ActorRef, Set<ActorRef>> monitorMap;
    protected ActorRuntime actorRuntime;
    protected HazelcastInstance hzInstance;

    public AbstractActorContainer(ActorRecipe<A> recipe, ActorRef actorRef, IMap<ActorRef, Set<ActorRef>> monitorMap) {
        this.recipe = notNull(recipe, "recipe");
        this.ref = notNull(actorRef, "ref");
        this.monitorMap = notNull(monitorMap, "monitorMap");
    }

    @Override
    public ActorRef getActorRef() {
        return self();
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
    public A activate(ActorRuntime actorRuntime, NodeServiceImpl nodeService, ActorFactory actorFactory) {
        this.actorRuntime = actorRuntime;

        this.actor = actorFactory.newActor(recipe);
        this.hzInstance = nodeService.getNode().hazelcastInstance;

        if (actor instanceof ActorContextAware) {
            ((ActorContextAware) actor).setActorContext(this);
        }

        if (actor instanceof ActorLifecycleAware) {
            try {
                ((ActorLifecycleAware) actor).activate();
            } catch (Exception e) {
                throw new ActorInstantiationException(format("Failed called %s.activate()", actor.getClass().getName()), e);
            }
        }
        return actor;
    }

    @Override
    public void terminate() throws Exception {
        post(null, TERMINATION);
    }

    protected void handleTermination() {
        if (actor instanceof ActorLifecycleAware) {
            try {
                ((ActorLifecycleAware) actor).terminate();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        Set<ActorRef> monitors = monitorMap.get(ref);
        if (monitors != null) {
            Actors.ActorTermination termination = new Actors.ActorTermination(ref);
            actorRuntime.send(ref, monitors, termination);
            monitorMap.remove(ref);
        }
    }

    protected void handleProcessingException(ActorRef sender, Exception exception) {
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
}
