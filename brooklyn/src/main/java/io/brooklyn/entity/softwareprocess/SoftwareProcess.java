package io.brooklyn.entity.softwareprocess;

import brooklyn.location.Location;
import io.brooklyn.attributes.Attribute;
import io.brooklyn.attributes.BasicAttributeRef;
import io.brooklyn.entity.Entity;

import java.io.Serializable;

public abstract class SoftwareProcess<D extends SoftwareProcessDriver> extends Entity {

    public static final Attribute<SoftwareProcessStatus> STATE =
            new Attribute<>("state", SoftwareProcessStatus.UNSTARTED);

    public final BasicAttributeRef<String> runDir = newBasicAttributeRef("runDir");
    public final BasicAttributeRef<SoftwareProcessStatus> state = newBasicAttributeRef(STATE);

    private D softwareProcessDriver;

    public abstract Class<? extends SoftwareProcessDriver> getDriverClass();

    public D getDriver() {
        if (softwareProcessDriver == null) {
            softwareProcessDriver = (D) getManagementContext().newDriver(this);
        }
        return softwareProcessDriver;
    }

    public static class Stop implements Serializable {
    }
}
