package com.hazelcast.actors.impl;

import com.hazelcast.actors.api.ActorFactory;
import com.hazelcast.actors.impl.actorcontainers.ActorContainerFactory;
import com.hazelcast.actors.impl.actorcontainers.ForkJoinPoolActorContainer;
import com.hazelcast.actors.impl.actorcontainers.ThreadPoolExecutorActorContainer;
import com.hazelcast.config.CustomServiceConfig;

import static com.hazelcast.actors.utils.Util.notNull;

public class ActorServiceConfig extends CustomServiceConfig {

    private ActorFactory actorFactory = new BasicActorFactory();
    private ActorContainerFactory actorContainerFactory = new ThreadPoolExecutorActorContainer.Factory();

    public ActorServiceConfig() {
        setName(ActorService.NAME);
        setClassName(ActorService.class.getName());
        setEnabled(true);
    }

    public ActorContainerFactory getActorContainerFactory() {
        return actorContainerFactory;
    }

    public void setActorContainerFactory(ActorContainerFactory actorContainerFactory) {
        this.actorContainerFactory = notNull(actorContainerFactory,"actorContainerFactory");
    }

    public ActorFactory getActorFactory() {
        return actorFactory;
    }

    public void setActorFactory(ActorFactory actorFactory) {
        this.actorFactory = notNull(actorFactory,"actorFactory");
    }
}
