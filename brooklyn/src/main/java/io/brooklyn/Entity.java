package io.brooklyn;

import com.hazelcast.actors.actors.AbstractActor;
import com.hazelcast.actors.actors.ReflectiveActor;
import com.hazelcast.actors.api.ActorRecipe;
import com.hazelcast.actors.api.ActorRef;
import com.hazelcast.actors.api.Autowired;
import io.brooklyn.attributes.Attribute;
import io.brooklyn.attributes.AttributeMap;
import io.brooklyn.attributes.BasicAttributeRef;
import io.brooklyn.attributes.ListAttributeRef;

import java.io.Serializable;

public abstract class Entity extends ReflectiveActor {

    @Autowired
    private ManagementContext managementContext;

    private AttributeMap attributeMap = new AttributeMap(this);

    @Override
    public void init(ActorRecipe actorRecipe) throws Exception{
        super.init(actorRecipe);
        attributeMap.init(getHzInstance(), actorRecipe);
    }

    public final ManagementContext getManagementContext() {
        return managementContext;
    }

    public final <E> ListAttributeRef<E> newListAttribute(final Attribute<E> attribute) {
        return attributeMap.newListAttribute(attribute);
    }

    public final <E> BasicAttributeRef<E> newBasicAttributeRef(final Attribute<E> attribute) {
        return attributeMap.newBasicAttributeRef(attribute);
    }

    public void receive(SubscribeMessage subscribeMessage, ActorRef sender) {
        attributeMap.subscribe(subscribeMessage.attributeName, subscribeMessage.subscriber);
    }

    public static class SubscribeMessage implements Serializable {
        private final String attributeName;
        private final ActorRef subscriber;

        public SubscribeMessage(ActorRef subscriber, String attributeName) {
            this.attributeName = attributeName;
            this.subscriber = subscriber;
        }

        public SubscribeMessage(ActorRef subscriber, Attribute attribute) {
            this(subscriber, attribute.getName());
        }
    }
}
