package com.hazelcast.actors.actors;

public interface MissingMethodHandler<E> {

    void onUnhandledMessage(Object msg, E sender);
}
