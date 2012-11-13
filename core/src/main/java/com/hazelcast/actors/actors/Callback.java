package com.hazelcast.actors.actors;

import com.hazelcast.actors.api.Actor;

import java.io.Serializable;

public interface Callback<A extends Actor> extends Serializable {
    void run(A actor);
}
