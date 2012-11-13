package com.hazelcast.actors.api;

public interface ActorLifecycleAware {

    void init(ActorRecipe recipe)throws Exception;

    void suspend()throws Exception;

    void reactivate()throws Exception;

    void terminate()throws Exception;
}
