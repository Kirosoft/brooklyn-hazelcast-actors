package example.hazelcast.brooklyn;

import com.hazelcast.core.IMap;
import example.hazelcast.actors.AbstractActor;
import example.hazelcast.actors.ActorRecipe;
import example.hazelcast.actors.ActorRef;
import example.hazelcast.actors.Autowired;
import example.hazelcast.brooklyn.attributes.AttributeType;
import example.hazelcast.brooklyn.attributes.BasicAttribute;
import example.hazelcast.brooklyn.attributes.ListAttribute;

import java.io.Serializable;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public abstract class Entity extends AbstractActor {

    private IMap<String, Object> attributeMap;
    @Autowired
    private ManagementContext managementContext;

    @Override
    public void init(ActorRecipe actorRecipe) {
        super.init(actorRecipe);

        attributeMap = getHzInstance().getMap(self() + "-attributes");

        for (Map.Entry<String, Object> entry : actorRecipe.getProperties().entrySet()) {
            //todo: for the time being this is disabled since the put blocks.
            //    attributeMap.put(entry.getKey(), entry.getValue());
        }
    }

    public final ManagementContext getManagementContext() {
        return managementContext;
    }

    public final <E> ListAttribute<E> newListAttribute(final AttributeType<E> type) {
        return new ListAttribute<E>() {

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
                return self() + ".list." + type.getName();
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

    public final <E> BasicAttribute<E> newBasicAttribute(final AttributeType<E> type) {
        return new BasicAttribute<E>() {
            @Override
            public E get() {
                return (E) attributeMap.get(type.getName());
            }

            @Override
            public void set(E newValue) {
                Object oldValue = attributeMap.get(type.getName());
                if (noChange(oldValue, newValue)) {
                    return;
                }

                attributeMap.put(type.getName(), newValue);

                List<ActorRef> listeners = (List<ActorRef>) attributeMap.get(self() + "." + type.getName() + ".listeners");
                if (listeners != null) {
                    for (ActorRef listener : listeners) {
                        getActorRuntime().send(self(), listener, new SensorEvent(self(), type.getName(), oldValue, newValue));
                    }
                }
            }

            @Override
            public String getName() {
                return type.getName();
            }


            private <E> boolean noChange(Object oldValue, E newValue) {
                if (oldValue == newValue) return true;
                return oldValue != null && oldValue.equals(newValue);
            }
        };
    }


    public void receive(SubscribeMessage subscribeMessage, ActorRef sender) {
        String key = self() + "." + subscribeMessage.attributeName + ".listeners";
        List<ActorRef> listeners = (List<ActorRef>) attributeMap.get(key);
        if (listeners == null) {
            listeners = new LinkedList<ActorRef>();
        }
        listeners.add(subscribeMessage.subscriber);
        attributeMap.put(key, listeners);
        Object value = attributeMap.get(subscribeMessage.attributeName);
        getActorRuntime().send(self(), subscribeMessage.subscriber, new SensorEvent(self(), subscribeMessage.attributeName, value, value));
    }

    public static class SubscribeMessage implements Serializable {
        private final String attributeName;
        private final ActorRef subscriber;

        public SubscribeMessage(ActorRef subscriber, String attributeName) {
            this.attributeName = attributeName;
            this.subscriber = subscriber;
        }
    }
}
