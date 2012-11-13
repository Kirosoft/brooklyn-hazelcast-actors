package io.brooklyn;

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
import net.sf.cglib.proxy.Enhancer;
import net.sf.cglib.proxy.MethodInterceptor;
import net.sf.cglib.proxy.MethodProxy;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static com.hazelcast.actors.utils.Util.notNull;


public class LocalManagementContext implements ManagementContext {

    private HazelcastInstance hzInstance;
    private IMap<ActorRef, Object> applicationsMap;
    private ExecutorService distributedExecutorService;
    private ExecutorService localExecutor;
    private ActorRuntime actorRuntime;

    public void init(HazelcastInstance hzInstance, ActorRuntime actorRuntime) {
        applicationsMap = hzInstance.getMap("applications");
        distributedExecutorService = hzInstance.getExecutorService("executor");
        localExecutor = Executors.newFixedThreadPool(10);
        this.actorRuntime = actorRuntime;
    }

    public void subscribe(ActorRef listener, ActorRef target, Attribute attribute) {
        actorRuntime.send(listener, target, new Entity.SubscribeMessage(listener, attribute));
    }

    @Override
    public void executeLocal(Runnable task) {
        localExecutor.execute(task);
    }

    @Override
    public void executeSomewhere(Runnable task) {
        distributedExecutorService.execute(task);
    }

    @Override
    public Set<ActorRef> getApplications() {
        return applicationsMap.keySet();
    }

     @Override
    public <A extends ActiveObject> A newActiveObject(Class<A> activeObjectClass) {
        notNull(activeObjectClass, "activeObjectClass");
        int partitionId = 0;//todo
        ActorRecipe actorRecipe = new ActorRecipe(ActiveObjectActor.class, partitionId,
                MutableMap.of("activeObjectClass", activeObjectClass.getName()));
        final ActorRef ref = actorRuntime.newActor(actorRecipe);

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
    public void registerApplication(ActorRef app) {
        notNull(app, "app");
        applicationsMap.put(app, null);
    }

    @Override
    public void unregisterApplication(ActorRef app) {
        notNull(app, "app");
        applicationsMap.remove(app);
    }

    //For the time being it is a simple mechanism; the driver class returned by the entity, is the actual class
    //of the driver instance to be used. In the future we can use the same mechanism as in Brooklyn.
    public SoftwareProcessDriver createDriver(SoftwareProcessEntity entity) {
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
