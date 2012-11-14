package io.brooklyn;


import com.hazelcast.actors.api.ActorRef;
import io.brooklyn.activeobject.ActiveObject;
import io.brooklyn.attributes.Attribute;

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

    void subscribe(ActorRef listener, ActorRef target, Attribute attribute);

    Set<ActorRef> getApplications();

    void registerApplication(ActorRef app);

    void unregisterApplication(ActorRef app);

    SoftwareProcessDriver newDriver(SoftwareProcess entity);
}
