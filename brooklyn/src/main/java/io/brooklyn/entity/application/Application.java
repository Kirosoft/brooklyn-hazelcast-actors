package io.brooklyn.entity.application;

import brooklyn.location.Location;
import io.brooklyn.attributes.BasicAttributeRef;
import io.brooklyn.entity.Entity;
import io.brooklyn.entity.PlatformComponent;
import io.brooklyn.entity.Start;
import io.brooklyn.entity.softwareprocess.SoftwareProcess;

public abstract class Application extends PlatformComponent {

    //On start we are going to register ourselves.
    public void receive(Start start) {
        getManagementContext().registerInNamespace("Applications", self());
    }

    //On start we are going to register ourselves.
    public void receive(SoftwareProcess.Stop start) {
        getManagementContext().unregisterFromNamespace("Application", self());
    }
}
