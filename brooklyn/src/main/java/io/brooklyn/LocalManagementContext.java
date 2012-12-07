package io.brooklyn;

import brooklyn.location.Location;
import brooklyn.location.basic.LocationRegistry;
import brooklyn.location.basic.SshMachineLocation;
import com.google.common.base.Throwables;
import com.hazelcast.actors.api.ActorRecipe;
import com.hazelcast.actors.api.ActorRef;
import com.hazelcast.actors.api.ActorRuntime;
import com.hazelcast.actors.utils.MutableMap;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.IMap;
import io.brooklyn.activeobject.ActiveObject;
import io.brooklyn.activeobject.ActiveObjectActor;
import io.brooklyn.activeobject.ActiveObjectMessage;
import io.brooklyn.attributes.Attribute;
import io.brooklyn.entity.Entity;
import io.brooklyn.entity.EntityConfig;
import io.brooklyn.entity.softwareprocess.SoftwareProcess;
import io.brooklyn.entity.softwareprocess.SoftwareProcessDriver;
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
    private LocationRegistry locationRegistry;

    public void init(HazelcastInstance hzInstance, ActorRuntime actorRuntime) {
        namespaceMap = hzInstance.getMap("namespace");
        namespaceSubscribersMap = hzInstance.getMap("namespace-subscribers");
        distributedExecutorService = hzInstance.getExecutorService("executor");
        localExecutor = Executors.newFixedThreadPool(10);
        this.actorRuntime = actorRuntime;
        this.locationRegistry = new LocationRegistry();
    }

    @Override
    public LocationRegistry getLocationRegistry() {
        return locationRegistry;
    }

    @Override
    public ActorRef spawn(EntityConfig entityConfig) {
        return spawn(null, entityConfig);
    }

    @Override
    public ActorRef spawn(Class<? extends Entity> entityClass) {
        return spawn(null, new EntityConfig(entityClass));
    }

    @Override
    public ActorRef spawnAndLink(ActorRef parent, Class<? extends Entity> entityClass) {
        return spawnAndLink(parent, new EntityConfig(entityClass));
    }

    @Override
    public ActorRef spawnAndLink(ActorRef parent, EntityConfig entityConfig) {
        return spawn(notNull(parent,"parent"),entityConfig);
    }

    private ActorRef spawn(ActorRef parent, EntityConfig entityConfig) {
        notNull(entityConfig, "entityConfig");

        int partitionId = 0;//todo: we need to fix the partition id.
        ActorRecipe actorRecipe = new ActorRecipe(
                entityConfig.getEntityClass(),
                parent,
                partitionId,
                MutableMap.map("entityConfig", entityConfig));
        return actorRuntime.newActor(actorRecipe);
    }

    public void subscribeToAttribute(ActorRef listener, ActorRef target, Attribute attribute) {
        actorRuntime.send(listener, target, new Entity.Subscription(listener, attribute));
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
        //todo: there should be a way to scheduleTask to the actorRuntime that we want a random location
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
            Set<ActorRef> result = new HashSet<>();
            if (refs != null) {
                result.addAll(refs);
            }
            return result;
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
                refs = new HashSet<>();
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
                actorRuntime.send(subscriber, new NamespaceChange(ref, true, nameSpace));
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


    public SoftwareProcessDriver newDriver(SoftwareProcess entity) {
        Class<SoftwareProcessDriver> driverInterface = entity.getDriverClass();
        Location location = entity.location.get();
        Class<SoftwareProcessDriver> driverClass;
        if (driverInterface.isInterface()) {
            String driverClassName = inferClassName(driverInterface, entity.location.get());
            try {
                driverClass = (Class<SoftwareProcessDriver>) entity.getClass().getClassLoader().loadClass(driverClassName);
            } catch (ClassNotFoundException e) {
                throw Throwables.propagate(e);
            }
        } else {
            driverClass = driverInterface;
        }

        Constructor<SoftwareProcessDriver> constructor = getConstructor(driverClass);
        try {
            constructor.setAccessible(true);
            return constructor.newInstance(entity, location);
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException e) {
            throw Throwables.propagate(e);
        }
    }

    private String inferClassName(Class<? extends SoftwareProcessDriver> driverInterface, Location location) {
        String driverInterfaceName = driverInterface.getName();

        if (location instanceof SshMachineLocation) {
            if (!driverInterfaceName.endsWith("Driver")) {
                throw new RuntimeException(String.format("Driver name [%s] doesn't end with 'Driver'", driverInterfaceName));
            }

            return driverInterfaceName.substring(0, driverInterfaceName.length() - "Driver".length()) + "SshDriver";
        } else {
            //TODO: Improve
            throw new RuntimeException("Currently only SshMachineLocation is supported, but location=" + location + " for driver +" + driverInterface);
        }
    }

    private Constructor<SoftwareProcessDriver> getConstructor(Class<SoftwareProcessDriver> driverClass) {
        for (Constructor<?> constructor : driverClass.getConstructors()) {
            if (constructor.getParameterTypes().length == 2) {
                return (Constructor<io.brooklyn.entity.softwareprocess.SoftwareProcessDriver>) constructor;
            }
        }

        //TODO:
        throw new RuntimeException(String.format("Class [%s] has no constructor with 2 arguments", driverClass.getName()));
    }
}
