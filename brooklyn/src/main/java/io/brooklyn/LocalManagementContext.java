package io.brooklyn;

import com.hazelcast.actors.ActorRef;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.IMap;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Set;


public class LocalManagementContext implements ManagementContext {

    private HazelcastInstance hzInstance;
    private IMap<ActorRef, Object> applicationsMap;

    public void init(HazelcastInstance hzInstance) {
        applicationsMap = hzInstance.getMap("applications");
    }

    @Override
    public Set<ActorRef> getApplications() {
        return applicationsMap.keySet();
    }

    @Override
    public void registerApplication(ActorRef app) {
        applicationsMap.put(app,null);
    }

    @Override
    public void unregisterApplication(ActorRef app) {
        applicationsMap.remove(app);
    }

    //For the time being it is a simple mechanism; the driver class returned by the entity, is the actual class
    //of the driver instance to be used. In the future we can use the same mechanism as in Brooklyn.
    public SoftwareProcessDriver createDriver(SoftwareProcessEntity entity) {
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
