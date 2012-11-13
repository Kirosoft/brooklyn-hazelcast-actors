package io.brooklyn.activeobject;

import com.hazelcast.actors.api.ActorRef;

public class AbstractActiveObject implements ActiveObject {

    private ActorRef actorRef;

    public ActorRef getActorRef() {
        return actorRef;
    }

    public void setActorRef(ActorRef actorRef) {
        this.actorRef = actorRef;
    }
}
