package io.brooklyn;


import brooklyn.location.basic.LocationRegistry;
import com.hazelcast.actors.api.ActorRef;
import io.brooklyn.activeobject.ActiveObject;
import io.brooklyn.attributes.Attribute;
import io.brooklyn.entity.EntityConfig;
import io.brooklyn.entity.softwareprocess.SoftwareProcess;
import io.brooklyn.entity.softwareprocess.SoftwareProcessDriver;

import java.util.Map;
import java.util.Set;

public interface ManagementContext {

    void executeAnywhere(Runnable task);

    void executeLocally(Runnable task);

    <A extends ActiveObject> A newActiveObject(Class<A> activeObjectClass);

    /**
     * Creates a new ActiveObject.
     * <p/>
     * The returned object is a proxy to the real ActiveObject (that communicates through sending messages). It relies
     * on generating a runtime subclass of the ActiveObjectClass.
     *
     * @param activeObjectClass
     * @param partitionId       the id of the partition this ActiveObject should run on. -1 indicates 'I don't care', so
     *                          a partition will be looked up (currently random.. but in the future we could load balance it).
     * @param config
     * @param <A>
     * @return
     */
    <A extends ActiveObject> A newActiveObject(Class<A> activeObjectClass, int partitionId, Map<String, Object> config);

    ActorRef newEntity(EntityConfig entityConfig);

    void subscribeToAttribute(ActorRef listener, ActorRef target, Attribute attribute);

    void registerInNamespace(String nameSpace, ActorRef ref);

    void unregisterFromNamespace(String nameSpace, ActorRef ref);

    void subscribeToNamespace(String nameSpace, ActorRef ref);

    Set<ActorRef> getFromNameSpace(String nameSpace);

    SoftwareProcessDriver newDriver(SoftwareProcess entity);

    LocationRegistry getLocationRegistry();
}
