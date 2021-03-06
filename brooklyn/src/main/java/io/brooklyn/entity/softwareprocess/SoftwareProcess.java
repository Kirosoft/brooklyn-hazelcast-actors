package io.brooklyn.entity.softwareprocess;

import brooklyn.entity.basic.Lifecycle;
import brooklyn.location.Location;
import io.brooklyn.AbstractMessage;
import io.brooklyn.attributes.AttributeType;
import io.brooklyn.attributes.ReferenceAttribute;
import io.brooklyn.entity.EntityReference;
import io.brooklyn.entity.PlatformComponent;
import io.brooklyn.entity.Stop;

public abstract class SoftwareProcess<D extends SoftwareProcessDriver> extends PlatformComponent {

    public static final AttributeType<Lifecycle> STATE = new AttributeType<>("state");

    public final ReferenceAttribute<String> runDir = newReferenceAttribute("runDir");
    public final ReferenceAttribute<Lifecycle> state = newReferenceAttribute(STATE);
    public final ReferenceAttribute<EntityReference> machine = newReferenceAttribute("machine");

    private D softwareProcessDriver;

    public abstract Class<D> getDriverClass();

    public D getDriver() {
        if (softwareProcessDriver == null) {
            softwareProcessDriver = (D) getManagementContext().newDriver(this);
        }
        return softwareProcessDriver;
    }

    public final void receive(Start start) {
        location.set(start.location);
        preStart();
        start();
        postStart();
    }

    public abstract void start();

    public void postStart() {
    }

    public void preStart() {
    }

    public final void receive(Stop stop) {
        preStop();
        stop();
        postStop();
    }

    private void stop() {
    }

    private void postStop() {
    }

    private void preStop() {
    }

    public static class Start extends AbstractMessage {
        public final Location location;

        public Start(Location location) {
            this.location = location;
        }
    }
}
