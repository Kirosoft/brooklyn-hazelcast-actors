package io.brooklyn.entity.policies;

import com.hazelcast.actors.api.ActorRef;
import io.brooklyn.attributes.Attribute;
import io.brooklyn.attributes.BasicAttributeRef;
import io.brooklyn.attributes.SensorEvent;
import io.brooklyn.entity.EntityConfig;

import java.util.HashMap;
import java.util.Map;

public class LoadBalancingPolicy extends Policy {

    public final BasicAttributeRef<ActorRef> cluster = newBasicAttributeRef(Config.CLUSTER);

    private final Map<ActorRef, Double> performanceMap = new HashMap<>();

    public void receive(SensorEvent e) {


        //if (!SoftwareProcessEntityStatus.FAILURE.equals(e.getNewValue())) {
        //    return;
        //}

        //getActorRuntime().send(cluster, WebCluster.ScaleToMessage());

        System.out.println("Detected a machine on fire: " + e);
    }

    public static class Config extends EntityConfig {

        public static final Attribute<ActorRef> CLUSTER = new Attribute<>("cluster");

        public Config() {
            super(LoadBalancingPolicy.class);
        }
    }
}
