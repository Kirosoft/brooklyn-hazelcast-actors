package io.brooklyn;

import io.brooklyn.attributes.AttributeType;
import io.brooklyn.attributes.BasicAttribute;

import java.io.Serializable;

public abstract class SoftwareProcessEntity<D extends SoftwareProcessDriver> extends Entity {

    protected final BasicAttribute<String> locationAttribute = newBasicAttribute(new AttributeType<String>("location"));
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
