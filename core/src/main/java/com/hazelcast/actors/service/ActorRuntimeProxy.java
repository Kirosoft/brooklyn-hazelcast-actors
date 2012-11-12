package com.hazelcast.actors.service;

import com.hazelcast.actors.api.ActorRuntime;
import com.hazelcast.spi.ServiceProxy;

public interface ActorRuntimeProxy extends ActorRuntime, ServiceProxy {
}
