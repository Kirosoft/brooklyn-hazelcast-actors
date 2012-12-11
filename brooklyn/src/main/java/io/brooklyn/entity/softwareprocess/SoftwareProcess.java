package io.brooklyn.entity.softwareprocess;

import brooklyn.entity.basic.Lifecycle;
import brooklyn.location.Location;
import com.hazelcast.actors.api.ActorRef;
import io.brooklyn.AbstractMessage;
import io.brooklyn.attributes.AttributeType;
import io.brooklyn.attributes.ReferenceAttribute;
import io.brooklyn.entity.PlatformComponent;

public abstract class SoftwareProcess<D extends SoftwareProcessDriver> extends PlatformComponent {

    public static final AttributeType<Lifecycle> STATE = new AttributeType<>("state");

    public final ReferenceAttribute<String> runDir = newReferenceAttribute("runDir");
    public final ReferenceAttribute<Lifecycle> state = newReferenceAttribute(STATE);
    public final ReferenceAttribute<ActorRef> machine = newReferenceAttribute("machine");

    private D softwareProcessDriver;

    public abstract Class<D> getDriverClass();

    public D getDriver() {
        if (softwareProcessDriver == null) {
            softwareProcessDriver = (D) getManagementContext().newDriver(this);
        }
        return softwareProcessDriver;
    }

    public void receive(Start start){
        //machine.set(start.machine);
    }

    public static class Start extends AbstractMessage{
        public final Location location;

        public Start(Location location) {
            this.location = location;
        }
    }
}
