package io.brooklyn.entity.web;

import com.hazelcast.actors.api.ActorRef;
import io.brooklyn.attributes.AttributeType;
import io.brooklyn.entity.EntityConfig;

import java.util.HashMap;
import java.util.Map;

public class WebClusterConfig extends EntityConfig {

    public static final AttributeType<? extends Map<AttributeType, ActorRef>> POLICIES = new AttributeType<>("httpPort", new HashMap<AttributeType, ActorRef>());

    public WebClusterConfig() {
        super(WebCluster.class);
    }

    public WebClusterConfig addPolicy(AttributeType attributeType, ActorRef policy){
        Map<AttributeType,ActorRef> policies = (Map<AttributeType,ActorRef>)getProperty(POLICIES);
        if(policies == null){
            policies = new HashMap();
            policies.put(attributeType,policy);
        }
        this.addProperty(POLICIES,policies);
        return this;
    }
}
