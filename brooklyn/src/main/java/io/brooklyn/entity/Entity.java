package io.brooklyn.entity;

import com.hazelcast.actors.actors.DispatchingActor;
import com.hazelcast.actors.api.ActorRecipe;
import com.hazelcast.actors.api.ActorRef;
import com.hazelcast.actors.api.Injected;
import io.brooklyn.ManagementContext;
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
    public void activate() throws Exception {
        super.activate();

        ActorRecipe recipe = getRecipe();

        EntityConfig config = (EntityConfig)recipe.getProperties().get("config");
        attributeMap.init(getHzInstance(), getRecipe(),config);
    }

    public EntityConfig getEntityConfig(){
        return (EntityConfig) getRecipe().getProperties().get("entityConfig");
    }

    public final ManagementContext getManagementContext() {
        return managementContext;
    }

    public final <E> ListAttributeRef<E> newListAttributeRef(String name, Class<E> type){
        return newListAttributeRef(new Attribute<E>(name));
    }

    public final <E> ListAttributeRef<E> newListAttributeRef(final Attribute<E> attribute) {
        return attributeMap.newListAttribute(attribute);
    }

    public final <E> BasicAttributeRef<E> newBasicAttributeRef(String name, Class<E> clazz) {
        return newBasicAttributeRef(new Attribute<E>(name));
    }

    public final <E> BasicAttributeRef<E> newBasicAttributeRef(final Attribute<E> attribute) {
        return attributeMap.newBasicAttributeRef(attribute);
    }

    public void receive(Subscription subscription) {
        attributeMap.subscribe(subscription.attributeName, subscription.subscriber);
    }

    public static class Subscription implements Serializable {
        private final String attributeName;
        private final ActorRef subscriber;

        public Subscription(ActorRef subscriber, String attributeName) {
            this.attributeName = notNull(attributeName, "attributeName");
            this.subscriber = notNull(subscriber, "subscriber");
        }

        public Subscription(ActorRef subscriber, Attribute attribute) {
            this(subscriber, attribute.getName());
        }
    }
}
