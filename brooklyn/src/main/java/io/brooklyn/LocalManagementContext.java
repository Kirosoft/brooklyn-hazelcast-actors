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
import io.brooklyn.attributes.AttributeType;
import io.brooklyn.entity.Entity;
import io.brooklyn.entity.EntityActor;
import io.brooklyn.entity.EntityConfig;
import io.brooklyn.entity.EntityReference;
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
    private IMap<String, Set<EntityReference>> namespaceMap;

    private ExecutorService distributedExecutorService;
    private ExecutorService localExecutor;
    private ActorRuntime actorRuntime;
    private IMap<String, Set<EntityReference>> namespaceSubscribersMap;
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
    public void send(EntityReference destination, Object msg) {
         actorRuntime.send(destination.toActorRef(),msg);
    }

    @Override
    public EntityReference spawn(EntityConfig config) {
        return spawn(null, config);
    }

    @Override
    public EntityReference spawnAndLink(EntityReference caller, EntityConfig config) {
        return spawn(notNull(caller,"caller"), config);
    }

    private EntityReference spawn(EntityReference caller, EntityConfig entityConfig) {
        notNull(entityConfig, "entityConfig");

        int partitionId = 0;//todo: we need to fix the partition id.
        ActorRecipe actorRecipe = new ActorRecipe(
                EntityActor.class,
                partitionId,
                MutableMap.map("entityConfig", entityConfig));
        ActorRef actorRef = actorRuntime.spawnAndLink(caller == null?null:caller.toActorRef(), actorRecipe);
        return new EntityReference(actorRef);
    }

    public void subscribeToAttribute(EntityReference listener, EntityReference target, AttributeType attributeType) {
        actorRuntime.send(listener.toActorRef(), target.toActorRef(), new Entity.AttributeSubscription(listener, attributeType));
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
        return spawnActiveObject(activeObjectClass, partitionId, MutableMap.map());
    }

    @Override
    public <A extends ActiveObject> A spawnActiveObject(Class<A> activeObjectClass, Object partitionKey, Map<String, Object> config) {
        notNull(activeObjectClass, "activeObjectClass");


        Map<String, Object> actorConfig = MutableMap.map("activeObjectClass", activeObjectClass.getName(), "config", config);
        ActorRecipe recipe = new ActorRecipe(ActiveObjectActor.class,partitionKey,actorConfig);
        final ActorRef ref = actorRuntime.spawn(recipe);

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
    public Set<EntityReference> getFromNameSpace(String nameSpace) {
        notNull(nameSpace, "nameSpace");

        namespaceMap.lock(nameSpace);
        try {
            Set<EntityReference> refs = namespaceMap.get(nameSpace);
            Set<EntityReference> result = new HashSet<>();
            if (refs != null) {
                result.addAll(refs);
            }
            return result;
        } finally {
            namespaceMap.unlock(nameSpace);
        }
    }

    @Override
    public void unregisterFromNamespace(String nameSpace, EntityReference ref) {
        notNull(nameSpace, "nameSpace");
        notNull(ref, "ref");

        namespaceMap.lock(nameSpace);
        try {
            Set<EntityReference> refs = namespaceMap.get(nameSpace);
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
    public void registerInNamespace(String nameSpace, EntityReference ref) {
        notNull(nameSpace, "nameSpace");
        notNull(ref, "ref");

        namespaceMap.lock(nameSpace);
        try {
            Set<EntityReference> refs = namespaceMap.get(nameSpace);
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
            Set<EntityReference> subscribers = namespaceMap.get(nameSpace);
            if (subscribers == null) {
                return;
            }

            for (EntityReference subscriber : subscribers) {
                actorRuntime.send(subscriber.toActorRef(), new NamespaceChange(ref, true, nameSpace));
            }
        } finally {
            namespaceSubscribersMap.unlock(nameSpace);

        }
    }

    @Override
    public void subscribeToNamespace(String nameSpace, EntityReference subscriber) {
        notNull(nameSpace, "nameSpace");
        notNull(subscriber, "subscriber");

        namespaceSubscribersMap.lock(nameSpace);
        try {
            Set<EntityReference> subscribers = namespaceSubscribersMap.get(nameSpace);
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
