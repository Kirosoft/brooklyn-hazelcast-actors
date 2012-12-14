package io.brooklyn.attributes;

import com.hazelcast.actors.api.ActorRef;
import io.brooklyn.entity.EntityReference;

public interface RelationsAttribute extends ListAttribute<EntityReference> {

    void registerOnChildren(AttributeType attributeType, ActorRef subscriber);
}
