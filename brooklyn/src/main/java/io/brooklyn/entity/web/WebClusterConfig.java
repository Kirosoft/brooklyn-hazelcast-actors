package io.brooklyn.entity.web;

import com.hazelcast.actors.api.ActorRef;
import io.brooklyn.attributes.Attribute;
import io.brooklyn.entity.EntityConfig;

import java.util.HashMap;
import java.util.Map;

public class WebClusterConfig extends EntityConfig {

    public static final Attribute<? extends Map<Attribute, ActorRef>> POLICIES = new Attribute<>("httpPort", new HashMap<Attribute, ActorRef>());

    public WebClusterConfig() {
        super(WebCluster.class);
    }

    public WebClusterConfig addPolicy(Attribute attribute, ActorRef policy){
        Map<Attribute,ActorRef> policies = (Map<Attribute,ActorRef>)getProperty(POLICIES);
        if(policies == null){
            policies = new HashMap();
            policies.put(attribute,policy);
        }
        this.addProperty(POLICIES,policies);
        return this;
    }
}
