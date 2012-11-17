package com.hazelcast.actors.impl;

import com.hazelcast.actors.api.ActorFactory;
import com.hazelcast.config.CustomServiceConfig;

import static com.hazelcast.actors.utils.Util.notNull;

public class ActorServiceConfig extends CustomServiceConfig {

    private ActorFactory actorFactory = new BasicActorFactory();

    public ActorServiceConfig() {
        setName(ActorService.NAME);
        setClassName(ActorService.class.getName());
        setEnabled(true);
    }

    public ActorFactory getActorFactory() {
        return actorFactory;
    }

    public void setActorFactory(ActorFactory actorFactory) {
        this.actorFactory = notNull(actorFactory,"actorFactory");
    }
}
