package io.brooklyn.attributes;

import io.brooklyn.entity.EntityConfig;
import io.brooklyn.entity.EntityReference;

public interface RelationAttribute extends ReferenceAttribute<EntityReference>{

    void start(EntityConfig recipe);

    void send(Object msg);
}
