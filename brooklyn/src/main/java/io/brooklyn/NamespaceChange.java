package io.brooklyn;

import io.brooklyn.entity.EntityReference;

import java.io.Serializable;

import static com.hazelcast.actors.utils.Util.notNull;

public class NamespaceChange implements Serializable {

    private final EntityReference entityReference;
    private final boolean added;
    private final String namespace;

    public NamespaceChange(EntityReference entityReference, boolean added, String namespace) {
        this.entityReference = notNull(entityReference, "entityReference");
        this.added = added;
        this.namespace = notNull(namespace, "namespace");
    }

    public EntityReference getEntityReference() {
        return entityReference;
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
                "entityReference=" + entityReference +
                ", added=" + added +
                ", namespace='" + namespace + '\'' +
                '}';
    }
}
