package io.brooklyn;


import brooklyn.location.basic.LocationRegistry;
import com.hazelcast.actors.api.ActorRef;
import io.brooklyn.activeobject.ActiveObject;
import io.brooklyn.attributes.AttributeType;
import io.brooklyn.entity.EntityConfig;
import io.brooklyn.entity.EntityReference;
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

    EntityReference spawnAndLink(EntityReference parent, EntityConfig config);

    EntityReference spawn(EntityConfig config);

    void send(EntityReference destination, Object msg);

    void subscribeToAttribute(EntityReference listener, EntityReference target, AttributeType attributeType);

    void registerInNamespace(String nameSpace, EntityReference ref);

    void unregisterFromNamespace(String nameSpace, EntityReference ref);

    void subscribeToNamespace(String nameSpace, EntityReference ref);

    Set<EntityReference> getFromNameSpace(String nameSpace);

    SoftwareProcessDriver newDriver(SoftwareProcess entity);

    LocationRegistry getLocationRegistry();
}
