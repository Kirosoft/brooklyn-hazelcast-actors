package io.brooklyn;


import brooklyn.location.basic.LocationRegistry;
import com.hazelcast.actors.api.ActorRef;
import io.brooklyn.activeobject.ActiveObject;
import io.brooklyn.attributes.Attribute;
import io.brooklyn.entity.Entity;
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
     * @param partitionKey       the key that determines the partition. If you don't care where the ActiveObject is going
     *                           to run, just pass null.
     * @param config
     * @param <A>
     * @return
     */
    <A extends ActiveObject> A spawnActiveObject(Class<A> activeObjectClass, Object partitionKey, Map<String, Object> config);

    ActorRef spawnAndLink(ActorRef parent, EntityConfig config);

    ActorRef spawn(EntityConfig config);

    void subscribeToAttribute(ActorRef listener, ActorRef target, Attribute attribute);

    void registerInNamespace(String nameSpace, ActorRef ref);

    void unregisterFromNamespace(String nameSpace, ActorRef ref);

    void subscribeToNamespace(String nameSpace, ActorRef ref);

    Set<ActorRef> getFromNameSpace(String nameSpace);

    SoftwareProcessDriver newDriver(SoftwareProcess entity);

    LocationRegistry getLocationRegistry();
}
