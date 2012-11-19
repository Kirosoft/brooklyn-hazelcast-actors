package io.brooklyn.entity;

import io.brooklyn.entity.softwareprocess.SoftwareProcess;

public abstract class Application extends Entity {

    //On start we are going to register ourselves.
    public void receive(SoftwareProcess.Start start) {
        getManagementContext().registerInNamespace("Applications", self());
    }

    //On start we are going to register ourselves.
    public void receive(SoftwareProcess.Stop start) {
        getManagementContext().unregisterFromNamespace("Application", self());
    }
}
