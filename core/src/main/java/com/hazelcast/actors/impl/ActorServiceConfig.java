package com.hazelcast.actors.impl;

import com.hazelcast.actors.api.ActorFactory;
import com.hazelcast.actors.impl.actorcontainers.ActorContainer;
import com.hazelcast.actors.impl.actorcontainers.ActorContainerFactoryFactory;
import com.hazelcast.actors.impl.actorcontainers.ThreadPoolExecutorActorContainer;
import com.hazelcast.config.CustomServiceConfig;

import static com.hazelcast.actors.utils.Util.notNull;

public class ActorServiceConfig extends CustomServiceConfig {

    private ActorFactory actorFactory = new BasicActorFactory();
    private ActorContainerFactoryFactory actorContainerFactoryFactory = new ThreadPoolExecutorActorContainer.FactoryFactory();

    public ActorServiceConfig() {
        setName(ActorService.NAME);
        setClassName(ActorService.class.getName());
        setEnabled(true);
    }

    public ActorContainerFactoryFactory getActorContainerFactoryFactory() {
        return actorContainerFactoryFactory;
    }

    public void setActorContainerFactory(ActorContainerFactoryFactory actorContainerFactory) {
        this.actorContainerFactoryFactory = notNull(actorContainerFactory, "actorContainerFactory");
    }

    public ActorFactory getActorFactory() {
        return actorFactory;
    }

    public void setActorFactory(ActorFactory actorFactory) {
        this.actorFactory = notNull(actorFactory, "actorFactory");
    }
}
