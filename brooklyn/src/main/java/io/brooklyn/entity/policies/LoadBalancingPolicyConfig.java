package io.brooklyn.entity.policies;

import com.hazelcast.actors.api.ActorRef;
import io.brooklyn.attributes.Attribute;
import io.brooklyn.entity.EntityConfig;

public class LoadBalancingPolicyConfig extends EntityConfig {

    public static final Attribute<ActorRef> CLUSTER = new Attribute<>("cluster");

    public LoadBalancingPolicyConfig() {
        super(LoadBalancingPolicy.class);
    }
}
