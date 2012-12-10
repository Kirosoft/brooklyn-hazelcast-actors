package io.brooklyn.entity.application;

import io.brooklyn.entity.PlatformComponent;
import io.brooklyn.entity.Stop;
import io.brooklyn.entity.softwareprocess.SoftwareProcess;

public abstract class Application extends PlatformComponent {

     //On start we are going to start ourselves.
    public void receive(SoftwareProcess.Start start) {
        getManagementContext().registerInNamespace("Applications", self());
    }

    //On start we are going to start ourselves.
    public void receive(Stop start) {
        getManagementContext().unregisterFromNamespace("Application", self());
    }


}
