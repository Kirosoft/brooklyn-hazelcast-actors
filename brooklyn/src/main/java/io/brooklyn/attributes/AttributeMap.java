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
import io.brooklyn.entity.PlatformComponent;

import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import static com.hazelcast.actors.utils.Util.notNull;

public final class AttributeMap {

    private IMap<String, Object> attributeMap;
    private final Entity entity;

    public AttributeMap(Entity entity) {
        this.entity = entity;
    }

    public void init(HazelcastInstance hazelcastInstance, ActorRecipe actorRecipe, EntityConfig config) {
        this.attributeMap = hazelcastInstance.getMap(entity.self() + "-attributes");

        Set<String> strings = actorRecipe.getProperties().keySet();
        for (String key : strings) {
            Object value = actorRecipe.getProperties().get(key);
            attributeMap.put(key, value);
        }

        if (config != null) {
            strings = config.getProperties().keySet();
            for (String key : strings) {
                Object value = config.getProperties().get(key);
                attributeMap.put(key, value);
            }
        }
    }

    public <E> void setAttribute(Attribute<E> attribute, E newValue) {
        notNull(attribute, "attribute");

        Object oldValue = attributeMap.get(attribute.getName());
        if (noChange(oldValue, newValue)) {
            return;
        }

        attributeMap.put(attribute.getName(), newValue);

        List<ActorRef> subscribers = (List<ActorRef>) attributeMap.get(getSubscriberKey(attribute.getName()));
        if (subscribers == null) {
            return;
        }

        for (ActorRef subscriber : subscribers) {
            SensorEvent event = new SensorEvent(entity.self(), attribute.getName(), oldValue, newValue);
            entity.getActorRuntime().send(entity.self(), subscriber, event);
        }
    }

    public <E> E getAttribute(Attribute<E> attribute) {
        notNull(attribute, "attribute");
        E value = (E) attributeMap.get(attribute.getName());

        //todo: this is a dirty hack to make sure the default value is in the map
        //this hack is temporarily needed to deal with a dealock in Hazelcast map.
        if (value == null) {
            //value = (E) entity.getRecipe().getProperties().get(attribute.getName());
            //
            //if (value == null) {
            //    EntityConfig entityConfig = (EntityConfig) entity.getRecipe().getProperties().get("entityConfig");
            //    if (entityConfig != null) {
            //        value = (E) entityConfig.getProperty(attribute.getName());
            //    }
            //}

            if (value == null) {
                value = attribute.getDefaultValue();
            }
            attributeMap.put(attribute.getName(), value);
        }
        return value;
    }


    private <E> boolean noChange(Object oldValue, E newValue) {
        if (oldValue == newValue) return true;
        return oldValue != null && oldValue.equals(newValue);
    }

    public void subscribe(String attributeName, ActorRef subscriber) {
        notNull(attributeName, "attributeName");
        notNull(subscriber, "subscriber");

        String key = getSubscriberKey(attributeName);
        List<ActorRef> subscribers = (List<ActorRef>) attributeMap.get(key);
        if (subscribers == null) {
            subscribers = new LinkedList<>();
        }
        subscribers.add(subscriber);
        attributeMap.put(key, subscribers);
        Object value = attributeMap.get(attributeName);
        entity.getActorRuntime().send(
                entity.self(),
                subscriber,
                new SensorEvent(entity.self(), attributeName, value, value));
    }

    private String getSubscriberKey(String attributeName) {
        return entity.self() + "." + attributeName + ".subscribers";
    }

    public final <E> BasicAttributeRef<E> newBasicAttributeRef(final Attribute<E> attribute) {
        notNull(attribute, "attribute");

        return new BasicAttributeRef<E>() {
            @Override
            public E get() {
                return getAttribute(attribute);
            }

            @Override
            public void set(E newValue) {
                setAttribute(attribute, newValue);
            }

            @Override
            public String getName() {
                return attribute.getName();
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
        };
    }

    public IntAttributeRef newIntAttributeRef(final Attribute<Integer> attribute) {
        notNull(attribute, "attribute");

        return new IntAttributeRef() {

            @Override
            public int get() {
                return getAttribute(attribute);
            }

            @Override
            public void set(int newValue) {
                setAttribute(attribute, newValue);
            }

            @Override
            public int getAndInc() {
                int oldValue = getAttribute(attribute);
                setAttribute(attribute, oldValue + 1);
                return oldValue;
            }

            @Override
            public String getName() {
                return attribute.getName();
            }

            @Override
            public String toString() {
                return Integer.toString(get());
            }
        };
    }

    public LongAttributeRef newLongAttributeRef(final Attribute<Long> attribute) {
        return new LongAttributeRef() {
            @Override
            public long get() {
                return getAttribute(attribute);
            }

            @Override
            public void set(long newValue) {
                setAttribute(attribute, newValue);
            }

            @Override
            public long getAndInc() {
                long oldValue = getAttribute(attribute);
                setAttribute(attribute, oldValue + 1);
                return oldValue;
            }

            @Override
            public String getName() {
                return attribute.getName();
            }

            @Override
            public String toString() {
                return Long.toString(get());
            }
        };
    }

    public final <E> ListAttributeRef<E> newListAttributeRef(final Attribute<E> attribute) {
        notNull(attribute, "attribute");

        return new ListAttributeRef<E>() {

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
                return entity.self() + ".list." + attribute.getName();
            }

            @Override
            public String getName() {
                return getName();
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
        };
    }

    public final PortAttributeRef newPortAttributeRef(final Attribute<PortRange> attribute) {
        notNull(attribute, "attribute");

        return new PortAttributeRef() {
            @Override
            public int get() {
                String portName = attribute.getName() + ".port";
                Integer port = (Integer) attributeMap.get(portName);
                if (port == null) {
                    if ((!(entity instanceof PlatformComponent))) {
                        throw new IllegalStateException("Only PlatformComponent can have PortRange attributes");
                    }
                    Location location = ((PlatformComponent) entity).location.get();
                    PortRange portRange = getAttribute(attribute);
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
                return attribute.getName();
            }

            @Override
            public String toString() {
                return Integer.toString(get());
            }
        };
    }


}
