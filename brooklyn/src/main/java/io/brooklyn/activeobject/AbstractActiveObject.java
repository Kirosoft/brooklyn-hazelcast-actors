package io.brooklyn.activeobject;

import com.hazelcast.actors.api.ActorRef;

import java.util.Map;

public class AbstractActiveObject implements ActiveObject {

    private ActorRef actorRef;
    private Map config;

    public ActorRef getActorRef() {
        return actorRef;
    }

    protected void setActorRef(ActorRef actorRef) {
        this.actorRef = actorRef;
    }

    public Map getConfig() {
        return config;
    }

    protected void init(Map config) {
        this.config = config;
    }
}
