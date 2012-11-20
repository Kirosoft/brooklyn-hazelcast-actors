package io.brooklyn.attributes;

import com.hazelcast.actors.api.ActorRecipe;
import com.hazelcast.actors.api.ActorRef;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.IMap;
import io.brooklyn.entity.Entity;
import io.brooklyn.entity.EntityConfig;

import java.util.LinkedList;
import java.util.List;

import static com.hazelcast.actors.utils.Util.notNull;

public final class AttributeMap {

    private IMap<String, Object> attributeMap;
    private final Entity entity;

    public AttributeMap(Entity entity) {
        this.entity = entity;
    }

    public void init(HazelcastInstance hazelcastInstance, ActorRecipe actorRecipe, EntityConfig config) {
        this.attributeMap = hazelcastInstance.getMap(entity.self() + "-attributes");


        //for (Map.Entry<String, Object> entry : actorRecipe.getProperties().entrySet()) {
        //todo: for the time being this is disabled since the put blocks.
        //attributeMap.put(entry.getKey(), entry.getValue());
        //}
    }

    public <E> void setAttribute(Attribute<E> attribute, E newValue) {
        notNull(attribute, "attribute");

        Object oldValue = attributeMap.get(attribute.getName());
        if (noChange(oldValue, newValue)) {
            return;
        }

        attributeMap.put(attribute.getName(), newValue);

        List<ActorRef> listeners = (List<ActorRef>) attributeMap.get(getListenerKey(attribute.getName()));
        if (listeners == null) {
            return;
        }

        for (ActorRef listener : listeners) {
            SensorEvent event = new SensorEvent(entity.self(), attribute.getName(), oldValue, newValue);
            entity.getActorRuntime().send(entity.self(), listener, event);
        }
    }

    public final <E> BasicAttributeRef<E> newBasicAttributeRef(final Attribute<E> attribute) {
        notNull(attribute, "attribute");
        return new BasicAttributeRefImpl<E>(attribute);
    }

    public <E> E getAttribute(Attribute<E> attribute) {
        notNull(attribute, "attribute");
        E value = (E) attributeMap.get(attribute.getName());

        //todo: this is a dirty hack to make sure the default value is in the map
        //this hack is temporarily needed to deal with a dealock in Hazelcast map.
        if (value == null) {
            value = (E) entity.getRecipe().getProperties().get(attribute.getName());

            if (value == null) {
                EntityConfig entityConfig = (EntityConfig) entity.getRecipe().getProperties().get("entityConfig");
                if (entityConfig != null) {
                    value = (E) entityConfig.getProperty(attribute.getName());
                }
            }

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

        String key = getListenerKey(attributeName);
        List<ActorRef> listeners = (List<ActorRef>) attributeMap.get(key);
        if (listeners == null) {
            listeners = new LinkedList<ActorRef>();
        }
        listeners.add(subscriber);
        attributeMap.put(key, listeners);
        Object value = attributeMap.get(attributeName);
        entity.getActorRuntime().send(
                entity.self(),
                subscriber,
                new SensorEvent(entity.self(), attributeName, value, value));
    }

    private String getListenerKey(String attributeName) {
        return entity.self() + "." + attributeName + ".listeners";
    }

    public IntAttributeRef newIntAttribute(final Attribute<Integer> attribute) {
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

            public String toString() {
                return "" + get();
            }
        };
    }

    public LongAttributeRef newLongAttribute(final Attribute<Long> attribute) {
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

            public String toString() {
                return "" + get();
            }
        };
    }

    private class BasicAttributeRefImpl<E> implements BasicAttributeRef<E> {
        private final Attribute<E> attribute;

        public BasicAttributeRefImpl(Attribute<E> attribute) {
            this.attribute = notNull(attribute, "attribute");
        }

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
        public String toString() {
            E value = get();
            return value == null ? "null" : value.toString();
        }
    }

    public final <E> ListAttributeRef<E> newListAttribute(final Attribute<E> attribute) {
        notNull(attribute, "attribute");

        return new ListAttributeRef<E>() {

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
            public void remove(E item) {
                String key = getKey();
                List<E> list = (List<E>) attributeMap.get(key);
                if (list == null) {
                    return;
                }
                list.remove(item);
                attributeMap.put(key, list);
            }
        };
    }
}
