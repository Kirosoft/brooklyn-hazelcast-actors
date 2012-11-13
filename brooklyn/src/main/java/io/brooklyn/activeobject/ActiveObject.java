package io.brooklyn.activeobject;

import com.hazelcast.actors.api.ActorRef;

public interface ActiveObject {
    ActorRef getActorRef();
}
