package io.brooklyn;

import io.brooklyn.attributes.Attribute;
import io.brooklyn.attributes.BasicAttributeRef;

import java.io.Serializable;

public abstract class SoftwareProcessEntity<D extends SoftwareProcessDriver> extends Entity {

    protected final BasicAttributeRef<String> locationAttribute = newBasicAttributeRef(new Attribute<String>("location"));
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
