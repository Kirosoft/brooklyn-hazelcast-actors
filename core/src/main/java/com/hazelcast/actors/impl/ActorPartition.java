package com.hazelcast.actors.impl;

import com.hazelcast.actors.api.ActorRecipe;
import com.hazelcast.actors.api.ActorRef;
import com.hazelcast.actors.api.ActorRuntime;
import com.hazelcast.actors.impl.actorcontainers.ActorContainer;
import com.hazelcast.actors.impl.actorcontainers.ActorContainerFactory;

import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Future;

import static com.hazelcast.actors.utils.Util.notNull;

class ActorPartition {
    private final ConcurrentMap<String, ActorContainer> actorContainerMap = new ConcurrentHashMap<>();
    private final ActorContainerFactory containerFactory;
    private final ActorService actorService;

    ActorPartition(ActorService actorService, String name) {
        this.actorService = notNull(actorService, "actorService");
        this.containerFactory = actorService.containerFactoryFactory.newFactory(
                actorService.actorFactory,
                (ActorRuntime) actorService.getProxy(name),
                actorService.linksMap,
                actorService.responseMap,
                actorService.nodeEngine);
    }

    public ActorContainer getActorContainer(String id) {
        return actorContainerMap.get(id);
    }

    public void send(ActorRef sender, String actorId, Object message) throws InterruptedException {
        notNull(actorId, "actorId");

        ActorContainer actorContainer = actorContainerMap.get(actorId);

        if (actorContainer == null) {
            throw new IllegalArgumentException("Can't send message, actor " + actorId + " is not found");
        }

        actorContainer.send(sender, message);
    }

    public void ask(ActorRef sender, String actorId, Object message, String responseId) throws InterruptedException {
        notNull(actorId, "actorId");

        ActorContainer actorContainer = actorContainerMap.get(actorId);

        if (actorContainer == null) {
            throw new IllegalArgumentException("Can't send message, actor " + actorId + " is not found");
        }

        actorContainer.ask(sender, message,responseId);
    }

    public ActorRef createActor(final ActorRecipe recipe, final Object partitionKey, final int partitionId) throws Exception {
        notNull(recipe, "recipe");
        notNull(partitionKey, "partitionKey");

        //todo: this id function needs to be improved, but for the time being is good enough
        String id = recipe.getActorClass().getSimpleName() + UUID.randomUUID().toString().replaceAll("-", "").substring(0, 10);
        final ActorRef ref = new ActorRef(id, partitionKey, partitionId);

        //we need to offload the actual creation/activation of the actor, since in Hazelcast it isn't allowed to call
        //a Hazelcast structure from the spi thread.
        Future<ActorContainer> future = actorService.offloadExecutor.submit(
                new Callable<ActorContainer>() {
                    @Override
                    public ActorContainer call() throws Exception {
                        try {
                            ActorContainer container = containerFactory.newContainer(ref, recipe);
                            actorService.actorMap.put(ref, recipe);
                            actorContainerMap.put(ref.getId(), container);
                            container.activate();
                            return container;
                        } catch (Exception e) {
                            actorService.actorMap.put(ref, e);
                            throw e;
                        }
                    }
                }
        );

        //try {
        //future.get();

        return ref;
        //} catch (InterruptedException e) {
        //    throw new RuntimeException(e);
        //} catch (ExecutionException e) {
        //    Throwable cause = e.getCause();
        //    if (cause instanceof Exception) {
        //        throw (Exception) cause;
        //    } else {
        //        throw new RuntimeException(e);
        //    }
        //}
    }

    public void exit(final ActorRef target) throws InterruptedException {
        notNull(target, "target");

        final ActorContainer actorContainer = actorContainerMap.remove(target.getId());
        if (actorContainer == null) {
            return;
        }

        try {
            actorContainer.exit();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public ActorPartitionContent content() {
        return null;
    }
}
