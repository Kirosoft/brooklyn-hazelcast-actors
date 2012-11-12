package com.hazelcast.actors.api;

public interface ActorLifecycleAware {

    void init(ActorRecipe recipe);

    void suspend();

    void reactivate();

    void stop();
}
