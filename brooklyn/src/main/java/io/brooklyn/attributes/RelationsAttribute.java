package io.brooklyn.attributes;

import com.hazelcast.actors.api.ActorRef;

public interface RelationsAttribute extends ListAttribute<ActorRef> {

    void registerOnChildren(AttributeType attributeType, ActorRef subscriber);
}
