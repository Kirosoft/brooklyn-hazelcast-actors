package io.brooklyn;

public abstract class Application extends Entity {

    //On start we are going to register ourselves.
    public void receive(SoftwareProcess.StartMessage startMessage) {
        getManagementContext().registerInNamespace("Applications", self());
    }

    //On start we are going to register ourselves.
    public void receive(SoftwareProcess.StopMessage startMessage) {
        getManagementContext().unregisterFromNamespace("Application", self());
    }
}
