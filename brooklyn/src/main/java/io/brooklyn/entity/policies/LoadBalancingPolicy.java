package io.brooklyn.entity.policies;

import brooklyn.entity.basic.Lifecycle;
import com.hazelcast.actors.api.ActorRef;
import io.brooklyn.attributes.BasicAttributeRef;
import io.brooklyn.attributes.SensorEvent;
import io.brooklyn.entity.Start;

public class LoadBalancingPolicy extends Policy {

    public final BasicAttributeRef<ActorRef> cluster = newBasicAttributeRef(LoadBalancingPolicyConfig.CLUSTER);

    public void receive(Start start) {

    }

    public void receive(SensorEvent e) {
        if (!Lifecycle.ON_FIRE.equals(e.getNewValue())) {

        }

        //if (!SoftwareProcessEntityStatus.FAILURE.equals(e.getNewValue())) {
        //    return;
        //}

        //getActorRuntime().send(cluster, WebCluster.ScaleToMessage());

        System.out.println("Detected a machine on fire: " + e);
    }
}
