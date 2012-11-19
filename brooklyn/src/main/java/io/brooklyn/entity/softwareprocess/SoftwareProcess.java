package io.brooklyn.entity.softwareprocess;

import io.brooklyn.attributes.Attribute;
import io.brooklyn.attributes.BasicAttributeRef;
import io.brooklyn.entity.Entity;

import java.io.Serializable;

public abstract class SoftwareProcess<D extends SoftwareProcessDriver> extends Entity {

    public static final Attribute<String> LOCATION = new Attribute<>("location");
    public static final Attribute<String> RUN_DIR = new Attribute<>("runDir");
    public static final Attribute<SoftwareProcessStatus> STATE =
            new Attribute<>("state", SoftwareProcessStatus.UNSTARTED);

    public final BasicAttributeRef<String> location = newBasicAttributeRef(LOCATION);
    public final BasicAttributeRef<String> runDir = newBasicAttributeRef(RUN_DIR);
    public final BasicAttributeRef<SoftwareProcessStatus> state = newBasicAttributeRef(STATE);

    private D softwareProcessDriver;

    public abstract Class<? extends SoftwareProcessDriver> getDriverClass();

    public D getDriver() {
        if (softwareProcessDriver == null) {
            softwareProcessDriver = (D) getManagementContext().newDriver(this);
        }
        return softwareProcessDriver;
    }

    public static class Start implements Serializable {
        public final String location;

        public Start(String location) {
            this.location = location;
        }
    }

    public static class Stop implements Serializable {
    }
}
