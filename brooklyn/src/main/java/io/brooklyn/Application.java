package io.brooklyn;

public abstract class Application extends Entity {

    //On start we are going to register ourselves.
    public void receive(SoftwareProcess.StartMessage startMessage) {
        getManagementContext().registerApplication(self());
    }

    //On start we are going to register ourselves.
    public void receive(SoftwareProcess.StopMessage startMessage) {
        getManagementContext().unregisterApplication(self());
    }
}
