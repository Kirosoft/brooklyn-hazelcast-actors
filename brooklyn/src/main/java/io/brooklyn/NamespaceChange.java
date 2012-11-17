package io.brooklyn;

import com.hazelcast.actors.api.ActorRef;

import java.io.Serializable;

import static com.hazelcast.actors.utils.Util.notNull;

public class NamespaceChange implements Serializable {

    private final ActorRef actorRef;
    private final boolean added;
    private final String namespace;

    public NamespaceChange(ActorRef actorRef, boolean added, String namespace) {
        this.actorRef = notNull(actorRef, "actorRef");
        this.added = added;
        this.namespace = notNull(namespace, "namespace");
    }

    public ActorRef getActorRef() {
        return actorRef;
    }

    public boolean isAdded() {
        return added;
    }

    public String getNamespace() {
        return namespace;
    }

    @Override
    public String toString() {
        return "NamespaceChange{" +
                "actorRef=" + actorRef +
                ", added=" + added +
                ", namespace='" + namespace + '\'' +
                '}';
    }
}
