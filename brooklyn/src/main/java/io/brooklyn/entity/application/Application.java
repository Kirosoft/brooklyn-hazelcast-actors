package io.brooklyn.entity.application;

import io.brooklyn.attributes.BasicAttributeRef;
import io.brooklyn.entity.Entity;
import io.brooklyn.entity.Start;
import io.brooklyn.entity.softwareprocess.SoftwareProcess;
import io.brooklyn.locations.Location;

public abstract class Application extends Entity {

    public final BasicAttributeRef<Location> location = newBasicAttributeRef("location");

    //On start we are going to register ourselves.
    public void receive(Start start) {
        getManagementContext().registerInNamespace("Applications", self());
    }

    //On start we are going to register ourselves.
    public void receive(SoftwareProcess.Stop start) {
        getManagementContext().unregisterFromNamespace("Application", self());
    }
}
