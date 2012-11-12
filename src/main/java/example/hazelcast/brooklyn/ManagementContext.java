package example.hazelcast.brooklyn;

import example.hazelcast.actors.ActorRef;

import java.util.Collection;
import java.util.Set;

public interface ManagementContext {

    Set<ActorRef> getApplications();

    void registerApplication(ActorRef app);

    void unregisterApplication(ActorRef app);

    SoftwareProcessDriver createDriver(SoftwareProcessEntity entity);
}
