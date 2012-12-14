package io.brooklyn.entity;

import com.hazelcast.actors.api.*;
import com.hazelcast.actors.api.exceptions.UnprocessedException;
import io.brooklyn.AbstractMessage;
import io.brooklyn.ManagementContext;
import io.brooklyn.attributes.*;
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
public abstract class Entity {

    private static final Logger log = LoggerFactory.getLogger(Entity.class);

    EntityActor entityActor;

    private AttributeMap attributeMap = new AttributeMap(this);

    public void onActivation() throws Exception {
        ActorRecipe recipe = entityActor.getRecipe();

        EntityConfig config = (EntityConfig) recipe.getProperties().get("entityConfig");
        attributeMap.init(entityActor.getHzInstance(), entityActor.getRecipe(), config);
    }

    public EntityReference self() {
        return entityActor.entityReference;
    }

    public final AttributeMap getAttributeMap() {
        return attributeMap;
    }

    public void onUnhandledMessage(Object msg, EntityReference sender) {
        String id = sender == null ? "unknown" : sender.getId();
        throw new UnprocessedException("No receive method found on Entity.class: " + getClass().getName() +
                " for message.class:" + msg.getClass().getName() + " send by: " + id);
    }

    /**
     * Spawns a new Entity and links it to the current Entity.
     * <p/>
     * Linking means that the current entity will receive the exit event of the newly spawned entity.
     *
     * @param config the configuration for the entity.
     * @return the ActorRef of the created Entity.
     */
    public final EntityReference spawnAndLink(EntityConfig config) {
        return getManagementContext().spawnAndLink(self(), config);
    }

    public final void send(EntityReference destination, Object msg) {
        getManagementContext().send(destination, msg);
    }

    public final void send(ReferenceAttribute<EntityReference> destination, Object msg) {
        getManagementContext().send(destination.get(), msg);
    }

    public final EntityConfig getEntityConfig() {
        return (EntityConfig) entityActor.getRecipe().getProperties().get("entityConfig");
    }

    public final ActorRuntime getActorRuntime() {
        return entityActor.getActorRuntime();
    }

    public final ManagementContext getManagementContext() {
        return entityActor.managementContext;
    }

    public final RelationsAttribute newRelationsAttribute(String name) {
        return attributeMap.newRelationsAttribute(new AttributeType(name));
    }

    //TODO: type is not used.
    public final <E> ListAttribute<E> newListAttribute(String name, Class<E> type) {
        return newListAttribute(new AttributeType<E>(name));
    }

    public final <E> ListAttribute<E> newListAttribute(AttributeType<E> attributeType) {
        return attributeMap.newListAttribute(attributeType);
    }

    //TODO: clazz is not used.
    public final <E> ReferenceAttribute<E> newReferenceAttribute(String name, Class<E> clazz) {
        return newReferenceAttribute(new AttributeType<E>(name));
    }

    public final <E> ReferenceAttribute<E> newReferenceAttribute(String name, E defaultValue) {
        return newReferenceAttribute(new AttributeType<>(name, defaultValue));
    }

    public final <E> ReferenceAttribute<E> newReferenceAttribute(AttributeType<E> attributeType) {
        return attributeMap.newReferenceAttribute(attributeType);
    }

    public final <E> ReferenceAttribute<E> newReferenceAttribute(String name) {
        return attributeMap.newReferenceAttribute(new AttributeType<E>(name));
    }

    public final IntAttribute newIntAttribute(AttributeType<Integer> attributeType) {
        return attributeMap.newIntAttribute(attributeType);
    }

    public final IntAttribute newIntAttribute(String name, int defaultValue) {
        return attributeMap.newIntAttribute(new AttributeType<>(name, defaultValue));
    }

    public final LongAttribute newLongAttribute(AttributeType<Long> attributeType) {
        return attributeMap.newLongAttribute(attributeType);
    }

    public final LongAttribute newLongAttribute(String name, long defaultValue) {
        return attributeMap.newLongAttribute(new AttributeType<>(name, defaultValue));
    }

    public void receive(AttributeSubscription subscription) {
        if (log.isErrorEnabled()) log.error(self() + ":Entity:" + subscription);
        attributeMap.subscribe(subscription.attributeName, subscription.subscriber);
    }

    public void receive(RelationsAttributeSubscription subscription) {
        if (log.isErrorEnabled()) log.error(self() + ":Entity:" + subscription);

        //todo: this code is nasty:
        String relationAttributeName = subscription.relationAttributeName;
        AttributeType attributeType = new AttributeType<RelationsAttribute>(relationAttributeName);
        attributeMap.newRelationsAttribute(attributeType).registerOnChildren(subscription.attributeType, subscription.subscriber);
    }

    public void receive(AttributePublication publication) {
        if (log.isDebugEnabled()) log.debug(self() + ":Entity:" + publication);
        attributeMap.setAttribute(publication.attributeType, publication.value);
    }

    public void receive(MessageDeliveryFailure e) {
        log.debug("" + e);
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
        getActorRuntime().notify(self().toActorRef(), msg, delayMs);
    }

    public final void subscribeToAttribute(ReferenceAttribute<EntityReference> subscriber, EntityReference target, AttributeType attributeType) {
        getManagementContext().subscribeToAttribute(subscriber.get(), target, attributeType);
    }

    public final void subscribeToAttribute(EntityReference subscriber, EntityReference target, AttributeType attributeType) {
        getManagementContext().subscribeToAttribute(subscriber, target, attributeType);
    }

    //message when a publication needs to be done to an attribute. This is useful for external mechanisms like enrichers
    public static class AttributePublication<E> extends AbstractMessage {
        private final AttributeType<E> attributeType;
        private final E value;

        public AttributePublication(ReferenceAttribute<AttributeType<E>> attributeRef, E value) {
            this(attributeRef.get(), value);
        }

        public AttributePublication(AttributeType<E> attributeType, E value) {
            this.attributeType = attributeType;
            this.value = value;
        }

        public AttributeType<E> getAttributeType() {
            return attributeType;
        }

        public E getValue() {
            return value;
        }
    }

    //This message indicates we want to listen to a certain attribute.
    public static class AttributeSubscription extends AbstractMessage {
        private final String attributeName;
        private final EntityReference subscriber;

        public AttributeSubscription(EntityReference subscriber, String attributeName) {
            this.attributeName = notNull(attributeName, "attributeName");
            this.subscriber = notNull(subscriber, "subscriber");
        }

        public AttributeSubscription(EntityReference subscriber, AttributeType attributeType) {
            this(subscriber, attributeType.getName());
        }
    }

    //This message indicates that we want to be subscribed to an attribute of the children of some relationsattribute.
    //E.g. when we would have a relation 'webapps' and each entity has an heap attribute, then using this subscription
    //we can automatically subscribe to all webapps their heap attribute. Also for newly added relations the subscription
    //will automatically be applied.
    public static class RelationsAttributeSubscription extends AbstractMessage {
        public final String relationAttributeName;
        public final AttributeType attributeType;
        public final ActorRef subscriber;

        public RelationsAttributeSubscription(String relationAttributeName, EntityReference subscriber, AttributeType attributeType) {
            this.relationAttributeName = notNull(relationAttributeName, "relationAttributeName");
            this.attributeType = notNull(attributeType, "attribute");
            this.subscriber = notNull(subscriber, "subscriber").toActorRef();
        }

        public RelationsAttributeSubscription(String relationAttributeName, ActorRef subscriber, AttributeType attributeType) {
            this.relationAttributeName = notNull(relationAttributeName, "relationAttributeName");
            this.attributeType = notNull(attributeType, "attribute");
            this.subscriber = notNull(subscriber, "subscriber");
        }

        public RelationsAttributeSubscription(String relationAttributeName, ReferenceAttribute<EntityReference> subscribers, AttributeType attributeType) {
            this(relationAttributeName, subscribers.get(), attributeType);
        }
    }
}
