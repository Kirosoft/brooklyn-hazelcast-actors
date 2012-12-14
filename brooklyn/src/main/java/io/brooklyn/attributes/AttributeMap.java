package io.brooklyn.attributes;

import brooklyn.location.Location;
import brooklyn.location.PortRange;
import brooklyn.location.PortSupplier;
import com.hazelcast.actors.api.ActorRecipe;
import com.hazelcast.actors.api.ActorRef;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.IMap;
import io.brooklyn.entity.Entity;
import io.brooklyn.entity.EntityConfig;
import io.brooklyn.entity.EntityReference;
import io.brooklyn.entity.PlatformComponent;

import java.io.Serializable;
import java.util.*;

import static com.hazelcast.actors.utils.Util.notNull;


/**
 * Stores Attributes.
 * <p/>
 * This class is very similar to the {@link brooklyn.event.basic.AttributeMap}.
 */
//TODO: The Attributes don't need to go to the attributeMap for every read. The can store the previous written value.
//This will certainly speed up access to the Attributemap
public final class AttributeMap {

    //TODO:
    //Instead of using a string as key, which leads to a lot of remoting because the keys probably are in a different
    //partition than then entity; we should inject a partition aware key so that all attributes for a specific entity
    //end up in the same partition as that entity. Currently the attributes are all over the place.
    private IMap<String, Object> attributeMap;
    private IMap<String, Object> attributeSupportMap;

    private final Entity entity;

    public AttributeMap(Entity entity) {
        this.entity = notNull(entity, "entity");
    }

    public Map<String, Object> getInheritableAttributes() {
        Map<String, Object> map = new HashMap<>();

        //TODO: Currently we copy all attributes, but we should only copy attributes that are inheritable
        if (attributeMap != null) {
        //   map.putAll(attributeMap);
        }
        return map;
    }

    public void init(HazelcastInstance hazelcastInstance, ActorRecipe actorRecipe, EntityConfig config) {
        this.attributeMap = hazelcastInstance.getMap(entity.self() + "-attributeMap");
        this.attributeSupportMap = hazelcastInstance.getMap(entity.self() + "-attributeSupportMap");

        //copy all the properties from the recipe.
        Set<String> strings = actorRecipe.getProperties().keySet();
        for (String key : strings) {
            Object value = actorRecipe.getProperties().get(key);
            attributeMap.put(key, value);
        }

        //copy all the properties from the entity config.
        if (config != null) {
            strings = config.getProperties().keySet();
            for (String key : strings) {
                Object value = config.getProperties().get(key);
                attributeMap.put(key, value);
            }
        }
    }

    public <E> void setAttribute(AttributeType<E> attributeType, E newValue) {
        notNull(attributeType, "attributeType");

        Object oldValue = attributeMap.get(attributeType.getName());
        if (noChange(oldValue, newValue)) {
            return;
        }

        attributeMap.put(attributeType.getName(), newValue);

        List<ActorRef> subscribers = (List<ActorRef>) attributeSupportMap.get(getSubscriberKey(attributeType.getName()));
        if (subscribers == null) {
            return;
        }

        for (ActorRef subscriber : subscribers) {
            SensorEvent event = new SensorEvent(entity.self(), attributeType.getName(), oldValue, newValue);
            entity.getActorRuntime().send(entity.self().toActorRef(), subscriber, event);
        }
    }

    public <E> E getAttribute(AttributeType<E> attributeType) {
        notNull(attributeType, "attributeType");

        if (attributeMap.containsKey(attributeType.getName())) {
            return (E) attributeMap.get(attributeType.getName());
        }

        E value = attributeType.getDefaultValue();
        attributeMap.put(attributeType.getName(), value);
        return value;
    }

    private <E> boolean noChange(Object oldValue, E newValue) {
        if (oldValue == newValue) return true;
        return oldValue != null && oldValue.equals(newValue);
    }

    public void subscribe(String attributeName, EntityReference subscriber) {
        notNull(attributeName, "attributeName");
        notNull(subscriber, "subscriber");

        String key = getSubscriberKey(attributeName);
        List<EntityReference> subscribers = (List<EntityReference>) attributeSupportMap.get(key);
        if (subscribers == null) {
            subscribers = new LinkedList<>();
        }
        subscribers.add(subscriber);
        attributeSupportMap.put(key, subscribers);
        Object value = attributeMap.get(attributeName);
        entity.getActorRuntime().send(
                entity.self().toActorRef(),
                subscriber.toActorRef(),
                new SensorEvent(entity.self(), attributeName, value, value));
    }

    private String getSubscriberKey(String attributeName) {
        return entity.self() + "." + attributeName + ".subscribers";
    }

    public <E> ReferenceAttribute<E> newReferenceAttribute(final AttributeType<E> attributeType) {
        notNull(attributeType, "attributeType");
        return new ReferenceAttributeImpl<>(attributeType);
    }

    public IntAttribute newIntAttribute(final AttributeType<Integer> attributeType) {
        notNull(attributeType, "attributeType");
        return new IntAttributeImpl(attributeType);
    }

    public LongAttribute newLongAttribute(final AttributeType<Long> attributeType) {
        notNull(attributeType, "attributeType");
        return new LongAttributeImpl(attributeType);
    }

    public final <E> ListAttribute<E> newListAttribute(final AttributeType<E> attributeType) {
        notNull(attributeType, "attributeType");
        return new ListAttributeImpl<>(attributeType);
    }

    public final RelationsAttribute newRelationsAttribute(final AttributeType<EntityReference> attributeType) {
        notNull(attributeType, "attributeType");
        return new RelationsAttributeImpl(attributeType);
    }

    public final PortAttribute newPortAttribute(final AttributeType<PortRange> attributeType) {
        notNull(attributeType, "attributeType");
        return new PortAttributeImpl(attributeType);
    }

    private class IntAttributeImpl implements IntAttribute {

        private final AttributeType<Integer> attributeType;

        public IntAttributeImpl(AttributeType<Integer> attributeType) {
            this.attributeType = attributeType;
        }

        @Override
        public int get() {
            return getAttribute(attributeType);
        }

        @Override
        public void set(int newValue) {
            setAttribute(attributeType, newValue);
        }

        @Override
        public int getAndInc() {
            int oldValue = getAttribute(attributeType);
            setAttribute(attributeType, oldValue + 1);
            return oldValue;
        }

        @Override
        public String getName() {
            return attributeType.getName();
        }

        @Override
        public String toString() {
            return Integer.toString(get());
        }

        @Override
        public AttributeType getAttributeType() {
            return attributeType;
        }
    }

    private class LongAttributeImpl implements LongAttribute {
        private final AttributeType<Long> attributeType;

        public LongAttributeImpl(AttributeType<Long> attributeType) {
            this.attributeType = attributeType;
        }

        @Override
        public long get() {
            return getAttribute(attributeType);
        }

        @Override
        public void set(long newValue) {
            setAttribute(attributeType, newValue);
        }

        @Override
        public long getAndInc() {
            long oldValue = getAttribute(attributeType);
            setAttribute(attributeType, oldValue + 1);
            return oldValue;
        }

        @Override
        public String getName() {
            return attributeType.getName();
        }

        @Override
        public String toString() {
            return Long.toString(get());
        }

        @Override
        public AttributeType getAttributeType() {
            return attributeType;
        }
    }

    private class RelationsAttributeImpl extends ListAttributeImpl<EntityReference> implements RelationsAttribute {

        public RelationsAttributeImpl(AttributeType<EntityReference> attributeType) {
            super(attributeType);
        }

        private String getRegistrationsKey() {
            return getName() + ".registrations";
        }

        @Override
        public void registerOnChildren(AttributeType childAttribute, ActorRef subscriber) {
            Set<ChildAttributeRegistration> registrations = (Set<ChildAttributeRegistration>) attributeSupportMap.get(getRegistrationsKey());
            if (registrations == null) {
                registrations = new HashSet();
            }
            registrations.add(new ChildAttributeRegistration(subscriber, childAttribute));
            attributeSupportMap.put(getRegistrationsKey(), registrations);
        }

        @Override
        public void add(EntityReference item) {
            super.add(item);

            Set<ChildAttributeRegistration> registrations = (Set<ChildAttributeRegistration>) attributeSupportMap.get(getRegistrationsKey());
            if (registrations != null) {
                for (ChildAttributeRegistration registration : registrations) {
                    subscribe(registration.attributeType.getName(), new EntityReference(registration.subscriber));
                }
            }
        }
    }

    static class ChildAttributeRegistration implements Serializable {
        public final AttributeType attributeType;
        public final ActorRef subscriber;

        public ChildAttributeRegistration(ActorRef subscriber, AttributeType attributeType) {
            this.attributeType = notNull(attributeType, "attributeType");
            this.subscriber = notNull(subscriber, "subscriber");
        }
    }

    private class ListAttributeImpl<E> implements ListAttribute<E> {

        private final AttributeType<E> attributeType;

        public ListAttributeImpl(AttributeType<E> attributeType) {
            this.attributeType = attributeType;
        }

        @Override
        public Iterator<E> iterator() {
            List<E> result = new LinkedList();
            String key = getKey();
            List<E> list = (List<E>) attributeMap.get(key);
            if (list != null) {
                result.addAll(list);
            }
            return result.iterator();
        }

        @Override
        public E removeFirst() {
            String key = getKey();
            List<E> list = (List<E>) attributeMap.get(key);
            if (list == null) {
                throw new IndexOutOfBoundsException();
            }

            E removed = list.remove(0);
            attributeMap.put(key, list);
            return removed;
        }

        @Override
        public E get(int index) {
            String key = getKey();
            List<E> list = (List<E>) attributeMap.get(key);
            if (list == null) {
                throw new IndexOutOfBoundsException();
            }
            return list.get(index);
        }

        @Override
        public boolean isEmpty() {
            return size() == 0;
        }

        @Override
        public int size() {
            String key = getKey();
            List<E> list = (List<E>) attributeMap.get(key);
            return list == null ? 0 : list.size();
        }

        @Override
        public void add(E item) {
            String key = getKey();
            List<E> list = (List<E>) attributeMap.get(key);
            if (list == null) {
                list = new LinkedList<E>();
            }
            list.add(item);
            attributeMap.put(key, list);
        }

        private String getKey() {
            return entity.self() + ".list." + attributeType.getName();
        }

        @Override
        public String getName() {
            return attributeType.getName();
        }

        @Override
        public boolean remove(E item) {
            String key = getKey();
            List<E> list = (List<E>) attributeMap.get(key);
            if (list == null) {
                return false;
            }
            boolean change = list.remove(item);
            if (change)
                attributeMap.put(key, list);
            return change;
        }

        @Override
        public String toString() {
            String key = getKey();
            List list = (List) attributeMap.get(key);
            return list == null ? Collections.EMPTY_LIST.toString() : list.toString();
        }

        @Override
        public AttributeType getAttributeType() {
            return attributeType;
        }
    }

    private class PortAttributeImpl implements PortAttribute {
        private final AttributeType<PortRange> attributeType;

        public PortAttributeImpl(AttributeType<PortRange> attributeType) {
            this.attributeType = attributeType;
        }

        @Override
        public int get() {
            String portName = attributeType.getName() + ".port";
            Integer port = (Integer) attributeMap.get(portName);
            if (port == null) {
                if ((!(entity instanceof PlatformComponent))) {
                    throw new IllegalStateException("Only PlatformComponent can have PortRange attributes");
                }
                Location location = ((PlatformComponent) entity).location.get();
                PortRange portRange = getAttribute(attributeType);
                PortSupplier portSupplier = (PortSupplier) location;
                port = portSupplier.obtainPort(portRange);
                if (port == -1) {
                    throw new RuntimeException();
                }
                attributeMap.put(portName, port);
            }

            return port;
        }

        @Override
        public String getName() {
            return attributeType.getName();
        }

        @Override
        public String toString() {
            return Integer.toString(get());
        }

        @Override
        public AttributeType getAttributeType() {
            return attributeType;
        }
    }

    private class ReferenceAttributeImpl<E> implements ReferenceAttribute<E> {
        private final AttributeType<E> attributeType;

        public ReferenceAttributeImpl(AttributeType<E> attributeType) {
            this.attributeType = attributeType;
        }

        @Override
        public E get() {
            return getAttribute(attributeType);
        }

        @Override
        public void set(E newValue) {
            setAttribute(attributeType, newValue);
        }

        @Override
        public String getName() {
            return attributeType.getName();
        }

        @Override
        public boolean isNull() {
            return get() == null;
        }

        @Override
        public String toString() {
            E value = get();
            return value == null ? "null" : value.toString();
        }

        @Override
        public AttributeType getAttributeType() {
            return attributeType;
        }
    }
}
