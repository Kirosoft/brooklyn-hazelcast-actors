package com.hazelcast.actors.service;

import com.hazelcast.config.CustomServiceConfig;

import java.util.HashMap;
import java.util.Map;

import static com.hazelcast.actors.utils.Util.notNull;

public class ActorServiceConfig extends CustomServiceConfig {

    private Map<String, Object> dependencies = new HashMap<String, Object>();

    public ActorServiceConfig() {
        setName(ActorService.NAME);
        setClassName(ActorService.class.getName());
        setEnabled(true);
    }

    public void addDependency(String name, Object dependency) {
        notNull(name, "name");
        notNull(dependency, "dependency");
        dependencies.put(name, dependency);
    }

    public Object getDependency(String name) {
        return dependencies.get(name);
    }

    public Map<String, Object> getDependencies() {
        return new HashMap<String, Object>(dependencies);
    }
}
