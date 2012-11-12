package com.hazelcast.actors.api;

import com.hazelcast.actors.api.ActorRef;

public interface ActorRefAware {
    void setActorRef(ActorRef actorRef);
}
