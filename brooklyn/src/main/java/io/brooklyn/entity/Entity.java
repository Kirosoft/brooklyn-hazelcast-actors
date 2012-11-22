package io.brooklyn.entity;

import com.hazelcast.actors.actors.DispatchingActor;
import com.hazelcast.actors.api.ActorRecipe;
import com.hazelcast.actors.api.ActorRef;
import com.hazelcast.actors.api.Injected;
import io.brooklyn.AbstractMessage;
import io.brooklyn.ManagementContext;
import io.brooklyn.attributes.Attribute;
import io.brooklyn.attributes.AttributeMap;
import io.brooklyn.attributes.BasicAttributeRef;
import io.brooklyn.attributes.IntAttributeRef;
import io.brooklyn.attributes.ListAttributeRef;
import io.brooklyn.attributes.LongAttributeRef;

import static com.hazelcast.actors.utils.Util.notNull;

/**
 * Each Entity has an AttributeMap where all the values for Attributes are stored. This AttributeMap is backedup by
 * a Hazelcast distributed map and can be made durable using the MapStorage (doesn't work yet in Hazelcast 3). So
 * because the attributes are backed up by the map, when a node fails, changes in the map already have been replicated
 * to another node. When the actors are re-started, the previously stored attributes will be available to them.
 * <p/>
 * So long story short: everything you will put in the attribute-map will be highly available.
 */
public abstract class Entity extends DispatchingActor {

    @Injected
    private ManagementContext managementContext;

    private AttributeMap attributeMap = new AttributeMap(this);

    @Override
    public void activate() throws Exception {
        super.activate();

        ActorRecipe recipe = getRecipe();

        EntityConfig config = (EntityConfig) recipe.getProperties().get("config");
        attributeMap.init(getHzInstance(), getRecipe(), config);
    }


    public final AttributeMap getAttributeMap() {
        return attributeMap;
    }

    public final ActorRef newEntity(EntityConfig config) {
        return getManagementContext().newEntity(config);
    }

    public final void send(BasicAttributeRef<ActorRef> destination, Object msg) {
        send(destination.get(), msg);
    }

    public final EntityConfig getEntityConfig() {
        return (EntityConfig) getRecipe().getProperties().get("entityConfig");
    }

    public final ManagementContext getManagementContext() {
        return managementContext;
    }

    public final <E> ListAttributeRef<E> newListAttributeRef(String name, Class<E> type) {
        return newListAttributeRef(new Attribute<E>(name));
    }

    public final <E> ListAttributeRef<E> newListAttributeRef(Attribute<E> attribute) {
        return attributeMap.newListAttribute(attribute);
    }

    public final <E> BasicAttributeRef<E> newBasicAttributeRef(String name, Class<E> clazz) {
        return newBasicAttributeRef(new Attribute<E>(name));
    }

    public final <E> BasicAttributeRef<E> newBasicAttributeRef(String name, E defaultValue) {
        return newBasicAttributeRef(new Attribute<E>(name, defaultValue));
    }

    public final <E> BasicAttributeRef<E> newBasicAttributeRef(Attribute<E> attribute) {
        return attributeMap.newBasicAttributeRef(attribute);
    }

    public final <E> BasicAttributeRef<E> newBasicAttributeRef(String name) {
        return attributeMap.newBasicAttributeRef(new Attribute<E>(name));
    }

    public final IntAttributeRef newIntAttributeRef(Attribute<Integer> attribute) {
        return attributeMap.newIntAttribute(attribute);
    }

    public final IntAttributeRef newIntAttributeRef(String name, int defaultValue) {
        return attributeMap.newIntAttribute(new Attribute<>(name, defaultValue));
    }

    public final LongAttributeRef newLongAttributeRef(Attribute<Long> attribute) {
        return attributeMap.newLongAttribute(attribute);
    }

    public final LongAttributeRef newLongAttributeRef(String name, long defaultValue) {
        return attributeMap.newLongAttribute(new Attribute<>(name, defaultValue));
    }

    public void receive(Subscription subscription) {
        attributeMap.subscribe(subscription.attributeName, subscription.subscriber);
    }

    public void receive(AttributePublication publication) {
        System.out.println(publication);
        attributeMap.setAttribute(publication.attribute, publication.value);
    }

    public final void repeatingSelfNotification(Object msg, int delayMs) {
        getActorRuntime().repeatingNotification(self(), msg, delayMs);
    }

    public final void subscribeToAttribute(BasicAttributeRef<ActorRef> subscriber, ActorRef target, Attribute attribute) {
        getManagementContext().subscribeToAttribute(subscriber.get(), target, attribute);
    }

    public final void subscribeToAttribute(ActorRef subscriber, ActorRef target, Attribute attribute) {
        getManagementContext().subscribeToAttribute(subscriber, target, attribute);
    }

    public static class AttributePublication<E> extends AbstractMessage {
        private final Attribute<E> attribute;
        private final E value;

        public AttributePublication(BasicAttributeRef<Attribute<E>> attributeRef, E value) {
            this(attributeRef.get(), value);
        }

        public AttributePublication(Attribute<E> attribute, E value) {
            this.attribute = attribute;
            this.value = value;
        }

        public Attribute<E> getAttribute() {
            return attribute;
        }

        public E getValue() {
            return value;
        }
    }

    public static class Subscription extends AbstractMessage {
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
