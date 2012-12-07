package io.brooklyn.entity.softwareprocess;

import brooklyn.entity.basic.Lifecycle;
import brooklyn.location.Location;
import com.hazelcast.actors.api.ActorRef;
import io.brooklyn.AbstractMessage;
import io.brooklyn.attributes.Attribute;
import io.brooklyn.attributes.BasicAttributeRef;
import io.brooklyn.entity.Entity;
import io.brooklyn.entity.PlatformComponent;

import java.io.Serializable;

public abstract class SoftwareProcess<D extends SoftwareProcessDriver> extends PlatformComponent {

    public static final Attribute<Lifecycle> STATE = new Attribute<>("state");

    public final BasicAttributeRef<String> runDir = newBasicAttributeRef("runDir");
    public final BasicAttributeRef<Lifecycle> state = newBasicAttributeRef(STATE);
    public final BasicAttributeRef<ActorRef> machine = newBasicAttributeRef("machine");

    private D softwareProcessDriver;

    public abstract Class<D> getDriverClass();

    public D getDriver() {
        if (softwareProcessDriver == null) {
            softwareProcessDriver = (D) getManagementContext().newDriver(this);
        }
        return softwareProcessDriver;
    }

    public void receive(Start start){
        machine.set(start.machine);
    }

    public static class Start extends AbstractMessage{
        public final ActorRef machine;
        public final Location location;

        public Start(ActorRef machine, Location location) {
            this.machine = machine;
            this.location = location;
        }
    }
}
