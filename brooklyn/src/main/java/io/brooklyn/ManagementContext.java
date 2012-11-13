package io.brooklyn;


import com.hazelcast.actors.api.ActorRef;
import io.brooklyn.activeobject.ActiveObject;
import io.brooklyn.attributes.Attribute;

import java.util.Set;

public interface ManagementContext {

    void executeSomewhere(Runnable task);

    void executeLocal(Runnable task);

    Set<ActorRef> getApplications();

    <A extends ActiveObject> A newActiveObject(Class<A> activeObjectInterface);


    void subscribe(ActorRef listener, ActorRef target, Attribute attribute);

    void registerApplication(ActorRef app);

    void unregisterApplication(ActorRef app);

    SoftwareProcessDriver createDriver(SoftwareProcessEntity entity);
}
