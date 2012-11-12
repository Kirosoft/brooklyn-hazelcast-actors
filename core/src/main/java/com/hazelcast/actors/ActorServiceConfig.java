package com.hazelcast.actors;

import com.hazelcast.config.CustomServiceConfig;

import java.util.HashMap;
import java.util.Map;

public class ActorServiceConfig extends CustomServiceConfig {

    private Map<String,Object> dependencies = new HashMap<String,Object>();

    public void addDependency(String name, Object dependency){
        dependencies.put(name,dependency);
    }

    public Object getDependency(String name){
        return dependencies.get(name);
    }

    public Map<String, Object> getDependencies() {
        return dependencies;
    }
}
