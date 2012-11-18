package io.brooklyn;

import com.hazelcast.actors.actors.DispatchingActor;
import com.hazelcast.actors.api.ActorRef;
import com.hazelcast.actors.api.Injected;
import io.brooklyn.attributes.Attribute;
import io.brooklyn.attributes.AttributeMap;
import io.brooklyn.attributes.BasicAttributeRef;
import io.brooklyn.attributes.ListAttributeRef;

import java.io.Serializable;

import static com.hazelcast.actors.utils.Util.notNull;

public abstract class Entity extends DispatchingActor {

    @Injected
    private ManagementContext managementContext;

    private AttributeMap attributeMap = new AttributeMap(this);

    @Override
    public void activate() throws Exception{
        super.activate();
        attributeMap.init(getHzInstance(), getRecipe());
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

    public void receive(SubscribeMessage subscribeMessage) {
        attributeMap.subscribe(subscribeMessage.attributeName, subscribeMessage.subscriber);
    }

    public static class SubscribeMessage implements Serializable {
        private final String attributeName;
        private final ActorRef subscriber;

        public SubscribeMessage(ActorRef subscriber, String attributeName) {
            this.attributeName = notNull(attributeName,"attributeName");
            this.subscriber = notNull(subscriber,"subscriber");
        }

        public SubscribeMessage(ActorRef subscriber, Attribute attribute) {
            this(subscriber, attribute.getName());
        }
    }
}
