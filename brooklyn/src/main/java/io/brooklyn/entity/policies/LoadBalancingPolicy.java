package io.brooklyn.entity.policies;

import com.hazelcast.actors.api.ActorRef;
import io.brooklyn.attributes.Attribute;
import io.brooklyn.attributes.BasicAttributeRef;
import io.brooklyn.attributes.SensorEvent;
import io.brooklyn.entity.Entity;

public class LoadBalancingPolicy extends Entity {

    public final BasicAttributeRef<ActorRef> cluster = newBasicAttributeRef(LoadBalancingPolicyConfig.CLUSTER);

    public void receive(SensorEvent e) {
        //if (!SoftwareProcessEntityStatus.FAILURE.equals(e.getNewValue())) {
        //    return;
        //}

        //getActorRuntime().send(cluster, WebCluster.ScaleToMessage());

        System.out.println("Detected a machine on fire: " + e);
    }
}
