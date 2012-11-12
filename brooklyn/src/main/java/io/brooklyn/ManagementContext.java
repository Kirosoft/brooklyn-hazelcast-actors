package io.brooklyn;


import com.hazelcast.actors.api.ActorRef;

import java.util.Set;

public interface ManagementContext {

    Set<ActorRef> getApplications();

    void registerApplication(ActorRef app);

    void unregisterApplication(ActorRef app);

    SoftwareProcessDriver createDriver(SoftwareProcessEntity entity);
}
