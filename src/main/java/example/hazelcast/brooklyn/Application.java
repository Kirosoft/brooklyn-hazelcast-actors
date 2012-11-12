package example.hazelcast.brooklyn;

public abstract class Application extends Entity {

    //On start we are going to register ourselves.
    public void receive(SoftwareProcessEntity.StartMessage startMessage) {
        getManagementContext().registerApplication(self());
    }

    //On start we are going to register ourselves.
    public void receive(SoftwareProcessEntity.StopMessage startMessage) {
        getManagementContext().unregisterApplication(self());
    }
}
