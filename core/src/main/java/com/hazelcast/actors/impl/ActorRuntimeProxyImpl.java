package com.hazelcast.actors.impl;

import com.hazelcast.actors.api.Actor;
import com.hazelcast.actors.api.ActorRecipe;
import com.hazelcast.actors.api.ActorRef;
import com.hazelcast.actors.impl.actorcontainers.ActorContainer;
import com.hazelcast.actors.utils.Util;
import com.hazelcast.spi.Invocation;
import com.hazelcast.spi.NodeEngine;
import com.hazelcast.spi.Operation;
import com.hazelcast.spi.OperationService;

import java.util.Collection;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static com.hazelcast.actors.utils.Util.notNull;

public class ActorRuntimeProxyImpl implements ActorRuntimeProxy {
    private final Random random = new Random();
    private final String name;
    private final ActorService actorService;
    private final OperationService operationService;

    ActorRuntimeProxyImpl(ActorService actorService, String name) {
        this.actorService = actorService;
        this.name = name;
        this.operationService = actorService.nodeEngine.getOperationService();
    }

    public NodeEngine getNodeEngine() {
        return actorService.nodeEngine;
    }

    //method is only here for testing.
    public Actor getActor(ActorRef actorRef) {
        int partitionId = actorRef.getPartitionId();
        ActorPartitionContainer actorPartitionContainer = actorService.partitionContainers[partitionId];
        if (actorPartitionContainer == null) {
            throw new NullPointerException("No actor with actorRef:" + actorRef.getId() + " found");
        }

        ActorPartition actorPartition = actorPartitionContainer.getPartition(name);
        ActorContainer actorContainer = actorPartition.getActorContainer(actorRef.getId());
        return actorContainer.getActor();
    }

    @Override
    public void link(ActorRef ref1, ActorRef ref2) {
        notNull(ref1, "link");
        notNull(ref2, "subject");

        if (ref1.equals(ref2)) {
            throw new IllegalArgumentException("Can't create a self link");
        }

        if (!actorService.actorMap.containsKey(ref1)) {
            throw new IllegalArgumentException();
        }

        if (!actorService.actorMap.containsKey(ref2)) {
            throw new IllegalArgumentException();
        }

        linkTo(ref1, ref2);
        linkTo(ref2, ref1);

        //todo: it could be the link is created, but that the actor already is exited. So we need to do some rechecking.
    }

    private void linkTo(ActorRef monitor, ActorRef subject) {
        actorService.linksMap.lock(subject);
        try {
            Set<ActorRef> links = actorService.linksMap.get(subject);
            Set<ActorRef> newLinks = new HashSet<>();
            if (links != null) {
                newLinks.addAll(links);
            }
            newLinks.add(monitor);
            actorService.linksMap.put(subject, newLinks);
        } finally {
            actorService.linksMap.unlock(subject);
        }
    }

    @Override
    public void notify(final ActorRef destination, final Object notification, int delaysMs) {
        Runnable command = new Runnable() {
            @Override
            public void run() {
                send(destination, notification);
            }
        };
        actorService.scheduler.scheduleWithFixedDelay(command, 0, delaysMs, TimeUnit.MILLISECONDS);
    }

    @Override
    public ActorRef spawnAndLink(ActorRef listener, ActorRecipe recipe) {
        notNull(recipe, "recipe");

        Object partitionKey = getPartitionKey(recipe);

        Operation createOperation = new CreateOperation(name, recipe, partitionKey);
        createOperation.setValidateTarget(true);
        createOperation.setServiceName(ActorService.NAME);
        try {
            int partitionId = actorService.nodeEngine.getPartitionId(actorService.nodeEngine.toData(partitionKey));
            Invocation invocation = operationService.createInvocationBuilder(ActorService.NAME, createOperation, partitionId).build();
            Future future = invocation.invoke();
            ActorRef ref = (ActorRef) actorService.nodeEngine.toObject(future.get());
            Object result;
            //we need to apply some hacking; because we need to wait till the container completes activation.
            //Normally this should be done using a future, but Hazelcast is deadlocking on the container initialization.
            for (; ; ) {
                result = actorService.actorMap.get(ref);
                if (result != null) {
                    break;
                }
                Util.sleep(100);
            }

            if (result instanceof Throwable) {
                Throwable throwable = (Throwable) result;
                StackTraceElement[] clientSideStackTrace = Thread.currentThread().getStackTrace();
                Util.fixStackTrace(throwable, clientSideStackTrace);
                if (throwable instanceof RuntimeException) {
                    throw (RuntimeException) throwable;
                } else {
                    throw new RuntimeException(throwable);
                }
            } else {
                if (listener != null) {
                    link(ref, listener);
                }
                return ref;
            }
        } catch (RuntimeException e) {
            throw e;
        } catch (Throwable throwable) {
            throw new RuntimeException(throwable);
        }
    }

    private Object getPartitionKey(ActorRecipe recipe) {
        Object partitionKey = recipe.getPartitionKey();
        //if there is no PartitionKey assigned to the recipe, it means that the caller doesn't care
        //where the actor is going to run. So lets pick a partition randomly.
        if (partitionKey == null) {
            partitionKey = random.nextInt();
        }
        return partitionKey;
    }


    @Override
    public ActorRef spawn(ActorRecipe recipe) {
        return spawnAndLink(null, recipe);
    }

    @Override
    public void send(ActorRef sender, Collection<ActorRef> destinations, Object msg) {
        notNull(destinations, "destinations");
        notNull(msg, "msg");

        if (destinations.isEmpty()) {
            return;
        }

        for (ActorRef destination : destinations) {
            send(sender, destination, msg);
        }
    }

    @Override
    public Future ask(ActorRef sender, ActorRef destination, Object msg) {
        notNull(destination, "destination");
        notNull(msg, "msg");

        Operation sendOperation = new AskOperation(sender, name, destination.getId(), msg);
        sendOperation.setValidateTarget(true);
        sendOperation.setServiceName(ActorService.NAME);
        try {
            Invocation invocation = operationService.createInvocationBuilder(
                    ActorService.NAME, sendOperation, destination.getPartitionId()).build();
            Future future = invocation.invoke();
            Object result = future.get();
            if (result instanceof Throwable) {
                Throwable throwable = (Throwable) result;
                StackTraceElement[] clientSideStackTrace = Thread.currentThread().getStackTrace();
                Util.fixStackTrace(throwable, clientSideStackTrace);
                if (throwable instanceof RuntimeException) {
                    throw (RuntimeException) throwable;
                } else {
                    throw new RuntimeException(throwable);
                }
            } else {
                String responseId = (String) result;
                return new AskFuture(responseId);
            }
        } catch (RuntimeException e) {
            throw e;
        } catch (Throwable throwable) {
            throw new RuntimeException(throwable);
        }
    }

    private class AskFuture implements Future {
        private final String responseId;

        private AskFuture(String responseId) {
            this.responseId = responseId;
        }

        @Override
        public boolean cancel(boolean mayInterruptIfRunning) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean isCancelled() {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean isDone() {
            return actorService.responseMap.containsKey(responseId);
        }

        @Override
        public Object get() throws InterruptedException, ExecutionException {
            //todo: very nasty hack to wait till the message is processed.
            for (; ; ) {
                Object result = actorService.responseMap.get(responseId);
                if (result != null) {
                    if (result instanceof Throwable) {
                        Throwable cause = (Throwable)result;
                        Util.fixStackTrace(cause,Thread.currentThread().getStackTrace());
                        throw new ExecutionException(cause);
                    } else {
                        return result;
                    }
                }
                Util.sleep(100);
            }
        }

        @Override
        public Object get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
            throw new UnsupportedOperationException();
        }
    }

    @Override
    public void send(ActorRef sender, ActorRef destination, Object msg) {
        notNull(destination, "destination");
        notNull(msg, "msg");

        Operation sendOperation = new SendOperation(sender, name, destination.getId(), msg);
        sendOperation.setValidateTarget(true);
        sendOperation.setServiceName(ActorService.NAME);
        try {
            Invocation invocation = operationService.createInvocationBuilder(
                    ActorService.NAME, sendOperation, destination.getPartitionId()).build();
            Future future = invocation.invoke();
            Object result = future.get();
            if (result instanceof Throwable) {
                Throwable throwable = (Throwable) result;
                StackTraceElement[] clientSideStackTrace = Thread.currentThread().getStackTrace();
                Util.fixStackTrace(throwable, clientSideStackTrace);
                if (throwable instanceof RuntimeException) {
                    throw (RuntimeException) throwable;
                } else {
                    throw new RuntimeException(throwable);
                }
            }
        } catch (RuntimeException e) {
            throw e;
        } catch (Throwable throwable) {
            throw new RuntimeException(throwable);
        }
    }

    @Override
    public void send(ActorRef destination, Object msg) {
        send(null, destination, msg);
    }

    @Override
    public void exit(ActorRef target) {
        notNull(target, "target");

        Operation op = new ExitOperation(name, target);
        op.setValidateTarget(true);
        op.setServiceName(ActorService.NAME);
        try {
            Invocation invocation = operationService.createInvocationBuilder(
                    ActorService.NAME, op, target.getPartitionId()).build();
            Future f = invocation.invoke();
            f.get();
        } catch (RuntimeException e) {
            throw e;
        } catch (Throwable throwable) {
            throw new RuntimeException(throwable);
        }
    }

    @Override
    public InstanceType getInstanceType() {
        return null;
    }

    @Override
    public void destroy() {
        //todo:
    }

    @Override
    public Object getId() {
        return name;
    }
}
