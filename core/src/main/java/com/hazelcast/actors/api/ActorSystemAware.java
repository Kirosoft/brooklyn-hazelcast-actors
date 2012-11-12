package com.hazelcast.actors.api;

public interface ActorSystemAware {
    void setActorRuntime(ActorRuntime actorRuntime);
}
