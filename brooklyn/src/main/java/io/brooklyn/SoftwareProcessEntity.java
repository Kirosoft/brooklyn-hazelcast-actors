package io.brooklyn;

import io.brooklyn.attributes.Attribute;
import io.brooklyn.attributes.BasicAttributeRef;

import java.io.Serializable;

public abstract class SoftwareProcessEntity<D extends SoftwareProcessDriver> extends Entity {

    public static final Attribute<String> LOCATION = new Attribute<String>("location");
    public static final Attribute<String> RUN_DIR = new Attribute<String>("runDir");

    public final BasicAttributeRef<String> location = newBasicAttributeRef(LOCATION);
    public final BasicAttributeRef<String> runDir = newBasicAttributeRef(RUN_DIR);

    private D softwareProcessDriver;

    public abstract Class<? extends SoftwareProcessDriver> getDriverClass();

    public D getDriver() {
        if (softwareProcessDriver == null) {
            softwareProcessDriver = (D) getManagementContext().createDriver(this);
        }
        return softwareProcessDriver;
    }

    public static class StartMessage implements Serializable {
        public final String location;

        public StartMessage(String location) {
            this.location = location;
        }
    }

    public static class StopMessage implements Serializable {
    }
}
