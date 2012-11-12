package example.hazelcast.actors;

public interface ActorLifecycleAware {

    void init(ActorRecipe recipe);

    void suspend();

    void reactivate();

    void stop();
}
