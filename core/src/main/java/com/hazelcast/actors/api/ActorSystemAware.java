package com.hazelcast.actors.api;

import com.hazelcast.actors.api.ActorRuntime;

public interface ActorSystemAware {
   void setActorRuntime(ActorRuntime actorRuntime);
}
