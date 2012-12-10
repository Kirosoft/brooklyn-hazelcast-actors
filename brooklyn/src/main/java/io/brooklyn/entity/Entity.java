package io.brooklyn.entity;

import com.hazelcast.actors.actors.DispatchingActor;
import com.hazelcast.actors.api.ActorRecipe;
import com.hazelcast.actors.api.ActorRef;
import com.hazelcast.actors.api.Injected;
import com.hazelcast.actors.api.MessageDeliveryFailure;
import io.brooklyn.AbstractMessage;
import io.brooklyn.ManagementContext;
import io.brooklyn.attributes.Attribute;
import io.brooklyn.attributes.AttributeMap;
import io.brooklyn.attributes.BasicAttributeRef;
import io.brooklyn.attributes.IntAttributeRef;
import io.brooklyn.attributes.ListAttributeRef;
import io.brooklyn.attributes.LongAttributeRef;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.hazelcast.actors.utils.Util.notNull;

/**
 * Each Entity has an AttributeMap where all the values for Attributes are stored. This AttributeMap is backedup by
 * a Hazelcast distributed map and can be made durable using the MapStorage (doesn't run yet in Hazelcast 3). So
 * because the attributes are backed up by the map, when a node fails, changes in the map already have been replicated
 * to another node. When the actors are re-started, the previously stored attributes will be available to them.
 * <p/>
 * So long story short: everything you will put in the attribute-map will be highly available.
 */
public abstract class Entity extends DispatchingActor {

    private static final Logger log = LoggerFactory.getLogger(Entity.class);

    @Injected
    private ManagementContext managementContext;

    private AttributeMap attributeMap = new AttributeMap(this);

    @Override
    public void onActivation() throws Exception {
        super.onActivation();

        ActorRecipe recipe = getRecipe();

        EntityConfig config = (EntityConfig) recipe.getProperties().get("entityConfig");
        attributeMap.init(getHzInstance(), getRecipe(), config);
    }

    public final AttributeMap getAttributeMap() {
        return attributeMap;
    }

    /**
     * Spawns a new Entity and links it to the current Entity.
     *
     * Linking means that the current entity will receive the exit event of the newly spawned entity.
     *
     * @param config  the configuration for the entity.
     * @return the ActorRef of the created Entity.
     */
    public final ActorRef spawnAndLink(EntityConfig config) {
        return getManagementContext().spawnAndLink(self(), config);
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
        return attributeMap.newListAttributeRef(attribute);
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
        return attributeMap.newIntAttributeRef(attribute);
    }

    public final IntAttributeRef newIntAttributeRef(String name, int defaultValue) {
        return attributeMap.newIntAttributeRef(new Attribute<>(name, defaultValue));
    }

    public final LongAttributeRef newLongAttributeRef(Attribute<Long> attribute) {
        return attributeMap.newLongAttributeRef(attribute);
    }

    public final LongAttributeRef newLongAttributeRef(String name, long defaultValue) {
        return attributeMap.newLongAttributeRef(new Attribute<>(name, defaultValue));
    }

    public void receive(Subscription subscription) {
        if (log.isDebugEnabled()) log.debug(self() + ":Entity:" + subscription);
        attributeMap.subscribe(subscription.attributeName, subscription.subscriber);
    }

    public void receive(AttributePublication publication) {
        if (log.isDebugEnabled()) log.debug(self() + ":Entity:" + publication);

        attributeMap.setAttribute(publication.attribute, publication.value);
    }

    public void receive(MessageDeliveryFailure e){
        log.debug(""+e);
    }

    /**
     * Notify self is 'the' way for entities to do scheduled operations. E.g. tomcat checking jmx information. The
     * notify self will repeatedly send messages to 'self' with a certain delay. And therefor dealing with repeated
     * actions is just message processing; so it will not break the single threaded nature of the actor.
     *
     * @param msg
     * @param delayMs
     */
    public final void notifySelf(Object msg, int delayMs) {
        getActorRuntime().notify(self(), msg, delayMs);
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
