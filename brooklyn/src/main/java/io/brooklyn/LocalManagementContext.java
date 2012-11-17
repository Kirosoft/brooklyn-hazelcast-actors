package io.brooklyn;

import com.hazelcast.actors.api.ActorRef;
import com.hazelcast.actors.api.ActorRuntime;
import com.hazelcast.actors.utils.MutableMap;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.IMap;
import io.brooklyn.activeobject.ActiveObject;
import io.brooklyn.activeobject.ActiveObjectActor;
import io.brooklyn.activeobject.ActiveObjectMessage;
import io.brooklyn.attributes.Attribute;
import net.sf.cglib.proxy.Enhancer;
import net.sf.cglib.proxy.MethodInterceptor;
import net.sf.cglib.proxy.MethodProxy;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static com.hazelcast.actors.utils.Util.notNull;


public class LocalManagementContext implements ManagementContext {

    private HazelcastInstance hzInstance;
    //todo: should use a MultiMap as soon as available again in Hazelcast 3.
    private IMap<String, Set<ActorRef>> namespaceMap;

    private ExecutorService distributedExecutorService;
    private ExecutorService localExecutor;
    private ActorRuntime actorRuntime;
    private IMap<String, Set<ActorRef>> namespaceSubscribersMap;

    public void init(HazelcastInstance hzInstance, ActorRuntime actorRuntime) {
        namespaceMap = hzInstance.getMap("namespace");
        namespaceSubscribersMap = hzInstance.getMap("namespace-subscribers");
        distributedExecutorService = hzInstance.getExecutorService("executor");
        localExecutor = Executors.newFixedThreadPool(10);
        this.actorRuntime = actorRuntime;
    }

    public void subscribeToAttribute(ActorRef listener, ActorRef target, Attribute attribute) {
        actorRuntime.send(listener, target, new Entity.SubscribeMessage(listener, attribute));
    }

    @Override
    public void executeLocally(Runnable task) {
        notNull(task, "task");
        localExecutor.execute(task);
    }

    @Override
    public void executeAnywhere(Runnable task) {
        notNull(task, "task");
        distributedExecutorService.execute(task);
    }

    @Override
    public <A extends ActiveObject> A newActiveObject(Class<A> activeObjectClass) {
        //todo: there should be a way to signal to the actorRuntime that we want a random location
        int partitionId = -1;
        return newActiveObject(activeObjectClass, partitionId, MutableMap.map());
    }

    @Override
    public <A extends ActiveObject> A newActiveObject(Class<A> activeObjectClass, int partitionId, Map<String, Object> config) {
        notNull(activeObjectClass, "activeObjectClass");


        Map<String, Object> actorConfig = MutableMap.map("activeObjectClass", activeObjectClass.getName(), "config", config);
        final ActorRef ref = actorRuntime.newActor(ActiveObjectActor.class, partitionId, actorConfig);

        return (A) Enhancer.create(activeObjectClass, new MethodInterceptor() {
            @Override
            public Object intercept(Object obj, Method method, Object[] args, MethodProxy proxy) throws Throwable {
                ActiveObjectMessage msg = new ActiveObjectMessage(method.getName(), args);
                actorRuntime.send(ref, msg);
                return null;
            }
        });
    }


    @Override
    public Set<ActorRef> getFromNameSpace(String nameSpace) {
        notNull(nameSpace, "nameSpace");

        namespaceMap.lock(nameSpace);
        try {
            Set<ActorRef> refs = namespaceMap.get(nameSpace);
            Set<ActorRef> result = new HashSet<ActorRef>();
            if (refs != null) {
                result.addAll(refs);
            }
            return refs;
        } finally {
            namespaceMap.unlock(nameSpace);
        }
    }

    @Override
    public void unregisterFromNamespace(String nameSpace, ActorRef ref) {
        notNull(nameSpace, "nameSpace");
        notNull(ref, "ref");

        namespaceMap.lock(nameSpace);
        try {
            Set<ActorRef> refs = namespaceMap.get(nameSpace);
            if (refs == null) {
                return;
            }
            if (!refs.remove(ref)) {
                return;
            }
            namespaceMap.put(nameSpace, refs);
        } finally {
            namespaceMap.unlock(nameSpace);
        }
    }

    @Override
    public void registerInNamespace(String nameSpace, ActorRef ref) {
        notNull(nameSpace, "nameSpace");
        notNull(ref, "ref");

        namespaceMap.lock(nameSpace);
        try {
            Set<ActorRef> refs = namespaceMap.get(nameSpace);
            if (refs == null) {
                refs = new HashSet<ActorRef>();
            }

            //if we are already registered, we are finished.
            if (!refs.add(ref)) {
                return;
            }
            namespaceMap.put(nameSpace, refs);
        } finally {
            namespaceMap.unlock(nameSpace);
        }

        namespaceSubscribersMap.lock(nameSpace);
        try {
            Set<ActorRef> subscribers = namespaceMap.get(nameSpace);
            if (subscribers == null) {
                return;
            }

            for (ActorRef subscriber : subscribers) {
                actorRuntime.send(subscriber, new NamespaceChange(ref, true,nameSpace));
            }
        } finally {
            namespaceSubscribersMap.unlock(nameSpace);

        }
    }

    @Override
    public void subscribeToNamespace(String nameSpace, ActorRef subscriber) {
        notNull(nameSpace, "nameSpace");
        notNull(subscriber, "subscriber");

        namespaceSubscribersMap.lock(nameSpace);
        try {
            Set<ActorRef> subscribers = namespaceSubscribersMap.get(nameSpace);
            if (subscribers == null) {
                return;
            }
            if (!subscribers.add(subscriber)) {
                return;
            }
            namespaceSubscribersMap.put(nameSpace, subscribers);
        } finally {
            namespaceSubscribersMap.unlock(nameSpace);
        }
    }


    //For the time being it is a simple mechanism; the driver class returned by the entity, is the actual class
    //of the driver instance to be used. In the future we can use the same mechanism as in Brooklyn.
    public SoftwareProcessDriver newDriver(SoftwareProcess entity) {
        notNull(entity, "entity");

        Class<? extends SoftwareProcessDriver> driver = entity.getDriverClass();
        try {
            Constructor<? extends SoftwareProcessDriver> driverConstructor = driver.getConstructor(entity.getClass());
            return driverConstructor.newInstance(entity);
        } catch (NoSuchMethodException e) {
            throw new RuntimeException(e);
        } catch (InvocationTargetException e) {
            throw new RuntimeException(e);
        } catch (InstantiationException e) {
            throw new RuntimeException(e);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }
}
