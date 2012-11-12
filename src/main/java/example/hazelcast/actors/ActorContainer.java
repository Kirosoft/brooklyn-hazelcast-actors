package example.hazelcast.actors;

import com.hazelcast.core.HazelcastInstanceAware;
import com.hazelcast.nio.DataSerializable;
import com.hazelcast.spi.impl.NodeServiceImpl;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import static example.hazelcast.Util.notNull;

public final class ActorContainer implements DataSerializable {
    private Actor actor;
    private BlockingQueue mailbox = new LinkedBlockingQueue();
    private ActorRef ref;
    private ActorRecipe recipe;

    public ActorContainer(ActorRecipe recipe, ActorRef actorRef) {
        this.recipe = notNull(recipe,"recipe");
        this.ref = notNull(actorRef,"ref");
    }

    public void init(ActorRuntime actorRuntime, NodeServiceImpl nodeService, Map<String, Object> dependencies) {
        try {
            actor = recipe.actorClass.newInstance();
        } catch (InstantiationException e) {
            throw new RuntimeException(e);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }

        initAutowiredFields(dependencies);

        if (actor instanceof HazelcastInstanceAware) {
            ((HazelcastInstanceAware) actor).setHazelcastInstance(nodeService.getNode().hazelcastInstance);
        }

        if (actor instanceof ActorSystemAware) {
            ((ActorSystemAware) actor).setActorRuntime(actorRuntime);
        }

        if (actor instanceof ActorRefAware) {
            ((ActorRefAware) actor).setActorRef(ref);
        }

        if (actor instanceof ActorLifecycleAware) {
            ((ActorLifecycleAware) actor).init(recipe);
        }
    }

    public void initAutowiredFields(Map<String, Object> dependencies) {
        Class clazz = actor.getClass();
        for (; ; ) {
            Field[] fields = clazz.getDeclaredFields();
            for (Field field : fields) {
                if (field.isAnnotationPresent(Autowired.class)) {
                    Object dependency = dependencies.get(field.getName());
                    field.setAccessible(true);
                    try {
                        field.set(actor, dependency);
                    } catch (IllegalAccessException e) {
                        throw new RuntimeException(e);
                    }
                }
            }

            clazz = clazz.getSuperclass();
            if (clazz == null) {
                return;
            }
        }
    }

    public void stop() {
        if (actor instanceof ActorLifecycleAware) {
            ((ActorLifecycleAware) actor).stop();
        }
    }

    public void post(ActorRef sender, Object message) throws InterruptedException {
        if (sender == null) {
            mailbox.put(message);
        } else {
            mailbox.put(new Message(message, sender));
        }
    }

    public synchronized void processMessage() throws InterruptedException {
        Object m = mailbox.take();
        ActorRef sender;
        Object message;
        if (m instanceof Message) {
            message = ((Message) m).content;
            sender = ((Message) m).sender;
        } else {
            message = m;
            sender = null;
        }

        try {
            actor.receive(message, sender);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static class Message {
        private final Object content;
        private final ActorRef sender;

        Message(Object content, ActorRef sender) {
            this.content = content;
            this.sender = sender;
        }
    }

    @Override
    public void readData(DataInput in) throws IOException {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void writeData(DataOutput out) throws IOException {
        //To change body of implemented methods use File | Settings | File Templates.
    }
}
